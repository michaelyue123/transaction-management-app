package com.michael.ai_banking_support.service;

import com.michael.ai_banking_support.domain.Transaction;

public record CreateTransactionResult(
    Transaction transaction,
    boolean created
) {
    
}
