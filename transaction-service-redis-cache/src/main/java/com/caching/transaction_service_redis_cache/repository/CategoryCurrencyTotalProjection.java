package com.caching.transaction_service_redis_cache.repository;

import java.math.BigDecimal;

public interface CategoryCurrencyTotalProjection {

  String getCategory();

  String getCurrency();

  BigDecimal getTotalAmount();
}
