package com.michael.ai_banking_support.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Page<Transaction> findByCustomerId(String customerId, Pageable pageable);
    Page<Transaction> findByCustomerIdAndStatus(String customerId, TransactionStatus status, Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}