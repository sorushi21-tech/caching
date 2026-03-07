package com.caching.transaction_service_caching.repository;

import com.caching.transaction_service_caching.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository extends MongoRepository<Transaction, String>, TransactionSummaryRepository {
}