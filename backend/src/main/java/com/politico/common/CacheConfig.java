package com.politico.common;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PARTICIPATION = "participation-stats";
    public static final String CACHE_VOTING = "voting-stats";
    public static final String CACHE_FILES = "riigikogu-files";
    /** Bucket4j rate-limit bucket store (cache-name in application.yml). */
    public static final String CACHE_BUCKETS = "buckets";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_PARTICIPATION, CACHE_VOTING, CACHE_FILES, CACHE_BUCKETS);
    }
}
