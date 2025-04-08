package com.example.mstemplateredis.config;

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

    @Value("${spring.data.redis.time-to-live}") // Read TTL from application.yml
    private String ttl;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Duration ttlDuration = parseTtl(ttl);

        // Configure RedisCacheWriter
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .entryTtl(ttlDuration);  // Apply the TTL

        // Create RedisCacheManager
        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        // Return a TransactionAwareCacheManagerProxy to ensure the cache is transaction-aware
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // Set the key and value serializers
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class)); // Serialize complex objects like Account

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