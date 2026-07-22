package com.riigiluup.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Clears the stats/analytics caches after a bulk import so dashboards reflect fresh data
 * immediately instead of serving the previous numbers for up to the 30-min TTL. The
 * proxied-files cache is left alone — portraits don't change with domain imports and
 * re-fetching them would only burn the upstream rate budget.
 */
@Component
public class AnalyticsCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheEvictor.class);

    private final CacheManager cacheManager;

    public AnalyticsCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAll() {
        List<String> cleared = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            if (CacheConfig.CACHE_FILES.equals(name) || CacheConfig.CACHE_BUCKETS.equals(name)) continue;
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                cleared.add(name);
            }
        }
        log.info("evicted analytics caches after import: {}", cleared);
    }
}
