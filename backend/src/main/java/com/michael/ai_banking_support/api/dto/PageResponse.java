package com.michael.ai_banking_support.api.dto;

import java.util.List;

public record PageResponse(
    List<TransactionResponse> transactions,
    int currentPage,
    int pageSize,
    long totalItems,
    int totalPages
) {
    
}
