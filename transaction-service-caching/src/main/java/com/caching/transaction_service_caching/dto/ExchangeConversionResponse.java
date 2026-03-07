package com.caching.transaction_service_caching.dto;

import java.math.BigDecimal;

public record ExchangeConversionResponse(String fromCurrency, String toCurrency, BigDecimal originalAmount,
                                         BigDecimal convertedAmount) {
}