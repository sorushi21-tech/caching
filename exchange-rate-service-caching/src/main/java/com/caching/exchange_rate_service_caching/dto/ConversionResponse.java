package com.caching.exchange_rate_service_caching.dto;

import java.math.BigDecimal;

public record ConversionResponse(String fromCurrency, String toCurrency, BigDecimal originalAmount,
                                 BigDecimal convertedAmount) {
}