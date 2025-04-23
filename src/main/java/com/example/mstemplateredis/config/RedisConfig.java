package com.example.mstemplateredis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.time-to-live}")
    private String ttl;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Duration ttlDuration = parseTtl(ttl);

        // Configure RedisCacheWriter, which handles the actual writing of cache data
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);

        // Configure the RedisCacheConfiguration
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .entryTtl(ttlDuration);  // Apply the TTL (Time To Live) to the cache entries

        // Create the RedisCacheManager with the defined cacheWriter and cacheConfig
        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        // Return a TransactionAwareCacheManagerProxy to ensure the cache operations are transaction-aware.
        // This is primarily used when annotations such as @Cacheable, @CacheEvict, and other Spring Caching annotations
        // are involved. These annotations manage cache within the context of a transaction, and
        // the TransactionAwareCacheManagerProxy ensures cache operations are consistent and aligned with
        // the transaction boundaries (i.e., cache updates will be committed only if the transaction is committed).
        //
        // Note that this proxy is used for managing cache operations in transactional contexts.
        // When using RedisTemplate directly, no such transactional support is required.
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);

        return redisTemplate;
    }

    // Helper method to parse TTL string value (e.g., "5m" -> Duration.ofMinutes(5))
    private Duration parseTtl(String ttl) {
        if (ttl != null && ttl.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(ttl.replace("m", "")));
        } else if (ttl != null && ttl.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(ttl.replace("s", "")));
        }
        return Duration.ZERO; // Default TTL if no valid configuration
    }
}