package com.example.mstemplateredis.exception;

import com.example.mstemplateredis.config.CustomerContextHolder;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle DatabaseOperationException (General for Insert, Update, Delete failures)
    @ExceptionHandler(DatabaseOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDataOperationException(DatabaseOperationException ex) {
        return buildErrorResponse("DATABASE_OPERATION_ERROR", ex.getMessage(), ex);
    }

    // Handle Account Retrieval Failures
    @ExceptionHandler(AccountRetrievalException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleAccountRetrievalException(AccountRetrievalException ex) {
        return buildErrorResponse("ACCOUNT_RETRIEVAL_ERROR", "Account retrieval failed", ex);
    }

    // Handle Account Creation Failures
    @ExceptionHandler(AccountCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleAccountCreationException(AccountCreationException ex) {
        return buildErrorResponse("ACCOUNT_CREATION_ERROR", "Account creation failed", ex);
    }

    // Handle Account Update Failures
    @ExceptionHandler(AccountUpdateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleAccountUpdateException(AccountUpdateException ex) {
        return buildErrorResponse("ACCOUNT_UPDATE_ERROR", "Account update failed", ex);
    }

    // Handle Account Deletion Failures
    @ExceptionHandler(AccountDeletionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleAccountDeletionException(AccountDeletionException ex) {
        return buildErrorResponse("ACCOUNT_DELETION_ERROR", "Account deletion failed", ex);
    }

    // Handle Generic Exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse("GENERIC_ERROR", "An unexpected error occurred", ex);
    }

    // Handle validation failures (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Validation failed: {}", errors);
        ErrorResponse response = new ErrorResponse("INVALID_REQUEST_DATA", String.join("; ", errors), Instant.now());
        return ResponseEntity.badRequest().body(response);
    }

    // Handle constraint violations (ConstraintViolationException)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        log.warn("Constraint violation: {}", errors);
        ErrorResponse response = new ErrorResponse("INVALID_INPUT", String.join("; ", errors), Instant.now());
        return ResponseEntity.badRequest().body(response);
    }

    // Helper method to DRY up error handling
    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, Exception ex) {
        // Fetching the customer ID from CustomerContextHolder
        String customerId = CustomerContextHolder.getCustomerId();
        String resolvedCustomerId = customerId != null ? customerId : "unknown";

        // Log the exception details
        log.error("Error [{}] for customer ID [{}]: {} | Exception: {}", code, resolvedCustomerId, message, ex.toString(), ex);

        // Create and return the error response
        ErrorResponse error = new ErrorResponse(
                code,
                String.format("%s for customer ID: %s", message, resolvedCustomerId),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}