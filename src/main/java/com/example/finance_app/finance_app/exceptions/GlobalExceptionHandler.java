package com.example.finance_app.finance_app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> handleBadRequest(BadRequestException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> handleTokenRefresh(TokenRefreshException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse("Access denied due to " + ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse("Internal server error : Error Message is " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ✅ Common response builder (clean code)
    private ResponseEntity<com.example.finance_app.finance_app.models.dto.ErrorResponse> buildResponse(String message, HttpStatus status) {
        com.example.finance_app.finance_app.models.dto.ErrorResponse error = com.example.finance_app.finance_app.models.dto.ErrorResponse.builder()
                .message(message)
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, status);
    }
}