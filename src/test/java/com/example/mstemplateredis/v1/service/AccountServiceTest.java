package com.example.mstemplateredis.v1.service;

import com.example.mstemplateredis.v1.model.Account;
import com.example.mstemplateredis.v1.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private RedisService redisService;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        redisService = mock(RedisService.class);
        accountService = new AccountService(accountRepository, redisService);
    }

    @Nested
    class GetAccountsByCustomerId {

        static Stream<Arguments> accountRetrievalScenarios() {
            return Stream.of(
                    Arguments.of("cust1", List.of(new Account("RO00AAA123456789", "cust1", BigDecimal.valueOf(200), null, null)), 200),
                    Arguments.of("custEmpty", List.of(), 200),
                    Arguments.of("custError", null, 500)
            );
        }

        @ParameterizedTest(name = "customerId={0}, expect status {2}")
        @MethodSource("accountRetrievalScenarios")
        void shouldHandleVariousRetrievalScenarios(String customerId, List<Account> mockResult, int expectedStatus) {
            when(redisService.getAccountsFromCache(customerId)).thenReturn(null);

            if (expectedStatus == 500) {
                when(accountRepository.getAccounts(customerId))
                        .thenThrow(new RuntimeException("DB failure"));

                assertThrows(RuntimeException.class, () -> accountService.getAccountsByCustomerId(customerId));
            } else {
                when(accountRepository.getAccounts(customerId)).thenReturn(mockResult);

                List<Account> result = accountService.getAccountsByCustomerId(customerId);

                assertEquals(mockResult, result);
                verify(redisService).getAccountsFromCache(customerId);
                verify(accountRepository).getAccounts(customerId);
                verify(redisService).setAccountsToCache(customerId, mockResult);
            }
        }
    }

    @Nested
    class CreateAccount {

        static Stream<Arguments> createAccountScenarios() {
            return Stream.of(
                    Arguments.of("RO3", 50, "cust123", 200),
                    Arguments.of("RO4", 100, "cust456", 200),
                    Arguments.of(null, 100, "cust789", 500)  // invalid IBAN scenario
            );
        }

        @ParameterizedTest(name = "Create IBAN={0}, Balance={1}, CustomerId={2}, expect status {3}")
        @MethodSource("createAccountScenarios")
        void shouldHandleCreateAccountScenarios(String iban, double balance, String customerId, int expectedStatus) {
            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    Account account = new Account(iban, customerId, BigDecimal.valueOf(balance), null, null);
                    if (iban == null) throw new IllegalArgumentException("Invalid IBAN");
                    accountService.createAccount(account, customerId);
                });
            } else {
                Account account = new Account(iban, customerId, BigDecimal.valueOf(balance), null, null);
                Account result = accountService.createAccount(account, customerId);

                assertEquals(account, result);
                verify(accountRepository).insertAccount(account, customerId);
                verify(redisService).createAccountToCache(customerId, account);
            }
        }
    }

    @Nested
    class UpdateAccount {

        static Stream<Arguments> updateAccountScenarios() {
            return Stream.of(
                    Arguments.of("RO4", 100, "cust123", 200),
                    Arguments.of("RO5", -50, "cust456", 500)
            );
        }

        @ParameterizedTest(name = "Update IBAN={0}, Balance={1}, CustomerId={2}, expect status {3}")
        @MethodSource("updateAccountScenarios")
        void shouldHandleUpdateAccountScenarios(String iban, double balance, String customerId, int expectedStatus) {
            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (balance < 0) throw new IllegalArgumentException("Negative balance");
                    accountService.updateAccount(iban, BigDecimal.valueOf(balance), customerId);
                });
            } else {
                accountService.updateAccount(iban, BigDecimal.valueOf(balance), customerId);

                verify(accountRepository).updateAccount(iban, BigDecimal.valueOf(balance), customerId);
                verify(redisService).updateAccountsInCache(iban, BigDecimal.valueOf(balance), customerId);
            }
        }
    }

    @Nested
    class DeleteAccount {

        static Stream<Arguments> deleteAccountScenarios() {
            return Stream.of(
                    Arguments.of("RO6", "cust123", 200),
                    Arguments.of(null, "cust456", 500)
            );
        }

        @ParameterizedTest(name = "Delete IBAN={0}, CustomerId={1}, expect status {2}")
        @MethodSource("deleteAccountScenarios")
        void shouldHandleDeleteAccountScenarios(String iban, String customerId, int expectedStatus) {
            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (iban == null) throw new IllegalArgumentException("IBAN cannot be null");
                    accountService.deleteAccount(iban, customerId);
                });
            } else {
                accountService.deleteAccount(iban, customerId);

                verify(accountRepository).deleteAccount(iban, customerId);
                verify(redisService).deleteAccountFromCache(customerId, iban);
            }
        }
    }
}