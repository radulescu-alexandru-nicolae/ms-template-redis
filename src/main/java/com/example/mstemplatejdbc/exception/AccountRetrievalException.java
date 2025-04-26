package com.example.mstemplatejdbc.exception;

public class AccountRetrievalException extends RuntimeException {
    public AccountRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}