package com.caching.transaction_service_caffeine_cache.repository;

import com.caching.transaction_service_caffeine_cache.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository
    extends MongoRepository<Transaction, String>, TransactionSummaryRepository {}
