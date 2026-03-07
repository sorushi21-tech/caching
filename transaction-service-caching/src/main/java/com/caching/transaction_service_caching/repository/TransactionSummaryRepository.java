package com.caching.transaction_service_caching.repository;

import java.util.List;

public interface TransactionSummaryRepository {

    List<CategoryCurrencyTotalProjection> findCategoryCurrencyTotals();
}