package com.example.mstemplateredis.exception;

public class AccountCreationException extends RuntimeException {
    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
