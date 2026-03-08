package com.caching.transaction_service_caffeine_cache.repository;

import java.util.List;

public interface TransactionSummaryRepository {

  List<CategoryCurrencyTotalProjection> findCategoryCurrencyTotals();
}
