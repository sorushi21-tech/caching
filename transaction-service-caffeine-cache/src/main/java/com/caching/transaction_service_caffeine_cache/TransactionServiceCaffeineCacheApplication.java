package com.caching.transaction_service_caffeine_cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class TransactionServiceCaffeineCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceCaffeineCacheApplication.class, args);
    }
}

