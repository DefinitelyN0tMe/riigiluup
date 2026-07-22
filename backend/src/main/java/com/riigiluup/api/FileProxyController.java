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
        } catch (UpstreamBusyException e) {
            // Upstream fetch slots exhausted — shed load instead of holding the thread.
            return ResponseEntity.status(503).header("Retry-After", "5").build();
        } catch (Exception e) {
            log.warn("file proxy failed for {}", uuid, e);
            return ResponseEntity.status(502).build();
        }
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
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

    @Component
    static class Loader {
        private final RiigikoguClient client;
        /** Bound concurrent upstream fetches so files/* cannot occupy the whole Tomcat pool. */
        private final Semaphore permits;
        private final long acquireTimeoutMs;

        Loader(RiigikoguClient client,
               @Value("${riigiluup.files.max-concurrent-fetch:6}") int maxConcurrent,
               @Value("${riigiluup.files.fetch-acquire-timeout-ms:3000}") long acquireTimeoutMs) {
            this.client = client;
            // Fair so callers are served roughly FIFO; a starved thread eventually gets 503, not a hang.
            this.permits = new Semaphore(Math.max(0, maxConcurrent), true);
            this.acquireTimeoutMs = acquireTimeoutMs;
        }

        // Don't pin an empty upstream response (missing photo, transient miss) for 6 hours.
        // @Cacheable does not cache on exception, so an UpstreamBusyException never poisons the cache.
        @Cacheable(value = CacheConfig.CACHE_FILES, key = "#uuid", sync = true,
                unless = "#result == null || #result.length == 0")
        public byte[] load(String uuid) {
            boolean acquired;
            try {
                acquired = permits.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UpstreamBusyException();
            }
            if (!acquired) throw new UpstreamBusyException();
            try {
                return client.fetchFileBytes(uuid);
            } finally {
                permits.release();
            }
        }
    }
}
