package com.caching.transaction_service_simple_cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class TransactionServiceSimpleCacheApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransactionServiceSimpleCacheApplication.class, args);
  }
}
