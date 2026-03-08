package com.caching.transaction_service_simple_cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SimpleCacheManager is a basic CacheManager that works with a fixed collection of caches defined at configuration time.
 * -
 * Each cache is backed by a ConcurrentMapCache (which internally uses ConcurrentHashMap), but the key difference from
 * the manual ConcurrentHashMap approach is that caching is managed declaratively via Spring's @Cacheable / @CacheEvict annotations
 * rather than programmatically.
 */
@Configuration
public class CacheConfig {
    public static final String FX_RATES_CACHE = "fxRates";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new ConcurrentMapCache(FX_RATES_CACHE)
        ));
        return cacheManager;
    }
}
