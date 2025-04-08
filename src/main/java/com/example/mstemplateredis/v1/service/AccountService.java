package com.example.mstemplateredis.v1.service;

import com.example.mstemplateredis.v1.model.Account;
import com.example.mstemplateredis.v1.repository.AccountRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Fetches all accounts for the specified customerId from cache or database.
     * Caches the result under the customerId.
     */
    @Cacheable(value = "accounts", key = "#customerId")
    public List<Account> getAccountsByCustomerId(@NotBlank String customerId) {
        log.debug("****************** Fetching accounts for customer: {}", customerId);
        List<Account> accounts = accountRepository.getAccounts(customerId);
        log.info("****************** Retrieved {} accounts for customer: {}", accounts.size(), customerId);
        return accounts;
    }

    /**
     * Creates a new account, stores it in the database, and evicts the cache.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    @CacheEvict(value = "accounts", key = "#customerId")
    public Account createAccount(Account account, String customerId) {
        log.debug("****************** Insert account for customer: {}", customerId);
        accountRepository.insertAccount(account, customerId);
        log.info("Created new account with IBAN: {}", account.getIban());
        return account;
    }

    /**
     * Updates an existing account balance, stores the change in the database, and evicts the cache.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    @CacheEvict(value = "accounts", key = "#customerId")
    public void updateAccount(String iban, BigDecimal balance, String customerId) {
        log.debug("****************** Updating account for IBAN: {}", iban);
        accountRepository.updateAccount(iban, balance, customerId);
        log.info("Updated account with IBAN: {} successfully", iban);
    }

    /**
     * Deletes an account, removes it from the database, and evicts the cache for the customerId.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    @CacheEvict(value = "accounts", key = "#customerId")
    public void deleteAccount(String iban, String customerId) {
        log.debug("****************** Deleting account for IBAN: {}", iban);
        accountRepository.deleteAccount(iban, customerId);
        log.info("Deleted account with IBAN: {} successfully", iban);
    }
}