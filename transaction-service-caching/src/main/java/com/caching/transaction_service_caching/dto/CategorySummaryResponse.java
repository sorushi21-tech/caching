package com.caching.transaction_service_caching.dto;

import java.math.BigDecimal;

public record CategorySummaryResponse(String category, BigDecimal totalAmount, String currency) {
}