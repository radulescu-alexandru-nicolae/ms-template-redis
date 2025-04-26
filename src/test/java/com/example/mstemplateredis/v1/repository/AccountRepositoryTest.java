package com.example.mstemplateredis.v1.repository;



import com.example.mstemplateredis.exception.*;
import com.example.mstemplateredis.v1.model.Account;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {AccountRepository.class, JdbcClient.class})
@ExtendWith(SpringExtension.class)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private JdbcClient jdbcClient;

    // --- getAccounts() ---
    @Nested
    class GetAccounts {

        @Test
        void shouldThrowAccountRetrievalExceptionWhenQueryFails() {
            var mappedQuerySpec = mock(JdbcClient.MappedQuerySpec.class);
            when(mappedQuerySpec.list()).thenThrow(new EmptyResultDataAccessException(1));

            var statementSpec = mock(JdbcClient.StatementSpec.class);
            when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
            when(statementSpec.query(Account.class)).thenReturn(mappedQuerySpec);
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

            assertThrows(AccountRetrievalException.class, () -> accountRepository.getAccounts("42"));

            verify(jdbcClient).sql(contains("FROM account_db"));
            verify(mappedQuerySpec).list();
        }

        @Test
        void shouldReturnEmptyListWhenNoResults() {
            var mappedQuerySpec = mock(JdbcClient.MappedQuerySpec.class);
            when(mappedQuerySpec.list()).thenReturn(new ArrayList<>());

            var statementSpec = mock(JdbcClient.StatementSpec.class);
            when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
            when(statementSpec.query(Account.class)).thenReturn(mappedQuerySpec);
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

            List<Account> result = accountRepository.getAccounts("42");

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(jdbcClient).sql(contains("FROM account_db"));
            verify(mappedQuerySpec).list();
        }
    }

    // --- insertAccount() ---
    @Nested
    class InsertAccount {

        static Stream<Arguments> insertAccountResponses() {
            return Stream.of(
                    Arguments.of(1, null), // success
                    Arguments.of(0, AccountNotFoundException.class), // no rows updated
                    Arguments.of(-1, AccountCreationException.class) // EmptyResultDataAccessException
            );
        }

        @ParameterizedTest(name = "insertAccount returns {0} -> expect exception {1}")
        @MethodSource("insertAccountResponses")
        void shouldHandleInsertAccountScenarios(int updateResult, Class<? extends Exception> expectedException) {
            var statementSpec = mock(JdbcClient.StatementSpec.class);
            when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

            if (updateResult == -1) {
                when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
            } else {
                when(statementSpec.update()).thenReturn(updateResult);
            }

            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

            if (expectedException == null) {
                assertDoesNotThrow(() -> accountRepository.insertAccount(new Account(), "42"));
            } else {
                assertThrows(expectedException, () -> accountRepository.insertAccount(new Account(), "42"));
            }

            verify(jdbcClient).sql(contains("INSERT INTO account_db"));
            verify(statementSpec).update();
        }
    }

    // --- updateAccount() ---
    @Nested
    class UpdateAccount {

        static Stream<Arguments> updateAccountResponses() {
            return Stream.of(
                    Arguments.of(1, null), // success
                    Arguments.of(0, AccountNotFoundException.class), // no rows updated
                    Arguments.of("retrieval", AccountRetrievalException.class),
                    Arguments.of("empty", AccountUpdateException.class)
            );
        }

        @ParameterizedTest(name = "updateAccount result {0} -> expect exception {1}")
        @MethodSource("updateAccountResponses")
        void shouldHandleUpdateAccountScenarios(Object result, Class<? extends Exception> expectedException) {
            var statementSpec = mock(JdbcClient.StatementSpec.class);
            when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

            if (result instanceof Integer) {
                when(statementSpec.update()).thenReturn((Integer) result);
            } else if ("retrieval".equals(result)) {
                when(statementSpec.update()).thenThrow(new AccountRetrievalException("fail", new RuntimeException()));
            } else if ("empty".equals(result)) {
                when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
            }

            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

            if (expectedException == null) {
                assertDoesNotThrow(() -> accountRepository.updateAccount("IBAN123", BigDecimal.TEN, "42"));
            } else {
                assertThrows(expectedException, () -> accountRepository.updateAccount("IBAN123", BigDecimal.TEN, "42"));
            }

            verify(jdbcClient).sql(contains("UPDATE account_db SET balance"));
            verify(statementSpec, times(3)).param(anyString(), any());
            verify(statementSpec).update();
        }
    }

    // --- deleteAccount() ---
    @Nested
    class DeleteAccount {

        static Stream<Arguments> deleteAccountResponses() {
            return Stream.of(
                    Arguments.of(1, null), // success
                    Arguments.of(0, AccountNotFoundException.class),
                    Arguments.of("retrieval", AccountRetrievalException.class),
                    Arguments.of("empty", AccountDeletionException.class)
            );
        }

        @ParameterizedTest(name = "deleteAccount result {0} -> expect exception {1}")
        @MethodSource("deleteAccountResponses")
        void shouldHandleDeleteAccountScenarios(Object result, Class<? extends Exception> expectedException) {
            var statementSpec = mock(JdbcClient.StatementSpec.class);
            when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

            if (result instanceof Integer) {
                when(statementSpec.update()).thenReturn((Integer) result);
            } else if ("retrieval".equals(result)) {
                when(statementSpec.update()).thenThrow(new AccountRetrievalException("fail", new RuntimeException()));
            } else if ("empty".equals(result)) {
                when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
            }

            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

            if (expectedException == null) {
                assertDoesNotThrow(() -> accountRepository.deleteAccount("IBAN123", "42"));
            } else {
                assertThrows(expectedException, () -> accountRepository.deleteAccount("IBAN123", "42"));
            }

            verify(jdbcClient).sql(contains("DELETE FROM account_db"));
            verify(statementSpec, times(2)).param(anyString(), any());
            verify(statementSpec).update();
        }
    }
}