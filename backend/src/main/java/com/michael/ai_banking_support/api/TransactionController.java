package com.michael.ai_banking_support.api;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.michael.ai_banking_support.api.dto.CreateTransactionRequest;
import com.michael.ai_banking_support.api.dto.PageResponse;
import com.michael.ai_banking_support.api.dto.TransactionResponse;
import com.michael.ai_banking_support.domain.Transaction;
import com.michael.ai_banking_support.domain.TransactionStatus;
import com.michael.ai_banking_support.service.CreateTransactionResult;
import com.michael.ai_banking_support.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins="http://localhost:5173")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "amount",
            "id",
            "description"
    );

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createTransaction(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateTransactionRequest request) {
        // Process POST request
        CreateTransactionResult result = transactionService.createTransaction(
            request,
            idempotencyKey
        );

        var response = TransactionResponse.from(result.transaction());

        if(result.created()) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}") // @PathVariable → 资源身份
    public TransactionResponse getTransactionById(@PathVariable String id) {
        var transaction = transactionService.getTransactionById(id);
        return TransactionResponse.from(transaction);
    }
    
    @GetMapping // @RequestParam → 查询、筛选、分页、排序等条件
    public PageResponse findTransactions(
        @RequestParam String customerId,
        @RequestParam(required = false) TransactionStatus status,
        @PageableDefault(
            size = 20,
            sort = {"createdAt", "id"},
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {
        for(Sort.Order order : pageable.getSort()) {
            String field = order.getProperty();
 
            if(!ALLOWED_SORT_FIELDS.contains(field)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported sort field: " + field
                );
            }
        }

        Page<Transaction> currentPage = transactionService.findTransactions(customerId, status, pageable);
        List<TransactionResponse> transactions = currentPage.getContent()
                                                            .stream()
                                                            .map(TransactionResponse::from)
                                                            .toList();

        return new PageResponse(
            transactions,
            currentPage.getNumber(),
            currentPage.getSize(),
            currentPage.getTotalElements(),
            currentPage.getTotalPages()
        );     
    }

    @PostMapping("/{id}/complete")
    public TransactionResponse completeTransaction(@PathVariable String id) {
        var transaction = transactionService.completeTransaction(id);
        return TransactionResponse.from(transaction);
    }

    @PostMapping("/{id}/fail")
    public TransactionResponse failTransaction(@PathVariable String id) {
        var transaction = transactionService.failTransaction(id);
        return TransactionResponse.from(transaction);
    }
    
}
