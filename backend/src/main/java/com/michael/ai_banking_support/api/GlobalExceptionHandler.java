package com.michael.ai_banking_support.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.michael.ai_banking_support.api.dto.ValidationErrorResponse;
import com.michael.ai_banking_support.exception.ApiError;
import com.michael.ai_banking_support.exception.IdempotencyKeyReuseException;
import com.michael.ai_banking_support.exception.InvalidTransactionStateException;
import com.michael.ai_banking_support.exception.TransactionNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTransactionNotFound(
        TransactionNotFoundException exception) {
            
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationError(
        MethodArgumentNotValidException exception) {
            
            Map<String, String> errors = new LinkedHashMap<>();

            for(FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ValidationErrorResponse(errors));
    }

    @ExceptionHandler(InvalidTransactionStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransactionState(
        InvalidTransactionStateException exception
    ) {
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<Map<String, String>> handleIdempotencyKeyReuse(
        IdempotencyKeyReuseException exception
    ) {
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockingFailure(
        ObjectOptimisticLockingFailureException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError("Transaction was modified by another request"));
    }
}
 