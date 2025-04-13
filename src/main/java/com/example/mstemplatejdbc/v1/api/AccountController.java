package com.example.mstemplatejdbc.v1.api;

import com.example.mstemplatejdbc.v1.model.Account;
import com.example.mstemplatejdbc.v1.service.AccountService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/brd-api/ms-template-jdbc/v1/accounts")
@Slf4j
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{customerId}")
    @ApiOperation(value = "Get accounts by customer ID", notes = "Fetches all accounts associated with a specific customer ID")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successfully retrieved the accounts"),
            @ApiResponse(code = 404, message = "Customer not found")
    })
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Account>> getAccountsByCustomerId(
            @PathVariable("customerId") @NotBlank String customerId) {
        log.debug("Received request to fetch accounts for customer ID: {}", customerId);

        List<Account> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a new account", notes = "Creates a new account for the given customer ID")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Account successfully created"),
            @ApiResponse(code = 400, message = "Bad request or invalid account data")
    })
    public Account createAccount(@RequestBody Account account,
                                 @PathVariable("customerId") @NotBlank String customerId) {
        log.debug("Received request to insert account for customer ID: {}", customerId);
        return accountService.createAccount(account, customerId);
    }

    @PutMapping("/{customerId}/update")
    @ApiOperation(value = "Update an existing account", notes = "Updates the balance of an existing account using IBAN and new balance")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Account successfully updated"),
            @ApiResponse(code = 400, message = "Invalid input or account not found"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateAccount(
            @PathVariable("customerId") @NotBlank String customerId,
            @RequestParam("iban") @NotBlank String iban,
            @RequestParam("balance") BigDecimal balance) {
        log.debug("Received request to update account for customer ID: {}, IBAN: {}", customerId, iban);

        accountService.updateAccount(iban, balance, customerId);

        log.info("Account with IBAN: {} updated successfully for customer ID: {}", iban, customerId);
        return ResponseEntity.ok("Account updated successfully.");
    }

    @DeleteMapping("/{customerId}/delete/{iban}")
    @ApiOperation(value = "Delete an account", notes = "Deletes an account for the given IBAN and customer ID")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Account successfully deleted"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteAccount(
            @PathVariable("customerId") @NotBlank String customerId,
            @PathVariable("iban") @NotBlank String iban) {
        log.debug("Received request to delete account for customer ID: {}, IBAN: {}", customerId, iban);

        accountService.deleteAccount(iban, customerId);

        log.info("Account with IBAN: {} deleted successfully for customer ID: {}", iban, customerId);
        return ResponseEntity.ok("Account deleted successfully.");
    }
}