package com.caching.transaction_service_redis_cache.repository;

import java.util.List;

public interface TransactionSummaryRepository {

  List<CategoryCurrencyTotalProjection> findCategoryCurrencyTotals();
}
