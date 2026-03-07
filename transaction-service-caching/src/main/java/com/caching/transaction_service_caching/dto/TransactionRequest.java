package com.caching.transaction_service_caching.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(BigDecimal amount, String currency, String category, LocalDate date) {
}