package com.example.finance_app.finance_app.exceptions;

import java.time.LocalDateTime;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

record ErrorResponse(String message, LocalDateTime timestamp) {}