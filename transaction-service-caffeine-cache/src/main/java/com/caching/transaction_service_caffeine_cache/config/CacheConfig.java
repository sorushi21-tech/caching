package com.caching.transaction_service_caffeine_cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

  public static final String FX_RATES_CACHE = "fxRates";
  public static final String TRANSACTIONS_CACHE = "transactions";

  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .maximumSize(1000)
        .recordStats()
        .removalListener(
            (key, value, cause) -> log.info("Cache entry removed: key={}, cause={}", key, cause));
  }

  @Bean
  public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
    CaffeineCacheManager cacheManager =
        new CaffeineCacheManager(FX_RATES_CACHE, TRANSACTIONS_CACHE);
    cacheManager.setCaffeine(caffeine);
    return cacheManager;
  }
}
