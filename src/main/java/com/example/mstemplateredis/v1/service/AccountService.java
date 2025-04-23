package com.example.mstemplateredis.v1.service;

import com.example.mstemplateredis.v1.model.Account;
import com.example.mstemplateredis.v1.repository.AccountRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final RedisService redisService;

    public AccountService(AccountRepository accountRepository, RedisService redisService) {
        this.accountRepository = accountRepository;
        this.redisService = redisService;
    }

    /**
     * Fetches all accounts for the specified customerId from cache or database.
     * Caches the result under the customerId.
     */
    public List<Account> getAccountsByCustomerId(@NotBlank String customerId) {
        log.debug("****************** Fetching accounts for customer: {}", customerId);
        // Try getting accounts from the cache
        List<Account> cachedAccounts = redisService.getAccountsFromCache(customerId);

        if (cachedAccounts != null) {
            log.info("****************** Retrieved {} accounts from cache for customer: {}", cachedAccounts.size(), customerId);
            return cachedAccounts;
        } else {
            log.info("****************** Cache miss for customer: {}", customerId);

            // Fallback to DB
            List<Account> accounts = accountRepository.getAccounts(customerId);
            log.info("****************** Retrieved {} accounts from DB for customer: {}", accounts.size(), customerId);

            redisService.setAccountsToCache(customerId, accounts);

            return accounts;
        }
    }

    /**
     * Creates a new account, stores it in the database, and evicts the cache.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    public Account createAccount(Account account, String customerId) {
        log.debug("****************** Insert account for customer: {}", customerId);
        accountRepository.insertAccount(account, customerId);

        redisService.createAccountToCache(customerId, account);

        log.info("Created new account with IBAN: {}", account.getIban());
        return account;
    }

    /**
     * Updates an existing account balance, stores the change in the database, and evicts the cache.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    public void updateAccount(String iban, BigDecimal balance, String customerId) {
        log.debug("****************** Updating account for IBAN: {}", iban);
        accountRepository.updateAccount(iban, balance, customerId);

        redisService.updateAccountsInCache(iban, balance, customerId);

        log.info("Updated account with IBAN: {} successfully", iban);
    }

    /**
     * Deletes an account, removes it from the database, and evicts the cache for the customerId.
     * After this, the next fetch will go to the database and repopulate the cache.
     */
    @Transactional
    public void deleteAccount(String iban, String customerId) {
        log.debug("****************** Deleting account for IBAN: {}", iban);
        accountRepository.deleteAccount(iban, customerId);

        redisService.deleteAccountFromCache(customerId, iban);

        log.info("Deleted account with IBAN: {} successfully", iban);
    }


}