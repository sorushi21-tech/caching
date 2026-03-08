package com.caching.transaction_service_caffeine_cache.caching;

import com.caching.transaction_service_caffeine_cache.config.CacheConfig;
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

  @Scheduled(cron = "0 */5 * * * *")
  public void refreshFxRatesCache() {
    log.info("Scheduled hard refresh — clearing fxRates cache via Caffeine CacheManager");
    Objects.requireNonNull(cacheManager.getCache(CacheConfig.FX_RATES_CACHE)).clear();
  }

  @Scheduled(cron = "0 */2 * * * *")
  public void refreshTransactionsCache() {
    log.info("Scheduled hard refresh — clearing transactions cache via Caffeine CacheManager");
    Objects.requireNonNull(cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE)).clear();
  }
}
