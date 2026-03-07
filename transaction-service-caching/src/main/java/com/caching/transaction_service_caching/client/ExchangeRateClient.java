package com.caching.transaction_service_caching.client;

import com.caching.transaction_service_caching.dto.ExchangeConversionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {

        String normalizedFrom = fromCurrency.toUpperCase(Locale.ROOT);
        String normalizedTo = toCurrency.toUpperCase(Locale.ROOT);

        log.info("Requesting currency conversion from exchange-rate-service: from={} to={} amount={}", normalizedFrom, normalizedTo, amount);

        if (normalizedFrom.equals(normalizedTo)) {
            log.debug("Skipping conversion call for identical currencies: {}", normalizedFrom);
            return amount;
        }

        String uri = UriComponentsBuilder
                .fromUriString(exchangeServiceBaseUrl)
                .path("/convert")
                .queryParam("from", normalizedFrom)
                .queryParam("to", normalizedTo)
                .queryParam("amount", amount)
                .toUriString();

        try {
            ExchangeConversionResponse exchangeResponse = restTemplate.getForObject(uri, ExchangeConversionResponse.class);
            if (exchangeResponse == null || exchangeResponse.convertedAmount() == null) {
                log.error("Exchange-rate-service returned empty conversion response for from={} to={} amount={}", normalizedFrom, normalizedTo, amount);
                throw new IllegalStateException("Unable to convert amount from exchange-rate-service");
            }

            log.debug("Conversion received from exchange-rate-service: from={} to={} amount={} convertedAmount={}", normalizedFrom, normalizedTo, amount, exchangeResponse.convertedAmount());
            return exchangeResponse.convertedAmount();
        } catch (RestClientException ex) {
            log.error("Failed to call exchange-rate-service for conversion from={} to={} amount={}", normalizedFrom, normalizedTo, amount, ex);
            throw new IllegalStateException("Unable to convert amount from exchange-rate-service", ex);
        }
    }
}
