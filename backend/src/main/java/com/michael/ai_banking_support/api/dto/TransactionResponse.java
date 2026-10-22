package com.michael.ai_banking_support.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;

public record TransactionResponse(
    String id,
    String customerId,
    String description,
    BigDecimal amount,
    String currency,
    Instant createdAt,
    TransactionStatus status,
    Long version
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt(),
                transaction.getStatus(),
                transaction.getVersion()
        );
    }
}
