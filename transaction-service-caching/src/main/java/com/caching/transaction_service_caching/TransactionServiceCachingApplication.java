package com.caching.transaction_service_caching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TransactionServiceCachingApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransactionServiceCachingApplication.class, args);
  }
}
