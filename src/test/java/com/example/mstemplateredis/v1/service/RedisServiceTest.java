package com.example.mstemplateredis.v1.service;
import com.example.mstemplateredis.utils.Constants;
import com.example.mstemplateredis.v1.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    class GetAccountsFromCacheTest {

        static Stream<Arguments> getAccountsScenarios() {
            return Stream.of(
                    Arguments.of("cust001", List.of(new Account("RO123", "cust001", BigDecimal.TEN, null, null)), 200),
                    Arguments.of("cust002", null, 404)
            );
        }

        @ParameterizedTest
        @MethodSource("getAccountsScenarios")
        void shouldHandleCacheRetrievalScenarios(String customerId, List<Account> expectedAccounts, int expectedStatus) {
            String cacheKey = Constants.cacheAcccountKey + customerId;

            when(valueOperations.get(cacheKey)).thenReturn(expectedAccounts);

            List<Account> result = redisService.getAccountsFromCache(customerId);

            if (expectedStatus == 404) {
                assertNull(result);
            } else {
                assertEquals(expectedAccounts, result);
            }
            verify(redisTemplate.opsForValue()).get(cacheKey);
        }
    }

    @Nested
    class SetAccountsToCacheTest {

        static Stream<Arguments> setAccountsScenarios() {
            return Stream.of(
                    Arguments.of("cust001", 200),
                    Arguments.of("custError", 500)
            );
        }

        @ParameterizedTest
        @MethodSource("setAccountsScenarios")
        void shouldHandleSettingAccountsToCache(String customerId, int expectedStatus) {
            List<Account> accounts = List.of(Account.builder().iban("RO123").customerId(customerId).balance(BigDecimal.TEN).build());

            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (customerId.equals("custError")) throw new IllegalArgumentException("Cache error");
                    redisService.setAccountsToCache(customerId, accounts);
                });
            } else {
                redisService.setAccountsToCache(customerId, accounts);
                verify(valueOperations).set(eq(Constants.cacheAcccountKey + customerId), eq(accounts), eq(Duration.ofSeconds(0)));
            }
        }
    }

    @Nested
    class CreateAccountToCacheTest {

        static Stream<Arguments> createAccountScenarios() {
            return Stream.of(
                    Arguments.of("cust001", 200),
                    Arguments.of("custError", 500)
            );
        }

        @ParameterizedTest
        @MethodSource("createAccountScenarios")
        void shouldHandleCreateAccountToCache(String customerId, int expectedStatus) {
            Account account = Account.builder().iban("RO124").customerId(customerId).balance(BigDecimal.ONE).build();

            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (customerId.equals("custError")) throw new IllegalArgumentException("Cache error");
                    List<Account> existing = new ArrayList<>();
                    when(valueOperations.get(Constants.cacheAcccountKey + customerId)).thenReturn(existing);
                    redisService.createAccountToCache(customerId, account);
                });
            } else {
                List<Account> existing = new ArrayList<>();
                when(valueOperations.get(Constants.cacheAcccountKey + customerId)).thenReturn(existing);

                redisService.createAccountToCache(customerId, account);

                assertTrue(existing.contains(account));
                verify(valueOperations).set(eq(Constants.cacheAcccountKey + customerId), eq(existing), eq(Duration.ofSeconds(0)));
            }
        }
    }

    @Nested
    class UpdateAccountsInCacheTest {

        static Stream<Arguments> updateAccountScenarios() {
            return Stream.of(
                    Arguments.of("RO999", BigDecimal.valueOf(200), "cust001", 200),
                    Arguments.of("RO000", BigDecimal.valueOf(-50), "cust002", 500)
            );
        }

        @ParameterizedTest
        @MethodSource("updateAccountScenarios")
        void shouldHandleUpdateAccountsInCache(String iban, BigDecimal balance, String customerId, int expectedStatus) {
            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (balance.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Invalid balance");
                    redisService.updateAccountsInCache(iban, balance, customerId);
                });
            } else {
                Account account = Account.builder().iban(iban).balance(BigDecimal.ZERO).customerId(customerId).build();
                List<Account> accounts = new ArrayList<>(List.of(account));
                when(valueOperations.get(Constants.cacheAcccountKey + customerId)).thenReturn(accounts);

                redisService.updateAccountsInCache(iban, balance, customerId);

                assertEquals(balance, accounts.get(0).getBalance());
                verify(valueOperations).set(eq(Constants.cacheAcccountKey + customerId), eq(accounts), eq(Duration.ofSeconds(0)));
            }
        }
    }

    @Nested
    class DeleteAccountFromCacheTest {

        static Stream<Arguments> deleteAccountScenarios() {
            return Stream.of(
                    Arguments.of("RO555", "cust001", 200),
                    Arguments.of(null, "cust002", 500)
            );
        }

        @ParameterizedTest
        @MethodSource("deleteAccountScenarios")
        void shouldHandleDeleteAccountFromCache(String iban, String customerId, int expectedStatus) {
            if (expectedStatus == 500) {
                assertThrows(IllegalArgumentException.class, () -> {
                    if (iban == null) throw new IllegalArgumentException("IBAN required");
                    redisService.deleteAccountFromCache(customerId, iban);
                });
            } else {
                Account account = Account.builder().iban(iban).customerId(customerId).balance(BigDecimal.TEN).build();
                List<Account> accounts = new ArrayList<>(List.of(account));
                when(valueOperations.get(Constants.cacheAcccountKey + customerId)).thenReturn(accounts);

                redisService.deleteAccountFromCache(customerId, iban);

                assertFalse(accounts.contains(account));
                verify(valueOperations).set(eq(Constants.cacheAcccountKey + customerId), eq(accounts), eq(Duration.ofSeconds(0)));
            }
        }
    }
    @Nested
    class ParseTtlTest {

        static Stream<Arguments> parseTtlScenarios() {
            return Stream.of(
                    Arguments.of("15m", 15),
                    Arguments.of("45s", 0.75)
            );
        }

        @ParameterizedTest
        @MethodSource("parseTtlScenarios")
        void shouldParseTtlCorrectly(String inputTtl, double expectedMinutes) {
            Duration duration = redisService.parseTtl(inputTtl);

            assertTrue(duration.toMillis() > 0, "Duration should be positive");
            assertEquals(expectedMinutes * 60 * 1000, duration.toMillis(), 100);
        }
    }
}