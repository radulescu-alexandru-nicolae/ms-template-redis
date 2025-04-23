package com.example.mstemplateredis.v1.api;

import com.example.mstemplateredis.config.CustomerContextHolder;
import com.example.mstemplateredis.v1.model.Account;
import com.example.mstemplateredis.v1.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/brd-api/ms-template-redis/v1/accounts")
@Slf4j
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Get accounts by customer ID",
            description = "Fetches all accounts associated with a specific customer ID. May return a 500 error if an unexpected internal error occurs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the accounts",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class))),
                    @ApiResponse(responseCode = "404", description = "Customer not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/{customerId}")
    public ResponseEntity<List<Account>> getAccountsByCustomerId(
            @PathVariable("customerId") @NotBlank String customerId) {
        log.debug("Received request to fetch accounts for customer ID: {}", customerId);
        CustomerContextHolder.setCustomerId(customerId);
        List<Account> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    @Operation(
            summary = "Create a new account",
            description = "Creates a new account for the given customer ID. May return a 500 error if an unexpected internal error occurs.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Account successfully created",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request or invalid account data",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping("/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody @Valid Account account,
                                 @PathVariable("customerId") @NotBlank String customerId) {
        log.debug("Received request to insert account for customer ID: {}", customerId);
        CustomerContextHolder.setCustomerId(customerId);
        return accountService.createAccount(account, customerId);
    }

    @Operation(
            summary = "Update an existing account",
            description = "Updates the balance of an existing account using IBAN and new balance. May return a 500 error if an unexpected internal error occurs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Account not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PutMapping("/{customerId}/update")
    @Validated
    public ResponseEntity<String> updateAccount(
            @PathVariable("customerId") @NotBlank String customerId,
            @RequestParam(value = "iban", required = true) @NotBlank String iban,
            @RequestParam(value = "balance", required = true)
            @DecimalMin(value = "0.0", inclusive = false, message = "Balance must be greater than 0.0")
            @NotNull
            BigDecimal balance
    ) {
        log.debug("Received request to update account for customer ID: {}, IBAN: {}", customerId, iban);
        CustomerContextHolder.setCustomerId(customerId);
        accountService.updateAccount(iban, balance, customerId);

        log.info("Account with IBAN: {} updated successfully for customer ID: {}", iban, customerId);
        return ResponseEntity.ok("Account updated successfully.");
    }

    @Operation(
            summary = "Delete an account",
            description = "Deletes an account for the given IBAN and customer ID. May return a 500 error if an unexpected internal error occurs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Account not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @DeleteMapping("/{customerId}/delete/{iban}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable("customerId") @NotBlank String customerId,
            @PathVariable("iban") @NotBlank String iban) {
        log.debug("Received request to delete account for customer ID: {}, IBAN: {}", customerId, iban);
        CustomerContextHolder.setCustomerId(customerId);
        accountService.deleteAccount(iban, customerId);

        log.info("Account with IBAN: {} deleted successfully for customer ID: {}", iban, customerId);
        return ResponseEntity.ok("Account deleted successfully.");
    }
}