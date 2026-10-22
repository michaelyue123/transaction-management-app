package com.michael.ai_banking_support.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.michael.ai_banking_support.api.dto.CreateTransactionRequest;
import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;
import com.michael.ai_banking_support.exception.IdempotencyKeyReuseException;
import com.michael.ai_banking_support.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository);
    }

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {
        // Arrange
        var existingTransaction = new Transaction(
            "transaction-1",
            "key-1",
            "customer-1",
            "Internet bill",
            new BigDecimal("79.00"),
            "AUD"
        );

        var request = new CreateTransactionRequest(
            "customer-1", 
            "Internet bill", 
            new BigDecimal("79.00"), 
            "AUD");

        when(transactionRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.of(existingTransaction));

        // Act
        var result = transactionService.createTransaction(request, "key-1");

        // Assert
        assertFalse(result.created());
        assertSame(existingTransaction, result.transaction());

        verify(transactionRepository, never()).saveAndFlush(existingTransaction);
    }

    @Test
    void shouldThrowKeyReuseExceptionForSameIdempotencyKeyAndDifferentRequest() {
        // Arrange
        var existingTransaction = new Transaction(
            "transaction-1",
            "key-1",
            "customer-1",
            "Internet bill",
            new BigDecimal("79.00"),
            "AUD"
        );

        var request = new CreateTransactionRequest(
            "customer-1", 
            "Coffee bill", 
            new BigDecimal("4.50"), 
            "AUD");

        when(transactionRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.of(existingTransaction));

        // Act
        var exception = assertThrows(
            IdempotencyKeyReuseException.class, 
            () -> transactionService.createTransaction(request, "key-1")
        );

        // Assert
        assertEquals(
            "Idempotency key has already been used with a different request: key-1",
            exception.getMessage()
        );

        verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldCreateTransactionForNewIdempotencyKey() {
        // Arrange
        var request = new CreateTransactionRequest(
            "customer-1", 
            "Coffee bill", 
            new BigDecimal("4.50"), 
            "AUD");

        when(transactionRepository.findByIdempotencyKey("key-new"))
                .thenReturn(Optional.empty());

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Transaction.class));

        // Act
        var result = transactionService.createTransaction(request, "key-new");

        // Assert
        assertTrue(result.created());
        assertEquals("customer-1", result.transaction().getCustomerId());
        assertEquals("Coffee bill", result.transaction().getDescription());
        assertEquals(0, new BigDecimal("4.50").compareTo(result.transaction().getAmount()));
        assertEquals(TransactionStatus.PENDING, result.transaction().getStatus());

        verify(transactionRepository).saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldReturnExistingTransactionWhenConcurrentRequestCreatesSameKey() {
        // Arrange
        var existingTransaction = new Transaction(
            "transaction-1",
            "key-1",
            "customer-1",
            "Internet bill",  
            new BigDecimal("79.00"),
            "AUD"
        );

        var request = new CreateTransactionRequest(
            "customer-1",
            "Internet bill",
            new BigDecimal("79.00"),
            "AUD"
        );

        when(transactionRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));

        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act
        var result = transactionService.createTransaction(request, "key-1");

        // Assert
        assertFalse(result.created());
        assertSame(existingTransaction, result.transaction());

        verify(transactionRepository, times(2))
                .findByIdempotencyKey("key-1");
        verify(transactionRepository).saveAndFlush(any(Transaction.class));
    }

    @Test
    void shouldCompletePendingTransaction() {
        // Arrange
        var transaction = new Transaction(
            "transaction-1",
            "key-1",
            "customer-1",
            "Internet bill",  
            new BigDecimal("79.00"),
            "AUD"
        );

        when(transactionRepository.findById("transaction-1"))
                .thenReturn(Optional.of(transaction));

        var result = transactionService.completeTransaction("transaction-1");

        assertEquals(TransactionStatus.COMPLETED, result.getStatus());

        verify(transactionRepository, times(1))
                .flush();
        
        verify(transactionRepository, never())
                .saveAndFlush(any(Transaction.class));
    }

}
