package com.caching.transaction_service_simple_cache.repository;

import com.caching.transaction_service_simple_cache.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository extends MongoRepository<Transaction, String>, TransactionSummaryRepository {
}

