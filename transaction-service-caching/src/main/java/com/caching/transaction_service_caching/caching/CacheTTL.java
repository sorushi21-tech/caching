package com.caching.transaction_service_caching.caching;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheTTL {

  private final FxRateCache<String, BigDecimal> fxRateCache;

  // Runs every 1 minutes
  @Scheduled(cron = "0 * * * * *")
  public void refreshCache() {

    log.info("Refreshing cache");
    fxRateCache.clear();
  }
}
