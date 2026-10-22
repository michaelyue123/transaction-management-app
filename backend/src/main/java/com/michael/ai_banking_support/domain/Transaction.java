package com.michael.ai_banking_support.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.michael.ai_banking_support.exception.InvalidTransactionStateException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "transactions")
public class Transaction {
    /*
        外部只能创建 PENDING 交易
        createdAt 由系统生成
        无效字段会在创建对象时立即报错
    */
    public Transaction(
            String id, 
            String idempotencyKey, // 幂等
            String customerId, 
            String description, 
            BigDecimal amount, 
            String currency) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        if(idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        if (customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }

        this.description = Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Transaction() {}

    @Version
    private Long version; // 乐观锁

    @Column(nullable = false, unique = true, updatable = false)
    private String idempotencyKey;
    
    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false) 
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void complete() {
        // 只有 PENDING 才能变成 COMPLETED
        ensurePending();
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail() {
        // 只有 PENDING 才能变成 FAILED
        ensurePending();
        this.status = TransactionStatus.FAILED;
    }

    private void ensurePending() {
        if(this.status != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(this.status);
        }
    }

    public boolean matchesCreationRequest(
        String customerId,
        String description,
        BigDecimal amount,
        String currency
    ) {
        return this.customerId.equals(customerId)
                && this.description.equals(description)
                && this.amount.compareTo(amount) == 0 // 不用equal, 避免因小数位不同被误判为不同金额 例如4.5和4.50
                && this.currency.equals(currency);
    }
}
