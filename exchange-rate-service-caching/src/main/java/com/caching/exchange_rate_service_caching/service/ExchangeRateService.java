package com.caching.exchange_rate_service_caching.service;

import com.caching.exchange_rate_service_caching.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        return exchangeRateRepository.findRate(from, to)
                .map(rate -> {
                    BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                    log.debug("Converted amount using rate {}: from={} to={} amount={} converted={}", rate, from, to, amount, converted);
                    return converted;
                })
                .orElseThrow(() -> {
                    log.warn("Exchange rate not available for from={} to={}", from, to);
                    return new IllegalArgumentException("Exchange rate not available for " + from + " to " + to);
                });
    }
}