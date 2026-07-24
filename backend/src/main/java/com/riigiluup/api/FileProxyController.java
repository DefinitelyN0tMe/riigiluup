package com.riigiluup.api;

import com.riigiluup.common.CacheConfig;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Proxies Riigikogu file downloads through this backend so we can serve them
 * to the SPA without hitting Riigikogu's per-IP rate limit on every render.
 * Bytes are cached in memory (see {@link CacheConfig#CACHE_FILES}).
 * <p>
 * DoS hardening: the {uuid} is validated before any upstream work (junk keys are
 * rejected without touching the cache/loader), and concurrent upstream fetches are
 * bounded by a fair semaphore so a flood of distinct UUIDs cannot pin the Tomcat pool.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "*")
public class FileProxyController {

    // Canonical UUID shape (8-4-4-4-12 hex). Cheap pre-filter before UUID.fromString.
    private static final Pattern UUID_RE =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final Loader loader;

    public FileProxyController(Loader loader) {
        this.loader = loader;
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        // Reject junk keys up front — no loader/cache/upstream involvement at all.
        if (!isValidUuid(uuid)) return ResponseEntity.badRequest().build();
        byte[] bytes;
        try {
            bytes = loader.load(uuid);
        } catch (EmptyUpstreamException e) {
            // Missing photo / transient upstream miss — 404, and (being an exception) not cached.
            return ResponseEntity.notFound().build();
        } catch (UpstreamBusyException e) {
            // Upstream fetch slots exhausted — shed load instead of holding the thread.
            return ResponseEntity.status(503).header("Retry-After", "5").build();
        } catch (Exception e) {
            log.warn("file proxy failed for {}", uuid, e);
            return ResponseEntity.status(502).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(bytes);
    }

    static boolean isValidUuid(String s) {
        if (s == null || !UUID_RE.matcher(s).matches()) return false;
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Thrown when no upstream-fetch permit could be acquired within the timeout. */
    static class UpstreamBusyException extends RuntimeException {
        UpstreamBusyException() {
            super("upstream fetch slots busy");
        }
    }

    /** Thrown when the upstream returns no bytes — surfaced as 404, and never cached. */
    static class EmptyUpstreamException extends RuntimeException {
        EmptyUpstreamException() {
            super("empty upstream response");
        }
    }

    @Component
    static class Loader {
        private final RiigikoguClient client;
        /** Bound concurrent upstream fetches so files/* cannot occupy the whole Tomcat pool. */
        private final Semaphore permits;
        private final long acquireTimeoutMs;
        /** Persistent disk tier; null = disabled (in-memory cache only). */
        private final Path cacheDir;

        Loader(RiigikoguClient client,
               @Value("${riigiluup.files.max-concurrent-fetch:6}") int maxConcurrent,
               @Value("${riigiluup.files.fetch-acquire-timeout-ms:3000}") long acquireTimeoutMs,
               @Value("${riigiluup.files.cache-dir:}") String cacheDir) {
            this.client = client;
            // Fair so callers are served roughly FIFO; a starved thread eventually gets 503, not a hang.
            this.permits = new Semaphore(Math.max(0, maxConcurrent), true);
            this.acquireTimeoutMs = acquireTimeoutMs;
            this.cacheDir = initCacheDir(cacheDir);
        }

        private static Path initCacheDir(String path) {
            if (path == null || path.isBlank()) return null;
            try {
                Path dir = Path.of(path);
                Files.createDirectories(dir);
                log.info("file proxy: persistent disk cache at {}", dir);
                return dir;
            } catch (IOException e) {
                log.warn("file cache dir '{}' unusable — in-memory only: {}", path, e.toString());
                return null;
            }
        }

        // Two tiers in front of the source: in-memory (@Cacheable) → persistent disk → upstream.
        // Portraits are immutable (a changed photo gets a new UUID), so the disk tier caches forever
        // and survives restarts — which is what stops a redeploy from re-fetching every photo behind
        // the ~1 req/s source throttle. sync=true collapses a stampede on one photo into one fetch;
        // it forbids `unless`, so an empty upstream is signalled by throwing (exceptions aren't cached).
        @Cacheable(value = CacheConfig.CACHE_FILES, key = "#uuid", sync = true)
        public byte[] load(String uuid) {
            byte[] fromDisk = readDisk(uuid);
            if (fromDisk != null) return fromDisk;

            boolean acquired;
            try {
                acquired = permits.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UpstreamBusyException();
            }
            if (!acquired) throw new UpstreamBusyException();
            try {
                byte[] bytes = client.fetchFileBytes(uuid);
                if (bytes == null || bytes.length == 0) throw new EmptyUpstreamException();
                writeDisk(uuid, bytes);
                return bytes;
            } finally {
                permits.release();
            }
        }

        private byte[] readDisk(String uuid) {
            if (cacheDir == null) return null;
            Path f = cacheDir.resolve(uuid);
            try {
                return Files.exists(f) ? Files.readAllBytes(f) : null;
            } catch (IOException e) {
                return null;
            }
        }

        private void writeDisk(String uuid, byte[] bytes) {
            if (cacheDir == null) return;
            try {
                // Write to a temp name then atomically move, so a concurrent reader never sees a partial file.
                Path tmp = cacheDir.resolve(uuid + ".tmp");
                Files.write(tmp, bytes);
                Files.move(tmp, cacheDir.resolve(uuid),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                log.debug("file cache write failed for {}: {}", uuid, e.toString());
            }
        }
    }
}
