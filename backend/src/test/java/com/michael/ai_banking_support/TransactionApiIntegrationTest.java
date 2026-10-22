package com.michael.ai_banking_support;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.support.TransactionTemplate;

import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.repository.TransactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "DELETE FROM transactions", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TransactionApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldHandleIdempotentCreateRequests() throws Exception {
        String key = "integration-test-key-1";
        
        // """ 是 Java 的 text block 语法，用来写多行字符串
        String originalRequest = """ 
                {
                    "customerId": "customer-1",
                    "description": "Internet bill",
                    "amount": 79.00,
                    "currency": "AUD"
                }
                """;

        String differentRequest = """
                {
                    "customerId": "customer-1",
                    "description": "Large transfer",
                    "amount": 5000.00,
                    "currency": "AUD"
                }        
                """;

        // First request: create a new transaction.
        mockMvc.perform(MockMvcRequestBuilders.post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(originalRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value("customer-1"))
                    .andExpect(jsonPath("$.description").value("Internet bill"))
                    .andExpect(jsonPath("$.status").value("PENDING"));

        // Same key + same request: returns the existing transaction.
        mockMvc.perform(MockMvcRequestBuilders.post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(originalRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.description").value("Internet bill"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Same key + different request: rejected
        mockMvc.perform(MockMvcRequestBuilders.post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(differentRequest))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                        "Idempotency key has already been used with a different request: " + key
                    ));
    }

    @Test
    void shouldRejectStaleTransactionUpdate() {
        // Arrange
        Transaction savedTransaction = transactionRepository.saveAndFlush(
            new Transaction(
                UUID.randomUUID().toString(),
                "1234",
                "customer-1",
                "Internet bill",
                new BigDecimal("79.00"),
                "AUD"
            )
        );

        String transactionId = savedTransaction.getId();

        // 每次 execute() 都创建独立事务和 persistence context，因此得到两个独立对象
        Transaction transactionA = transactionTemplate.execute((transactionStatus) -> 
                transactionRepository.findById(transactionId).orElseThrow()
        );

        Transaction transactionB = transactionTemplate.execute((transactionStatus) -> 
                transactionRepository.findById(transactionId).orElseThrow()
        );

        // Act: A updates successfully
        transactionA.complete();

        transactionTemplate.executeWithoutResult((transactionStatus) -> 
                transactionRepository.saveAndFlush(transactionA)
        );

        // Act & Assert: B uses the stale version
        transactionB.fail();

        var result = assertThrows(
            ObjectOptimisticLockingFailureException.class, 
            () -> transactionTemplate.executeWithoutResult((transactionStatus) -> 
                    transactionRepository.saveAndFlush(transactionB))
        );

        assertEquals(ObjectOptimisticLockingFailureException.class, result.getClass());
    }
}
