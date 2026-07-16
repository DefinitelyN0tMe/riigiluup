package com.riigiluup.common;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PARTICIPATION = "participation-stats";
    public static final String CACHE_VOTING = "voting-stats";
    public static final String CACHE_FILES = "riigikogu-files";
    /** Legacy rate-limit bucket store name (kept for compatibility; RateLimitFilter owns its own). */
    public static final String CACHE_BUCKETS = "buckets";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        // Bounded default for stats + analytics caches. Domain data refreshes ~4x/day, so a
        // 30-min TTL keeps the dashboard cheap (analytics aggregates scan the 395k individual_vote
        // table) without serving stale numbers for long. Dynamic creation stays on so each
        // analytics-* method gets its own bounded cache without pre-registration.
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofMinutes(30)));
        // Proxied Riigikogu files (portraits, and potentially large PDFs) are keyed by a
        // client-supplied UUID — bound the cache by total bytes and evict idle entries so a
        // crawler of distinct UUIDs cannot grow the JVM heap without limit.
        mgr.registerCustomCache(CACHE_FILES, Caffeine.newBuilder()
                .maximumWeight(64L * 1024 * 1024) // 64 MB of cached file bytes
                .weigher((Object k, Object v) -> v instanceof byte[] b ? b.length : 1)
                .expireAfterAccess(Duration.ofHours(6))
                .build());
        return mgr;
    }
}
