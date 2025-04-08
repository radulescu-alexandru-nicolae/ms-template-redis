package com.example.mstemplateredis.exception;

public class AccountDeletionException extends RuntimeException {
    public AccountDeletionException(String message) {
        super(message);
    }

    public AccountDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}