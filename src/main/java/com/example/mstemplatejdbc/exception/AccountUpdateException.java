package com.example.mstemplatejdbc.exception;

public class AccountUpdateException extends RuntimeException {
    public AccountUpdateException(String message) {
        super(message);
    }

    public AccountUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}