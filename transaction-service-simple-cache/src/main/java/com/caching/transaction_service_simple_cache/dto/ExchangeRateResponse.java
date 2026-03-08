package com.caching.transaction_service_simple_cache.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(String fromCurrency, String toCurrency, BigDecimal rate) {}
