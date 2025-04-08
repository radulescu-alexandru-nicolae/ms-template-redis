package com.example.mstemplateredis.exception;

public class AccountRetrievalException extends RuntimeException {
    public AccountRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}