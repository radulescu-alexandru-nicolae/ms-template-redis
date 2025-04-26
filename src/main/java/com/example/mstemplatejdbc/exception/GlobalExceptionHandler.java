package com.example.mstemplatejdbc.exception;

import com.example.mstemplatejdbc.config.CustomerContextHolder;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountCreationException.class)
    public ProblemDetail handleAccountCreationException(AccountCreationException ex) {
        return buildProblemDetail(
                "ACCOUNT_CREATION_ERROR",
                "Account creation failed",
                ex,
                HttpStatus.BAD_REQUEST,
                Map.of("operation", "createAccount")
        );
    }

    @ExceptionHandler(AccountUpdateException.class)
    public ProblemDetail handleAccountUpdateException(AccountUpdateException ex) {
        return buildProblemDetail(
                "ACCOUNT_UPDATE_ERROR",
                "Account update failed",
                ex,
                HttpStatus.BAD_REQUEST,
                Map.of("operation", "updateAccount")
        );
    }

    @ExceptionHandler(AccountDeletionException.class)
    public ProblemDetail handleAccountDeletionException(AccountDeletionException ex) {
        return buildProblemDetail(
                "ACCOUNT_DELETION_ERROR",
                "Account deletion failed",
                ex,
                HttpStatus.BAD_REQUEST,
                Map.of("operation", "deleteAccount")
        );
    }

    @ExceptionHandler(AccountRetrievalException.class)
    public ProblemDetail handleAccountRetrievalException(AccountRetrievalException ex) {
        return buildProblemDetail(
                "ACCOUNT_RETRIEVAL_ERROR",
                "Account retrieval failed",
                ex,
                HttpStatus.NOT_FOUND,
                Map.of("operation", "getAccountsByCustomerId")
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleDatabaseOperationException(AccountNotFoundException ex) {
        return buildProblemDetail(
                "DATABASE_OPERATION_ERROR",
                "Database operation failed",
                ex,
                HttpStatus.NOT_FOUND,
                Map.of("operation", "databaseAction")
        );
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(MissingServletRequestParameterException ex) {
        String paramName = ex.getParameterName();
        String message = "Missing required parameter: " + paramName;

        log.warn(message);

        Map<String, Object> props = new HashMap<>();
        props.put("missingParameter", paramName);
        props.put("operation", "inputValidation");

        return buildProblemDetail(
                "MISSING_REQUIRED_PARAMETER",
                message,
                ex,
                HttpStatus.BAD_REQUEST,
                props
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<String> violations = ex.getAllErrors()
                .stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();

        log.warn("Handler method validation failed: {}", violations);

        Map<String, Object> props = new HashMap<>();
        props.put("validationErrors", violations);
        props.put("operation", "methodLevelValidation");

        return buildProblemDetail(
                "METHOD_VALIDATION_FAILED",
                "Validation failed for request input",
                ex,
                HttpStatus.BAD_REQUEST,
                props
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        log.warn("Validation failed: {}", errors);

        Map<String, Object> props = new HashMap<>();
        props.put("validationErrors", errors);
        props.put("operation", "inputValidation");

        return buildProblemDetail(
                "INVALID_REQUEST_DATA",
                "Validation failed for request input",
                ex,
                HttpStatus.BAD_REQUEST,
                props
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<String> violations = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        log.warn("Constraint violation: {}", violations);

        Map<String, Object> props = new HashMap<>();
        props.put("validationErrors", violations);
        props.put("operation", "inputValidation");

        return buildProblemDetail(
                "INVALID_INPUT",
                "Constraint violation occurred",
                ex,
                HttpStatus.BAD_REQUEST,
                props
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());

        return buildProblemDetail(
                "RESOURCE_NOT_FOUND",
                "Invalid or incomplete request path",
                ex,
                HttpStatus.BAD_REQUEST, // or 404 if appropriate
                Map.of("operation", "pathResolution")
        );
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ProblemDetail handleMissingPathVariable(MissingPathVariableException ex) {
        log.error("Missing path variable: {}", ex.getVariableName());

        return buildProblemDetail(
                "MISSING_PATH_VARIABLE",
                "Required path variable is missing: " + ex.getVariableName(),
                ex,
                HttpStatus.NOT_FOUND,
                Map.of("operation", "missingPathVariable")
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        return buildProblemDetail(
                "GENERIC_ERROR",
                "An unexpected error occurred",
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                Map.of("operation", "unknown")
        );
    }

    private ProblemDetail buildProblemDetail(String code, String message, Exception ex, HttpStatus status, Map<String, Object> additionalProps) {
        String customerId = CustomerContextHolder.getCustomerId();
        String resolvedCustomerId = (customerId != null) ? customerId : "unknown";

        log.error("Error [{}] for customer [{}]: {} | Exception: {}", code, resolvedCustomerId, message, ex.toString(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message + " for customer ID: " + resolvedCustomerId);
        problem.setTitle(code);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("customerId", resolvedCustomerId);

        if (additionalProps != null) {
            additionalProps.forEach(problem::setProperty);
        }

        return problem;
    }
}
