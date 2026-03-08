package com.caching.exchange_rate_service_caching.client;

import com.caching.exchange_rate_service_caching.dto.FrankfurterLatestResponse;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class FrankfurterClient {

  private final RestTemplate restTemplate;
  private final String frankfurterBaseUrl;

  public FrankfurterClient(
      RestTemplate restTemplate, @Value("${frankfurter.api.base-url}") String frankfurterBaseUrl) {
    this.restTemplate = restTemplate;
    this.frankfurterBaseUrl = frankfurterBaseUrl;
  }

  public Optional<BigDecimal> getRate(String fromCurrency, String toCurrency) {

    String normalizedFrom = normalize(fromCurrency);
    String normalizedTo = normalize(toCurrency);

    if (normalizedFrom.equals(normalizedTo)) {
      log.debug("Skipping external lookup for identical currencies: {}", normalizedFrom);
      return Optional.of(BigDecimal.ONE);
    }

    log.info("Fetching rate from Frankfurter for from={} to={}", normalizedFrom, normalizedTo);

    String uri =
        UriComponentsBuilder.fromUriString(frankfurterBaseUrl)
            .path("/latest")
            .queryParam("base", normalizedFrom)
            .queryParam("symbols", normalizedTo)
            .toUriString();

    try {

      FrankfurterLatestResponse response =
          restTemplate.getForObject(uri, FrankfurterLatestResponse.class);

      if (response == null || response.rates() == null) {
        log.warn(
            "Frankfurter rate response was empty for from={} to={}", normalizedFrom, normalizedTo);
        return Optional.empty();
      }

      Optional<BigDecimal> rate = Optional.ofNullable(response.rates().get(normalizedTo));

      if (rate.isEmpty()) {
        log.warn(
            "Frankfurter response did not include requested symbol for from={} to={}",
            normalizedFrom,
            normalizedTo);
      } else {
        log.debug(
            "Frankfurter returned rate {} for from={} to={}",
            rate.get(),
            normalizedFrom,
            normalizedTo);
      }

      return rate;

    } catch (RestClientException ex) {
      log.warn(
          "Failed to fetch rate for from={} to={} due to client error",
          normalizedFrom,
          normalizedTo,
          ex);
      return Optional.empty();
    }
  }

  private String normalize(String currency) {

    return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
  }
}
