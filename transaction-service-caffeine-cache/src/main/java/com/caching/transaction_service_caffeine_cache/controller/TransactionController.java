package com.caching.transaction_service_caffeine_cache.controller;

import com.caching.transaction_service_caffeine_cache.client.ExchangeRateClient;
import com.caching.transaction_service_caffeine_cache.dto.CategorySummaryResponse;
import com.caching.transaction_service_caffeine_cache.dto.TransactionRequest;
import com.caching.transaction_service_caffeine_cache.model.Transaction;
import com.caching.transaction_service_caffeine_cache.service.TransactionService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping
public class TransactionController {

  private final TransactionService transactionService;
  private final ExchangeRateClient exchangeRateClient;

  public TransactionController(
      TransactionService transactionService, ExchangeRateClient exchangeRateClient) {
    this.transactionService = transactionService;
    this.exchangeRateClient = exchangeRateClient;
  }

  @PostMapping("/transactions")
  public Transaction createTransaction(@RequestBody TransactionRequest request) {

    log.info(
        "Create transaction request received: category={} currency={} amount={}",
        request.category(),
        request.currency(),
        request.amount());
    Transaction transaction = transactionService.createTransaction(request);
    log.info("Transaction created successfully: id={}", transaction.id());
    return transaction;
  }

  @GetMapping("/transactions")
  public List<Transaction> getTransactions() {

    log.info("Get transactions request received");
    List<Transaction> transactions = transactionService.getTransactions();
    log.debug("Returning {} transactions", transactions.size());
    return transactions;
  }

  @GetMapping("/summary")
  public List<CategorySummaryResponse> getConvertedSummary(@RequestParam String toCurrency) {

    log.info("Get converted summary request received for toCurrency={}", toCurrency);
    List<CategorySummaryResponse> summary = transactionService.getConvertedSummary(toCurrency);
    log.debug("Returning {} category summary rows for toCurrency={}", summary.size(), toCurrency);
    return summary;
  }

  @DeleteMapping("/cache/fx-rates")
  public String evictFxRate(@RequestParam String from, @RequestParam String to) {

    log.info("Evict fx rate cache request: from={} to={}", from, to);
    exchangeRateClient.evictRate(from.toUpperCase(), to.toUpperCase());
    return "Evicted cached rate for " + from.toUpperCase() + " -> " + to.toUpperCase();
  }

  @DeleteMapping("/cache/fx-rates/all")
  public String evictAllFxRates() {

    log.info("Evict all fx rates cache request");
    exchangeRateClient.evictAllRates();
    return "All cached exchange rates evicted";
  }

  @DeleteMapping("/cache/transactions")
  public String evictTransactionsCache() {

    log.info("Evict transactions cache request");
    transactionService.evictTransactionsCache();
    return "Transactions cache evicted";
  }
}
