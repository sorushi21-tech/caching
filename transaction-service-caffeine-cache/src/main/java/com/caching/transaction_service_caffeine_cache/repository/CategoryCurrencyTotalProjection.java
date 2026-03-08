package com.caching.transaction_service_caffeine_cache.repository;

import java.math.BigDecimal;

public interface CategoryCurrencyTotalProjection {

    String getCategory();

    String getCurrency();

    BigDecimal getTotalAmount();
}

