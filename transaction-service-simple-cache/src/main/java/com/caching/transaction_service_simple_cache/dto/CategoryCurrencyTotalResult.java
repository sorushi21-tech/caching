package com.caching.transaction_service_simple_cache.dto;

import com.caching.transaction_service_simple_cache.repository.CategoryCurrencyTotalProjection;

import java.math.BigDecimal;

public record CategoryCurrencyTotalResult(String category, String currency, BigDecimal totalAmount)
        implements CategoryCurrencyTotalProjection {

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}

