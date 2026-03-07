package com.caching.exchange_rate_service_caching.controller;

import com.caching.exchange_rate_service_caching.dto.ConversionResponse;
import com.caching.exchange_rate_service_caching.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/convert")
    public ConversionResponse convert(@RequestParam String from,
                                      @RequestParam String to,
                                      @RequestParam BigDecimal amount) {

        log.info("Conversion request received: from={} to={} amount={}", from, to, amount);

        try {
            BigDecimal convertedAmount = exchangeRateService.convert(from, to, amount);
            log.debug("Conversion successful: from={} to={} amount={} convertedAmount={}", from, to, amount, convertedAmount);
            return new ConversionResponse(
                    from.toUpperCase(Locale.ROOT),
                    to.toUpperCase(Locale.ROOT),
                    amount,
                    convertedAmount
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Conversion failed: from={} to={} amount={} reason={}", from, to, amount, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}