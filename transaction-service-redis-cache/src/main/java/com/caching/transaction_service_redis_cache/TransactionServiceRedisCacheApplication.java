package com.caching.transaction_service_redis_cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TransactionServiceRedisCacheApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransactionServiceRedisCacheApplication.class, args);
  }
}
