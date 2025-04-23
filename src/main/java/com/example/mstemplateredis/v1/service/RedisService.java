package com.example.mstemplateredis.v1.service;

import com.example.mstemplateredis.utils.Constants;
import com.example.mstemplateredis.v1.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${spring.data.redis.time-to-live}")
    String ttl;


    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<Account> getAccountsFromCache(String customerId) {
        String cacheKey = Constants.cacheAcccountKey + customerId;

        try {
            @SuppressWarnings("unchecked")
            List<Account> cachedAccounts = (List<Account>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedAccounts != null) {
                log.info("****************** Retrieved {} accounts from cache for customer: {}", cachedAccounts.size(), customerId);
                return cachedAccounts;
            } else {
                log.info("****************** Cache miss for customer: {}", customerId);
            }
        } catch (Exception e) {
            logRedisError("getAccountsByCustomerId - read", e);
        }
        return null; // Cache miss
    }

    public void setAccountsToCache(String customerId, List<Account> accounts) {
        String cacheKey = Constants.cacheAcccountKey + customerId;
        // Try writing to cache
        try {
            redisTemplate.opsForValue().set(cacheKey, accounts,parseTtl(ttl));
            log.info("****************** Cached accounts for customer: {}", customerId);
        } catch (Exception e) {
            logRedisError("getAccountsByCustomerId - write", e);
        }

    }

    public void createAccountToCache(String customerId, Account account) {
        String cacheKey = Constants.cacheAcccountKey + customerId;

        try {
            @SuppressWarnings("unchecked")
            List<Account> accounts = (List<Account>) redisTemplate.opsForValue().get(cacheKey);

            if (accounts != null) {
                accounts.add(account);
                redisTemplate.opsForValue().set(cacheKey, accounts,parseTtl(ttl));
                log.info("Appended new account to cache for customer: {}", customerId);
            } else {
                log.info("Cache miss while appending account. No cache exists yet for customer: {}", customerId);
            }
        } catch (Exception e) {
            logRedisError("createAccount", e);
        }
    }

    public void updateAccountsInCache(String iban, BigDecimal balance, String customerId) {
        String cacheKey = Constants.cacheAcccountKey + customerId;

        try {
            @SuppressWarnings("unchecked")
            List<Account> accounts = (List<Account>) redisTemplate.opsForValue().get(cacheKey);

            if (accounts != null) {
                boolean updated = false;
                for (Account acc : accounts) {
                    if (acc.getIban().equals(iban)) {
                        acc.setBalance(balance);
                        updated = true;
                        break;
                    }
                }

                if (updated) {
                    redisTemplate.opsForValue().set(cacheKey, accounts,parseTtl(ttl));
                    log.info("Updated account in cache for IBAN: {}", iban);
                } else {
                    log.warn("Account with IBAN {} not found in cache for customer {}", iban, customerId);
                }
            } else {
                log.info("Cache miss while updating cache for IBAN: {}", iban);
            }
        } catch (Exception e) {
            logRedisError("updateAccount", e);
        }
    }

    public void deleteAccountFromCache(String customerId, String iban) {
        String cacheKey = Constants.cacheAcccountKey + customerId;

        try {
            @SuppressWarnings("unchecked")
            List<Account> accounts = (List<Account>) redisTemplate.opsForValue().get(cacheKey);

            if (accounts != null) {
                boolean removed = accounts.removeIf(acc -> acc.getIban().equals(iban));

                if (removed) {
                    redisTemplate.opsForValue().set(cacheKey, accounts,parseTtl(ttl));
                    log.info("Removed account from cache for IBAN: {}", iban);
                } else {
                    log.warn("Account with IBAN {} not found in cache for customer {}", iban, customerId);
                }
            } else {
                log.info("Cache miss while deleting account from cache for IBAN: {}", iban);
            }
        } catch (Exception e) {
            logRedisError("deleteAccount", e);
        }
    }

    Duration parseTtl(String ttl) {
        if (ttl != null && ttl.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(ttl.replace("m", "")));
        } else if (ttl != null && ttl.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(ttl.replace("s", "")));
        }
        return Duration.ZERO; // Default TTL if no valid configuration
    }

    private void logRedisError(String action, Exception e) {
        log.error("Redis error during {}: {}", action, e.getMessage());
    }
}