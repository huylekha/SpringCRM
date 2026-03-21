package com.company.platform.shared.idempotency;

import com.company.platform.shared.cqrs.IdempotencyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-based implementation of IdempotencyStore with database fallback. Provides distributed
 * locking and response caching for idempotent operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisIdempotencyStore implements IdempotencyStore {

  private final StringRedisTemplate redisTemplate;
  private final IdempotencyDatabaseService databaseService;
  private final ObjectMapper objectMapper;

  private static final String LOCK_PREFIX = "idempotency:lock:";
  private static final String RESPONSE_PREFIX = "idempotency:response:";

  @Override
  public boolean tryLock(String idempotencyKey, long ttlSeconds) {
    String lockKey = LOCK_PREFIX + idempotencyKey;

    try {
      // Try Redis first
      Boolean lockAcquired =
          redisTemplate
              .opsForValue()
              .setIfAbsent(lockKey, "locked", Duration.ofSeconds(ttlSeconds));

      if (Boolean.TRUE.equals(lockAcquired)) {
        log.debug("Acquired Redis lock for idempotency key: {}", idempotencyKey);
        return true;
      }

      // Redis lock failed, check if we have a cached response
      String responseKey = RESPONSE_PREFIX + idempotencyKey;
      if (Boolean.TRUE.equals(redisTemplate.hasKey(responseKey))) {
        log.debug("Redis lock failed but response exists for key: {}", idempotencyKey);
        return false;
      }

    } catch (Exception e) {
      log.warn(
          "Redis operation failed for idempotency key: {}, falling back to database",
          idempotencyKey,
          e);
    }

    // Fallback to database
    return databaseService.tryDatabaseLock(idempotencyKey, ttlSeconds);
  }

  @Override
  public void storeResponse(String idempotencyKey, Object response, long ttlSeconds) {
    try {
      String responseJson = objectMapper.writeValueAsString(response);

      // Store in Redis
      storeResponseInRedis(idempotencyKey, responseJson, ttlSeconds);

      // Also store in database as fallback
      databaseService.storeResponseInDatabase(idempotencyKey, responseJson, ttlSeconds);

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize response for idempotency key: {}", idempotencyKey, e);
    }
  }

  private void storeResponseInRedis(String idempotencyKey, String responseJson, long ttlSeconds) {
    try {
      String responseKey = RESPONSE_PREFIX + idempotencyKey;
      redisTemplate.opsForValue().set(responseKey, responseJson, Duration.ofSeconds(ttlSeconds));
      log.debug("Stored response in Redis for idempotency key: {}", idempotencyKey);
    } catch (Exception e) {
      log.warn("Failed to store response in Redis for key: {}", idempotencyKey, e);
    }
  }

  @Override
  public <T> Optional<T> getResponse(String idempotencyKey, Class<T> responseType) {
    String responseKey = RESPONSE_PREFIX + idempotencyKey;

    // Try Redis first
    try {
      String responseJson = redisTemplate.opsForValue().get(responseKey);
      if (responseJson != null) {
        T response = objectMapper.readValue(responseJson, responseType);
        log.debug("Retrieved response from Redis for idempotency key: {}", idempotencyKey);
        return Optional.of(response);
      }
    } catch (Exception e) {
      log.warn(
          "Failed to retrieve response from Redis for key: {}, trying database", idempotencyKey, e);
    }

    // Fallback to database
    return databaseService.getDatabaseResponse(idempotencyKey, responseType);
  }

  @Override
  public void releaseLock(String idempotencyKey) {
    String lockKey = LOCK_PREFIX + idempotencyKey;

    try {
      redisTemplate.delete(lockKey);
      log.debug("Released Redis lock for idempotency key: {}", idempotencyKey);
    } catch (Exception e) {
      log.warn("Failed to release Redis lock for key: {}", idempotencyKey, e);
    }

    // Also clean up database record if it exists
    databaseService.releaseDatabaseLock(idempotencyKey);
  }
}
