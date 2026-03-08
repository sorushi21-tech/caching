package com.caching.transaction_service_simple_cache.dto;

import java.math.BigDecimal;

public record CategorySummaryResponse(String category, BigDecimal totalAmount, String currency) {}
