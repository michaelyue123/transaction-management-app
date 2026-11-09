package com.michael.ai_banking_support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private final Transaction transactionA1 = new Transaction(
                UUID.randomUUID().toString(),
                "1234",
                "customer-A",
                "Water bill",
                new BigDecimal("50.00"),
                "AUD"
    );

    private final Transaction transactionA2 = new Transaction(
            UUID.randomUUID().toString(),
            "1235",
            "customer-A",
            "Gas bill",
            new BigDecimal("20.00"),
            "AUD"
    );

    private final Transaction transactionA3 = new Transaction(
            UUID.randomUUID().toString(),
            "1236",
            "customer-A",
            "Electricity bill",
            new BigDecimal("79.00"),
            "AUD"
    );

    private final Transaction transactionB1 = new Transaction(
                UUID.randomUUID().toString(),
                "1237",
                "customer-B",
                "Internet bill",
                new BigDecimal("45.00"),
                "AUD"
    );

    private final Transaction transactionC1 = new Transaction(
                "transaction-001",
                "1238",
                "customer-C",
                "Grocery bill",
                new BigDecimal("90.0"),
                "AUD"
    );

    private final Transaction transactionC2 = new Transaction(
                "transaction-002",
                "1239",
                "customer-C",
                "Credit Card bill",
                new BigDecimal("1500.0"),
                "AUD"
    );

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

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


    @Test
    void findTransactions_shouldReturnPagesForRequestedCustomer() throws Exception {
        // Arrange
        // 创建并保存用户 A 的 3 条交易 + B 的 1 条交易
        transactionRepository.saveAllAndFlush(List.of(transactionA1, transactionA2, transactionA3, transactionB1));
        //
        // 给交易使用不同的 description，方便判断返回了哪几条。
        // 确保排序字段的值可控，这样才能可靠地验证顺序。

        // Act & Assert: 第一页
        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "description,asc")) // 规定排序规则
                .andExpect(status().isOk())
                // 验证 transactions 长度为 2
                .andExpect(jsonPath("$.transactions.length()").value(2))
                // 验证 currentPage 为 0
                .andExpect(jsonPath("$.currentPage").value(0))
                // 验证 pageSize 为 2
                .andExpect(jsonPath("$.pageSize").value(2))
                // 验证 totalItems 为 3
                .andExpect(jsonPath("$.totalItems").value(3))
                // 验证 totalPages 为 2
                .andExpect(jsonPath("$.totalPages").value(2))
                // 验证返回的是用户 A 的预期交易
                .andExpect(jsonPath("$.transactions[0].customerId").value("customer-A"))
                .andExpect(jsonPath("$.transactions[1].customerId").value("customer-A"))
                .andExpect(jsonPath("$.transactions[0].description").value("Electricity bill"))
                .andExpect(jsonPath("$.transactions[1].description").value("Gas bill"));

        // Act & Assert: 第二页
        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                // 验证 transactions 长度为 1
                .andExpect(jsonPath("$.transactions.length()").value(1))
                // 验证 currentPage 为 1
                .andExpect(jsonPath("$.currentPage").value(1))
                // 验证 pageSize 为 2
                .andExpect(jsonPath("$.pageSize").value(2))
                // 验证 totalItems 为 3
                .andExpect(jsonPath("$.totalItems").value(3))
                // 验证 totalPages 为 2
                .andExpect(jsonPath("$.totalPages").value(2))
                // 验证返回的是用户 A 的剩余交易
                .andExpect(jsonPath("$.transactions[0].customerId").value("customer-A"))
                .andExpect(jsonPath("$.transactions[0].description").value("Water bill"));

        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-B")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "description,asc")) // 规定排序规则
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                // 验证 currentPage 为 0
                .andExpect(jsonPath("$.currentPage").value(0))
                // 验证 pageSize 为 2
                .andExpect(jsonPath("$.pageSize").value(2))
                // 验证 totalItems 为 1
                .andExpect(jsonPath("$.totalItems").value(1))
                // 验证 totalPages 为 1
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.transactions[0].customerId").value("customer-B"))
                .andExpect(jsonPath("$.transactions[0].description").value("Internet bill"))
                .andExpect(jsonPath("$.transactions[0].amount").value(45.0));
    }

    @Test
    void findTransactions_withStatus_shouldReturnOnlyMatchingTransactions()
            throws Exception {

        // Arrange
        // 创建并保存上表中的 4 条交易
        // 注意：创建后将 Electricity bill 转成 COMPLETED
        String transactionId = transactionA3.getId();

        transactionRepository.saveAllAndFlush(
            List.of(transactionA1, transactionA2, transactionA3, transactionB1));

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        
        transaction.complete();

        transactionRepository.saveAndFlush(transaction);
        
        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "description,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                // 验证这一页有 1 条交易
                .andExpect(jsonPath("$.transactions.length()").value(1))
                // 验证 totalItems 为 2、totalPages 为 2
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                // 验证返回的是 Gas bill，状态为 PENDING
                .andExpect(jsonPath("$.transactions[0].description").value("Gas bill"))
                .andExpect(jsonPath("$.transactions[0].status").value("PENDING"));

         mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A")
                        .param("status", "PENDING")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "description,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                // 验证这一页有 1 条交易
                .andExpect(jsonPath("$.transactions.length()").value(1))
                // 验证 totalItems 为 2、totalPages 为 2
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                // 验证返回的是 Water bill，状态为 PENDING
                .andExpect(jsonPath("$.transactions[0].description").value("Water bill"))
                .andExpect(jsonPath("$.transactions[0].status").value("PENDING"));

        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A")
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "description,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                // 验证返回的是 Electricity bill，状态为 COMPLETED
                .andExpect(jsonPath("$.transactions[0].description").value("Electricity bill"))
                .andExpect(jsonPath("$.transactions[0].status").value("COMPLETED"));
    }

    @Test
        void findTransactions_withoutSort_shouldOrderByCreatedAtDescending()
                throws Exception {

        // Arrange
        // 创建并保存同一用户的 3 条交易
        ReflectionTestUtils.setField(
                transactionA1,
                "createdAt",
                Instant.parse("2026-01-01T10:00:00Z")
        );

        ReflectionTestUtils.setField(
                transactionA2,
                "createdAt",
                Instant.parse("2026-01-01T11:00:00Z")
        );

        ReflectionTestUtils.setField(
                transactionA3,
                "createdAt",
                Instant.parse("2026-01-01T12:00:00Z")
        );

        ReflectionTestUtils.setField(
                transactionC1,
                "createdAt",
                Instant.parse("2026-01-02T12:00:00Z")
        );

        ReflectionTestUtils.setField(
                transactionC2,
                "createdAt",
                Instant.parse("2026-01-02T12:00:00Z")
        );

        transactionRepository.saveAllAndFlush(
            List.of(transactionA1, transactionA2, transactionA3, transactionC1, transactionC2));
        

        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(3))
                // 验证 Electricity bill 在第 0 位
                .andExpect(jsonPath("$.transactions[0].description").value("Electricity bill"))
                // 验证 Gas bill 在第 1 位
                .andExpect(jsonPath("$.transactions[1].description").value("Gas bill"))
                // 验证 Water bill 在第 2 位
                .andExpect(jsonPath("$.transactions[2].description").value("Water bill"));

        mockMvc.perform(get("/transactions")
                        .param("customerId", "customer-C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                // 验证 Credit Card bill 在第 0 位
                .andExpect(jsonPath("$.transactions[0].description").value("Credit Card bill"))
                // 验证 Grocery bill 在第 1 位
                .andExpect(jsonPath("$.transactions[1].description").value("Grocery bill"))
                .andExpect(jsonPath("$.transactions[0].id").value("transaction-002"))
                .andExpect(jsonPath("$.transactions[1].id").value("transaction-001"));
        }
}
