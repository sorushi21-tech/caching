package com.caching.transaction_service_caching.service;

import com.caching.transaction_service_caching.client.ExchangeRateClient;
import com.caching.transaction_service_caching.dto.CategorySummaryResponse;
import com.caching.transaction_service_caching.dto.TransactionRequest;
import com.caching.transaction_service_caching.model.Transaction;
import com.caching.transaction_service_caching.repository.CategoryCurrencyTotalProjection;
import com.caching.transaction_service_caching.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final ExchangeRateClient exchangeRateClient;

  public Transaction createTransaction(TransactionRequest request) {

    log.debug(
        "Preparing transaction entity for category={} currency={} amount={}",
        request.category(),
        request.currency(),
        request.amount());
    Transaction transaction =
        Transaction.create(
            request.amount(),
            request.currency().toUpperCase(Locale.ROOT),
            request.category(),
            request.date());

    Transaction saved = transactionRepository.save(transaction);
    log.info(
        "Saved transaction id={} category={} currency={} amount={}",
        saved.id(),
        saved.category(),
        saved.currency(),
        saved.amount());
    return saved;
  }

  public List<Transaction> getTransactions() {
    List<Transaction> transactions = transactionRepository.findAll();
    log.debug("Loaded {} transactions from repository", transactions.size());
    return transactions;
  }

  public List<CategorySummaryResponse> getConvertedSummary(String toCurrency) {

    String targetCurrency = toCurrency.toUpperCase(Locale.ROOT);
    log.info("Building converted summary for targetCurrency={}", targetCurrency);
    List<CategoryCurrencyTotalProjection> projections =
        transactionRepository.findCategoryCurrencyTotals();
    log.debug("Loaded {} grouped category/currency projections", projections.size());

    Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();

    for (CategoryCurrencyTotalProjection projection : projections) {

      log.debug(
          "Fetching rate for projection category={} fromCurrency={} targetCurrency={} amount={}",
          projection.getCategory(),
          projection.getCurrency(),
          targetCurrency,
          projection.getTotalAmount());
      BigDecimal rate = exchangeRateClient.getRate(projection.getCurrency(), targetCurrency);

      BigDecimal convertedAmount =
          projection.getTotalAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

      totalsByCategory.merge(projection.getCategory(), convertedAmount, BigDecimal::add);
    }

    List<CategorySummaryResponse> response = new ArrayList<>();
    for (Map.Entry<String, BigDecimal> entry : totalsByCategory.entrySet()) {
      response.add(
          new CategorySummaryResponse(
              entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP), targetCurrency));
    }

    log.info(
        "Converted summary generated for targetCurrency={} categories={}",
        targetCurrency,
        response.size());
    return response;
  }
}
