package com.example.mstemplateredis.v1.repository;

import com.example.mstemplateredis.exception.*;
import com.example.mstemplateredis.utils.Constants;
import com.example.mstemplateredis.v1.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

import static com.example.mstemplateredis.utils.Constants.SqlConstants.*;

@Repository
@Slf4j
public class AccountRepository {

    private final JdbcClient jdbcClient;

    public AccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Account> getAccounts(String customerId) {
        log.debug("************* AccountRepository.getAccounts for customer ID: {}", customerId);
        try {
            List<Account> accounts = jdbcClient.sql(retriveAccountsSql)
                    .param(Constants.customerId, customerId)
                    .query(Account.class)
                    .list();
            log.info("Successfully retrieved {} accounts for customer ID: {}", accounts.size(), customerId);
            return accounts;
        } catch (DataAccessException ex) {
            log.error("Database error retrieving accounts for customer ID {}", customerId, ex);
            throw new AccountRetrievalException("Failed to retrieve accounts", ex);
        }
    }

    public void insertAccount(Account account, String customerId) {
        log.debug("************* AccountRepository.insertAccount for customer ID: {}", customerId);
        try {
            int rows = jdbcClient.sql(insertSql)
                    .param(Constants.iban, account.getIban())
                    .param(Constants.customerId, account.getCustomerId())
                    .param(Constants.balance, account.getBalance())
                    .update();

            validateRowsAffected(rows, "insert", account.getIban());

            log.info("Account inserted successfully for IBAN: {}", account.getIban());
        } catch (DataAccessException ex) {
            log.error("Insert error for IBAN: {}", account.getIban(), ex);
            throw new AccountCreationException("Database insert failed for IBAN: " + account.getIban(), ex);
        }
    }

    public void updateAccount(String iban, BigDecimal balance, String customerId) {
        log.debug("************* AccountRepository.updateAccount for customer ID: {}, IBAN: {}", customerId, iban);

        try {
            int rows = jdbcClient.sql(updateSql)
                    .param(Constants.iban, iban)
                    .param(Constants.customerId, customerId)
                    .param(Constants.balance, balance)
                    .update();

            validateRowsAffected(rows, "update", iban);

            log.info("Account updated successfully for IBAN: {}", iban);
        } catch (DataAccessException ex) {
            log.error("Update error for IBAN: {}", iban, ex);
            throw new AccountUpdateException("Database update failed for IBAN: " + iban, ex);
        }
    }

    public void deleteAccount(String iban, String customerId) {
        log.debug("************* AccountRepository.deleteAccount for customer ID: {}, IBAN: {}", customerId, iban);

        try {
            int rows = jdbcClient.sql(deleteSql)
                    .param(Constants.iban, iban)
                    .param(Constants.customerId, customerId)
                    .update();

            validateRowsAffected(rows, "delete", iban);

            log.info("Account deleted successfully for IBAN: {}", iban);
        } catch (DataAccessException ex) {
            log.error("Delete error for IBAN: {}", iban, ex);
            throw new AccountDeletionException("Database deletion failed for IBAN: " + iban, ex);
        }
    }


    private void validateRowsAffected(int rows, String operation, String iban) {
        if (rows < 1) {
            String message = String.format("Failed to %s account for IBAN: %s", operation, iban);
            log.error(message);
            throw new AccountNotFoundException(message);
        }
    }
}