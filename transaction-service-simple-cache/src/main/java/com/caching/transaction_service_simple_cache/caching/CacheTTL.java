package com.caching.transaction_service_simple_cache.caching;

import com.caching.transaction_service_simple_cache.config.CacheConfig;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheTTL {

  private final CacheManager cacheManager;

  public CacheTTL(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Scheduled(cron = "0 * * * * *")
  public void refreshCache() {

    log.info("Refreshing fxRates cache via SimpleCacheManager");
    Objects.requireNonNull(cacheManager.getCache(CacheConfig.FX_RATES_CACHE)).clear();
  }
}
