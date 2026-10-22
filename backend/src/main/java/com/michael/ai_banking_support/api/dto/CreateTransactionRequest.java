package com.michael.ai_banking_support.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateTransactionRequest( // record 让request创建后不可变，避免某段代码意外修改它 -> 数据在处理过程中保持一致, 这样业务更安全、可预测，也更容易排查问题。
    @NotBlank(message = "customerId must not be blank")
    String customerId,

    @NotBlank(message = "description must not be blank")
    String description,

    @Positive(message = "amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "currency must not be blank")
    @Pattern(
        regexp = "^[A-Z]{3}$",
        message = "currency must be a three-letter uppercase code"
    )
    String currency
) {
}
