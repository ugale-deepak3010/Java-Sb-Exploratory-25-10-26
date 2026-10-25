package com.dbs.banking.transfer.service;

import com.dbs.banking.common.exception.DuplicateTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * CRITICAL FOR 7+ YOE INTERVIEW:
 * Redis SETNX Token Bucket Idempotency Engine.
 * Guarantees that duplicate HTTP requests (e.g. mobile app retry / network timeout)
 * are rejected at the edge before hitting core banking logic.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String IDEMPOTENCY_PREFIX = "idemp:";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically acquires an idempotency lock for 120 seconds.
     * Throws DuplicateTransactionException if key already exists.
     */
    public void acquireLock(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", Duration.ofSeconds(120));
        if (Boolean.FALSE.equals(success)) {
            log.warn("Idempotency key collision detected for key: {}", idempotencyKey);
            throw new DuplicateTransactionException(idempotencyKey);
        }
    }

    public void markCompleted(String idempotencyKey, String transferId) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, "COMPLETED:" + transferId, Duration.ofHours(24));
    }

    public void releaseLock(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
    }
}
