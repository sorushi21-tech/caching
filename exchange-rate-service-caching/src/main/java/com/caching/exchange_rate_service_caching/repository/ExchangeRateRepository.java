package com.caching.exchange_rate_service_caching.repository;

import com.caching.exchange_rate_service_caching.client.FrankfurterClient;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExchangeRateRepository {

  private final FrankfurterClient frankfurterClient;

  public Optional<BigDecimal> findRate(String from, String to) {
    Optional<BigDecimal> rate = frankfurterClient.getRate(from, to);
    log.debug("Repository lookup for from={} to={} foundRate={}", from, to, rate.isPresent());
    return rate;
  }
}
