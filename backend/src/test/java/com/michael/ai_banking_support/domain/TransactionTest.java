package com.michael.ai_banking_support.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.michael.ai_banking_support.exception.InvalidTransactionStateException;

class TransactionTest {
    
    @Test
    void shouldCompletePendingTransaction() {
        // Arrange
        var transaction = createPendingTransaction();

        // Act
        transaction.complete();

        // Assert
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
    }

    @Test
    void shouldNotCompleteAlreadyCompletedTransaction() {
        // Arrange
        var transaction = createPendingTransaction();
        transaction.complete();

        // Act
        var exception = assertThrows(
                InvalidTransactionStateException.class, 
                transaction::complete // () -> transaction.complete()
        );

        // Assert
        assertEquals(
            "Transaction cannot be updated because its current status is COMPLETED",
            exception.getMessage()
        );
    }

    @Test
    void shouldFailPendingTransaction() {
        // Arrange
        var transaction = createPendingTransaction();

        // Act
        transaction.fail();

        // Assert
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    void shouldNotFailAlreadyCompletedTransaction() {
        // Arrange
        var transaction = createPendingTransaction();
        transaction.complete();

        // Act + Assert
        var exception = assertThrows(
            InvalidTransactionStateException.class, 
            transaction::fail);

        assertEquals(
            "Transaction cannot be updated because its current status is COMPLETED",
            exception.getMessage()
        );

        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
    }

    private Transaction createPendingTransaction() {
        return new Transaction(
            "transaction-1",
            "test-idempotency-key",
            "customer-1",
            "Coffee purchase",
            new BigDecimal("4.50"),
            "AUD"
        );
    }
}
