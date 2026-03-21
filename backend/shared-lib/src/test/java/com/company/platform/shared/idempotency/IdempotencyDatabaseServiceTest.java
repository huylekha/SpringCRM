package com.company.platform.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotencyDatabaseServiceTest {

  @Mock private IdempotencyRecordRepository idempotencyRepository;
  @Mock private ObjectMapper objectMapper;

  private IdempotencyDatabaseService databaseService;

  @BeforeEach
  void setUp() {
    databaseService = new IdempotencyDatabaseService(idempotencyRepository, objectMapper);
  }

  @Test
  void shouldAcquireLockForNewKey() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;

    when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

    // When
    boolean result = databaseService.tryDatabaseLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(result).isTrue();

    ArgumentCaptor<IdempotencyRecord> recordCaptor =
        ArgumentCaptor.forClass(IdempotencyRecord.class);
    verify(idempotencyRepository).save(recordCaptor.capture());

    IdempotencyRecord savedRecord = recordCaptor.getValue();
    assertThat(savedRecord.getIdempotencyKey()).isEqualTo(idempotencyKey);
    assertThat(savedRecord.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
  }

  @Test
  void shouldReturnFalseForExistingKey() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;

    IdempotencyRecord existingRecord =
        IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .expiresAt(Instant.now().plusSeconds(ttlSeconds))
            .build();

    when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.of(existingRecord));

    // When
    boolean result = databaseService.tryDatabaseLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(result).isFalse();
    verify(idempotencyRepository, never()).save(any());
  }

  @Test
  void shouldReturnFalseOnRaceCondition() {
    // Given
    String idempotencyKey = "test-key";
    long ttlSeconds = 3600;

    when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(idempotencyRepository.save(any(IdempotencyRecord.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    // When
    boolean result = databaseService.tryDatabaseLock(idempotencyKey, ttlSeconds);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void shouldStoreResponseSuccessfully() {
    // Given
    String idempotencyKey = "test-key";
    String responseJson = "{\"result\":\"success\"}";
    long ttlSeconds = 3600;

    IdempotencyRecord existingRecord =
        IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .status(IdempotencyStatus.PROCESSING)
            .build();

    when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.of(existingRecord));

    // When
    databaseService.storeResponseInDatabase(idempotencyKey, responseJson, ttlSeconds);

    // Then
    verify(idempotencyRepository).save(existingRecord);
    assertThat(existingRecord.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
    assertThat(existingRecord.getResponseData()).isEqualTo(responseJson);
  }

  @Test
  void shouldRetrieveResponseSuccessfully() throws JsonProcessingException {
    // Given
    String idempotencyKey = "test-key";
    String responseJson = "{\"result\":\"success\"}";
    TestResponse expectedResponse = new TestResponse("success");

    IdempotencyRecord record =
        IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .responseData(responseJson)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.of(record));
    when(objectMapper.readValue(responseJson, TestResponse.class)).thenReturn(expectedResponse);

    // When
    Optional<TestResponse> result =
        databaseService.getDatabaseResponse(idempotencyKey, TestResponse.class);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getResult()).isEqualTo("success");
  }

  private static class TestResponse {
    private final String result;

    TestResponse(String result) {
      this.result = result;
    }

    public String getResult() {
      return result;
    }
  }
}
