package com.michael.ai_banking_support.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String id) {
        super("Transaction not found " + id);
    }
}
