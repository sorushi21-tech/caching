package com.caching.transaction_service_simple_cache.repository;

import java.util.List;

public interface TransactionSummaryRepository {

    List<CategoryCurrencyTotalProjection> findCategoryCurrencyTotals();
}

