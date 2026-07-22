package com.riigiluup.api;

import com.riigiluup.common.CacheConfig;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Proxies Riigikogu file downloads through this backend so we can serve them
 * to the SPA without hitting Riigikogu's per-IP rate limit on every render.
 * Bytes are cached in memory (see {@link CacheConfig#CACHE_FILES}).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileProxyController {

    private final Loader loader;

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        byte[] bytes;
        try {
            bytes = loader.load(uuid);
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

    @Component
    @RequiredArgsConstructor
    static class Loader {
        private final RiigikoguClient client;
        /** Serialize cross-key file fetches so we don't burst Riigikogu's per-IP limit. */
        private final ReentrantLock fetchLock = new ReentrantLock(true);

        // Don't pin an empty upstream response (missing photo, transient miss) for 6 hours.
        @Cacheable(value = CacheConfig.CACHE_FILES, key = "#uuid", sync = true,
                unless = "#result == null || #result.length == 0")
        public byte[] load(String uuid) {
            fetchLock.lock();
            try {
                return client.fetchFileBytes(uuid);
            } finally {
                fetchLock.unlock();
            }
        }
    }
}
