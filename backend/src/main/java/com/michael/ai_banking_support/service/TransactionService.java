package com.michael.ai_banking_support.service;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.michael.ai_banking_support.api.dto.CreateTransactionRequest;
import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;
import com.michael.ai_banking_support.exception.IdempotencyKeyReuseException;
import com.michael.ai_banking_support.exception.TransactionNotFoundException;
import com.michael.ai_banking_support.repository.TransactionRepository;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public CreateTransactionResult createTransaction(CreateTransactionRequest request, String idempotencyKey) {
        var existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey); // 幂等

        if(existingTransaction.isPresent()) {
            Transaction transaction = existingTransaction.get();
            if(!transaction.matchesCreationRequest(
                request.customerId(), 
                request.description(), 
                request.amount(), 
                request.currency())) {
                    throw new IdempotencyKeyReuseException(idempotencyKey);
            }

            return new CreateTransactionResult(transaction, false);
        }

        var transaction = new Transaction(
            UUID.randomUUID().toString(), 
            idempotencyKey,
            request.customerId(), 
            request.description(), 
            request.amount(), 
            request.currency()
        );

        // 并发处理
        try {
            return new CreateTransactionResult(
                transactionRepository.saveAndFlush(transaction),
                true
            );
        } catch (DataIntegrityViolationException exception) {
            Transaction existing = transactionRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);

            if(!existing.matchesCreationRequest(
                request.customerId(), 
                request.description(), 
                request.amount(), 
                request.currency())) {

                throw new IdempotencyKeyReuseException(idempotencyKey);
            }

            return new CreateTransactionResult(existing, false);
        }
    }

    public Transaction getTransactionById(String id) {
        return transactionRepository
                    .findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    public List<Transaction> findTransactions(String customerId, TransactionStatus status) {
        if(status == null) {
            return transactionRepository.findByCustomerId(customerId);
        }
        return transactionRepository.findByCustomerIdAndStatus(customerId, status);
    }

    @Transactional
    /*
        进入 @Transactional 方法
        → getTransactionById() 查询实体
        → transaction 成为 managed entity
        → complete()/fail() 修改内存状态
        → 方法正常结束
        → Hibernate flush
        → 执行带 version 条件的 UPDATE
        → Spring commit
     */
    public Transaction completeTransaction(String id) {
        var transaction = getTransactionById(id);

        transaction.complete();

        transactionRepository.flush(); // 明确要求响应里的 version 必须是更新后的值

        return transaction;
    }

    @Transactional
    public Transaction failTransaction(String id) {
        var transaction = getTransactionById(id);

        transaction.fail();

        transactionRepository.flush(); // 明确要求响应里的 version 必须是更新后的值

        return transaction;
    }
}
