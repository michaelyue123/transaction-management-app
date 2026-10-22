package com.michael.ai_banking_support.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByCustomerId(String customerId);
    List<Transaction> findByCustomerIdAndStatus(String customerId, TransactionStatus status);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
