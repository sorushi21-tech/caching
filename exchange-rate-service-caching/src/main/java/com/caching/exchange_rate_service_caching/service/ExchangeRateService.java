package com.caching.exchange_rate_service_caching.service;

import com.caching.exchange_rate_service_caching.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal findRate(String from, String to) {
        return exchangeRateRepository.findRate(from, to)
                .map(rate -> {
                    log.debug("Rate lookup successful: from={} to={} rate={}", from, to, rate);
                    return rate;
                })
                .orElseThrow(() -> {
                    log.warn("Exchange rate not available for from={} to={}", from, to);
                    return new IllegalArgumentException("Exchange rate not available for " + from + " to " + to);
                });
    }
}