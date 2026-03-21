package com.company.platform.crm.infrastructure.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyStoreTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @Mock private IdempotencyDatabaseService databaseService;

  private RedisIdempotencyStore idempotencyStore;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    idempotencyStore = new RedisIdempotencyStore(redisTemplate, databaseService, objectMapper);
  }

  @Test
  void shouldAcquireLockWhenRedisAvailable() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);

    // When
    boolean lockAcquired = idempotencyStore.tryLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(lockAcquired).isTrue();
    verify(valueOperations)
        .setIfAbsent(
            eq("idempotency:lock:" + idempotencyKey),
            eq("locked"),
            eq(Duration.ofSeconds(ttlSeconds)));
  }

  @Test
  void shouldNotAcquireLockWhenAlreadyExists() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);
    when(redisTemplate.hasKey(anyString())).thenReturn(true);

    // When
    boolean lockAcquired = idempotencyStore.tryLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(lockAcquired).isFalse();
  }

  @Test
  void shouldStoreAndRetrieveResponse() throws Exception {
    // Given
    String idempotencyKey = "test-key";
    String response = "test-response";
    long ttlSeconds = 3600;

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    String responseJson = objectMapper.writeValueAsString(response);
    when(valueOperations.get(anyString())).thenReturn(responseJson);

    // When
    idempotencyStore.storeResponse(idempotencyKey, response, ttlSeconds);
    Optional<String> retrievedResponse = idempotencyStore.getResponse(idempotencyKey, String.class);

    // Then
    verify(valueOperations)
        .set(
            eq("idempotency:response:" + idempotencyKey),
            eq(responseJson),
            eq(Duration.ofSeconds(ttlSeconds)));

    assertThat(retrievedResponse).isPresent();
    assertThat(retrievedResponse.get()).isEqualTo(response);
  }

  @Test
  void shouldFallbackToDatabaseWhenRedisUnavailable() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;

    // Redis fails
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new RuntimeException("Redis unavailable"));

    // Database fallback succeeds
    when(databaseService.tryDatabaseLock(idempotencyKey, ttlSeconds)).thenReturn(true);

    // When
    boolean lockAcquired = idempotencyStore.tryLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(lockAcquired).isTrue();
    verify(databaseService).tryDatabaseLock(idempotencyKey, ttlSeconds);
  }

  @Test
  void shouldReleaseLock() {
    // Given
    String idempotencyKey = "test-key";

    // When
    idempotencyStore.releaseLock(idempotencyKey);

    // Then
    verify(redisTemplate).delete("idempotency:lock:" + idempotencyKey);
  }
}
