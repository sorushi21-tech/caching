package com.caching.transaction_service_caching.repository;

import com.caching.transaction_service_caching.dto.CategoryCurrencyTotalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class TransactionSummaryRepositoryImpl implements TransactionSummaryRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<CategoryCurrencyTotalProjection> findCategoryCurrencyTotals() {

        log.debug("Running category/currency totals aggregation");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("category", "currency").sum("amount").as("totalAmount"),
                Aggregation.project("totalAmount")
                        .and("_id.category").as("category")
                        .and("_id.currency").as("currency")
        );

        AggregationResults<CategoryCurrencyTotalResult> results =
                mongoTemplate.aggregate(aggregation, "transactions", CategoryCurrencyTotalResult.class);

        List<CategoryCurrencyTotalProjection> mappedResults = new ArrayList<>(results.getMappedResults());
        log.debug("Category/currency totals aggregation produced {} rows", mappedResults.size());
        return mappedResults;
    }
}