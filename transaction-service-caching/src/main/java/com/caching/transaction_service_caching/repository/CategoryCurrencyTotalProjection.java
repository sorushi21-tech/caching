package com.caching.transaction_service_caching.repository;

import java.math.BigDecimal;

public interface CategoryCurrencyTotalProjection {

    String getCategory();

    String getCurrency();

    BigDecimal getTotalAmount();
}