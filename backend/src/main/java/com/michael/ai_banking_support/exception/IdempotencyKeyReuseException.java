package com.michael.ai_banking_support.exception;

public class IdempotencyKeyReuseException extends RuntimeException {
    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key has already been used with a different request: " + idempotencyKey);
    }
}
