package com.michael.ai_banking_support.exception;

import com.michael.ai_banking_support.domain.TransactionStatus;

public class InvalidTransactionStateException extends RuntimeException {    
    public InvalidTransactionStateException(TransactionStatus currentStatus) {
        super("Transaction cannot be updated because its current status is " + currentStatus);
    } 
}
