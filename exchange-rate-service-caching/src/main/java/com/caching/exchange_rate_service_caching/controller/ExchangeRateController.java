package com.caching.exchange_rate_service_caching.controller;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.caching.exchange_rate_service_caching.dto.RateResponse;
import com.caching.exchange_rate_service_caching.service.ExchangeRateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    public RateResponse getRate(@RequestParam String from,
                                @RequestParam String to) {

        log.info("Rate request received: from={} to={}", from, to);

        try {
            var rate = exchangeRateService.findRate(from, to);
            log.debug("Rate lookup successful: from={} to={} rate={}", from, to, rate);
            return new RateResponse(
                    from.toUpperCase(Locale.ROOT),
                    to.toUpperCase(Locale.ROOT),
                    rate
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Rate lookup failed: from={} to={} reason={}", from, to, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}