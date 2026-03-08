package com.caching.transaction_service_caffeine_cache.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(String fromCurrency, String toCurrency, BigDecimal rate) {}
