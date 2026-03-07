package com.caching.transaction_service_caching.controller;

import com.caching.transaction_service_caching.dto.CategorySummaryResponse;
import com.caching.transaction_service_caching.dto.TransactionRequest;
import com.caching.transaction_service_caching.model.Transaction;
import com.caching.transaction_service_caching.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public Transaction createTransaction(@RequestBody TransactionRequest request) {

        log.info("Create transaction request received: category={} currency={} amount={}", request.category(), request.currency(), request.amount());
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
    public List<CategorySummaryResponse> getConvertedSummary(
            @RequestParam String toCurrency) {

        log.info("Get converted summary request received for toCurrency={}", toCurrency);
        List<CategorySummaryResponse> summary = transactionService.getConvertedSummary(toCurrency);
        log.debug("Returning {} category summary rows for toCurrency={}", summary.size(), toCurrency);
        return summary;
    }
}