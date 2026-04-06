package com.example.finance_app.finance_app.exceptions;

public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message) { super(message); }
}