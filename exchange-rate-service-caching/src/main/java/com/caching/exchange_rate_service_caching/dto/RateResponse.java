package com.caching.exchange_rate_service_caching.dto;

import java.math.BigDecimal;

public record RateResponse(String fromCurrency, String toCurrency, BigDecimal rate) {
}
