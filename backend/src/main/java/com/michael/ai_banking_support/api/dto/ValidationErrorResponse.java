package com.michael.ai_banking_support.api.dto;

import java.util.Map;

public record ValidationErrorResponse(Map<String, String> errors) {
    
}
