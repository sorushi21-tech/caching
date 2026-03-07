package com.caching.transaction_service_caching.client;

import com.caching.transaction_service_caching.caching.FxRateCache;
import com.caching.transaction_service_caching.dto.ExchangeRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Component
public class ExchangeRateClient {

    private final RestTemplate restTemplate;
    private final String exchangeServiceBaseUrl;
    private final FxRateCache<String, BigDecimal> fxRateCache;

    public ExchangeRateClient(
            RestTemplate restTemplate,
            @Value("${exchange.service.base-url}") String exchangeServiceBaseUrl, FxRateCache<String, BigDecimal> fxRateCache
    ) {
        this.restTemplate = restTemplate;
        this.exchangeServiceBaseUrl = exchangeServiceBaseUrl;
        this.fxRateCache = fxRateCache;
    }

    public BigDecimal getRate(String fromCurrency, String toCurrency) {

        log.info("Checking for fx rate in cache for currency change from currency {} to currency {}", fromCurrency, toCurrency);
        BigDecimal fxRate = fxRateCache.get(fromCurrency + ":" + toCurrency);

        if (fxRate != null) {
            log.info("FxRate found in cached");
            return fxRate;
        }

        log.info("FxRate not found in cached");
        String normalizedFrom = fromCurrency.toUpperCase(Locale.ROOT);
        String normalizedTo = toCurrency.toUpperCase(Locale.ROOT);

        log.info("Requesting exchange rate from exchange-rate-service: from={} to={}", normalizedFrom, normalizedTo);

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

        ExchangeRateResponse exchangeResponse = null;

        try {
            exchangeResponse = restTemplate.getForObject(uri, ExchangeRateResponse.class);
            if (exchangeResponse == null || exchangeResponse.rate() == null) {
                log.error("Exchange-rate-service returned empty rate response for from={} to={}", normalizedFrom, normalizedTo);
                throw new IllegalStateException("Unable to fetch exchange rate from exchange-rate-service");
            }

            log.debug("Rate received from exchange-rate-service: from={} to={} rate={}", normalizedFrom, normalizedTo, exchangeResponse.rate());
            return exchangeResponse.rate();
        } catch (RestClientException ex) {
            log.error("Failed to call exchange-rate-service for rate from={} to={}", normalizedFrom, normalizedTo, ex);
            throw new IllegalStateException("Unable to fetch exchange rate from exchange-rate-service", ex);
        } finally {
            fxRateCache.put(fromCurrency + ":" + toCurrency, Objects.requireNonNull(exchangeResponse.rate()));
        }
    }
}
