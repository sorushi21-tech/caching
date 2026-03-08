package com.caching.transaction_service_caffeine_cache.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Builder
@Document(collection = "transactions")
public record Transaction(
    @Id String id,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal amount,
    String currency,
    String category,
    LocalDate date) {

  public static Transaction create(
      BigDecimal amount, String currency, String category, LocalDate date) {
    return Transaction.builder()
        .amount(amount)
        .currency(currency)
        .category(category)
        .date(date)
        .build();
  }
}
