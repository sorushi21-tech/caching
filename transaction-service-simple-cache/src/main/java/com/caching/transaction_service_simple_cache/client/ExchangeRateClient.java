package com.caching.transaction_service_simple_cache.client;

import com.caching.transaction_service_simple_cache.config.CacheConfig;
import com.caching.transaction_service_simple_cache.dto.ExchangeRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Component
public class ExchangeRateClient {

    private final RestTemplate restTemplate;
    private final String exchangeServiceBaseUrl;

    public ExchangeRateClient(
            RestTemplate restTemplate,
            @Value("${exchange.service.base-url}") String exchangeServiceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.exchangeServiceBaseUrl = exchangeServiceBaseUrl;
    }

    @Cacheable(value = CacheConfig.FX_RATES_CACHE, key = "#fromCurrency + ':' + #toCurrency")
    public BigDecimal getRate(String fromCurrency, String toCurrency) {

        log.info("Cache miss — fetching rate from exchange-rate-service for {} -> {}", fromCurrency, toCurrency);

        String normalizedFrom = fromCurrency.toUpperCase(Locale.ROOT);
        String normalizedTo = toCurrency.toUpperCase(Locale.ROOT);

        if (normalizedFrom.equals(normalizedTo)) {
            log.debug("Skipping exchange-rate-service call for identical currencies: {}", normalizedFrom);
            return BigDecimal.ONE;
        }

        String uri = UriComponentsBuilder
                .fromUriString(exchangeServiceBaseUrl)
                .path("/rate")
                .queryParam("from", normalizedFrom)
                .queryParam("to", normalizedTo)
                .toUriString();

        try {
            ExchangeRateResponse exchangeResponse = restTemplate.getForObject(uri, ExchangeRateResponse.class);

            if (exchangeResponse == null || exchangeResponse.rate() == null) {
                log.error("Exchange-rate-service returned empty rate response for from={} to={}", normalizedFrom, normalizedTo);
                throw new IllegalStateException("Unable to fetch exchange rate from exchange-rate-service");
            }

            log.debug("Rate received from exchange-rate-service: from={} to={} rate={}", normalizedFrom, normalizedTo, exchangeResponse.rate());
            return exchangeResponse.rate();
        } catch (RestClientException ex) {
            log.error("Failed to call exchange-rate-service for rate from={} to={}", normalizedFrom, normalizedTo, ex);
            throw new IllegalStateException("Unable to fetch exchange rate from exchange-rate-service", ex);
        }
    }
}

