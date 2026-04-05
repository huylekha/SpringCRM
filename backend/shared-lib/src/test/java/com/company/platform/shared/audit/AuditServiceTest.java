package com.company.platform.shared.audit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.AuditorAware;

/** Unit tests for AuditService. */
@DisplayName("AuditService Tests")
class AuditServiceTest {

  private AuditService auditService;

  @Mock private AuditorAware<String> auditorAware;

  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    auditService = new AuditService(auditorAware);
  }

  @Test
  @DisplayName("Should return current auditor from AuditorAware")
  void shouldReturnCurrentAuditorFromAuditorAware() {
    // Given
    String expectedAuditor = "user-123";
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(expectedAuditor));

    // When
    String auditor = auditService.getCurrentAuditor();

    // Then
    assertThat(auditor).isEqualTo(expectedAuditor);
  }

  @Test
  @DisplayName("Should return SYSTEM when AuditorAware returns empty")
  void shouldReturnSystemWhenAuditorAwareReturnsEmpty() {
    // Given
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());

    // When
    String auditor = auditService.getCurrentAuditor();

    // Then
    assertThat(auditor).isEqualTo("SYSTEM");
  }

  @Test
  @DisplayName("Should return current timestamp")
  void shouldReturnCurrentTimestamp() {
    // When
    Instant before = Instant.now();
    Instant timestamp = auditService.getCurrentTimestamp();
    Instant after = Instant.now();

    // Then
    assertThat(timestamp).isBetween(before, after);
  }

  @Test
  @DisplayName("Should create audit record with all fields")
  void shouldCreateAuditRecordWithAllFields() {
    // Given
    String expectedAuditor = "user-123";
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(expectedAuditor));

    String operation = "CREATE";
    String entityType = "User";
    String entityId = "user-456";
    String details = "Created new user";

    // When
    Instant before = Instant.now();
    AuditService.AuditRecord record =
        auditService.createAuditRecord(operation, entityType, entityId, details);
    Instant after = Instant.now();

    // Then
    assertThat(record.getOperation()).isEqualTo(operation);
    assertThat(record.getEntityType()).isEqualTo(entityType);
    assertThat(record.getEntityId()).isEqualTo(entityId);
    assertThat(record.getDetails()).isEqualTo(details);
    assertThat(record.getPerformedBy()).isEqualTo(expectedAuditor);
    assertThat(record.getPerformedAt()).isBetween(before, after);
  }

  @Test
  @DisplayName("Should validate valid audit fields")
  void shouldValidateValidAuditFields() {
    // Given
    String createdBy = "user-123";
    Instant createdAt = Instant.now().minusSeconds(60);
    String updatedBy = "user-456";
    Instant updatedAt = Instant.now();

    // When & Then
    assertThatCode(
            () -> auditService.validateAuditFields(createdBy, createdAt, updatedBy, updatedAt))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should throw exception when createdBy is null")
  void shouldThrowExceptionWhenCreatedByIsNull() {
    // Given
    Instant createdAt = Instant.now();

    // When & Then
    assertThatThrownBy(() -> auditService.validateAuditFields(null, createdAt, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("createdBy cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when createdBy is empty")
  void shouldThrowExceptionWhenCreatedByIsEmpty() {
    // Given
    Instant createdAt = Instant.now();

    // When & Then
    assertThatThrownBy(() -> auditService.validateAuditFields("   ", createdAt, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("createdBy cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when createdAt is null")
  void shouldThrowExceptionWhenCreatedAtIsNull() {
    // When & Then
    assertThatThrownBy(() -> auditService.validateAuditFields("user-123", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("createdAt cannot be null");
  }

  @Test
  @DisplayName("Should throw exception when updatedBy is null but updatedAt is provided")
  void shouldThrowExceptionWhenUpdatedByIsNullButUpdatedAtIsProvided() {
    // Given
    String createdBy = "user-123";
    Instant createdAt = Instant.now().minusSeconds(60);
    Instant updatedAt = Instant.now();

    // When & Then
    assertThatThrownBy(
            () -> auditService.validateAuditFields(createdBy, createdAt, null, updatedAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("updatedBy cannot be null or empty when updatedAt is provided");
  }

  @Test
  @DisplayName("Should throw exception when updatedAt is before createdAt")
  void shouldThrowExceptionWhenUpdatedAtIsBeforeCreatedAt() {
    // Given
    String createdBy = "user-123";
    Instant createdAt = Instant.now();
    String updatedBy = "user-456";
    Instant updatedAt = createdAt.minusSeconds(60); // Before createdAt

    // When & Then
    assertThatThrownBy(
            () -> auditService.validateAuditFields(createdBy, createdAt, updatedBy, updatedAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("updatedAt cannot be before createdAt");
  }

  @Test
  @DisplayName("Should return true for isModified when timestamps are different")
  void shouldReturnTrueForIsModifiedWhenTimestampsAreDifferent() {
    // Given
    Instant createdAt = Instant.now().minusSeconds(60);
    Instant updatedAt = Instant.now();

    // When & Then
    assertThat(auditService.isModified(createdAt, updatedAt)).isTrue();
  }

  @Test
  @DisplayName("Should return false for isModified when timestamps are same")
  void shouldReturnFalseForIsModifiedWhenTimestampsAreSame() {
    // Given
    Instant timestamp = Instant.now();

    // When & Then
    assertThat(auditService.isModified(timestamp, timestamp)).isFalse();
  }

  @Test
  @DisplayName("Should return false for isModified when updatedAt is null")
  void shouldReturnFalseForIsModifiedWhenUpdatedAtIsNull() {
    // Given
    Instant createdAt = Instant.now();

    // When & Then
    assertThat(auditService.isModified(createdAt, null)).isFalse();
  }

  @Test
  @DisplayName("Should calculate entity age in milliseconds")
  void shouldCalculateEntityAgeInMilliseconds() {
    // Given
    Instant createdAt = Instant.now().minusSeconds(60); // 60 seconds ago

    // When
    long ageMillis = auditService.getEntityAgeMillis(createdAt);

    // Then
    assertThat(ageMillis).isBetween(59000L, 61000L); // Around 60 seconds
  }

  @Test
  @DisplayName("Should throw exception when calculating age with null createdAt")
  void shouldThrowExceptionWhenCalculatingAgeWithNullCreatedAt() {
    // When & Then
    assertThatThrownBy(() -> auditService.getEntityAgeMillis(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("createdAt cannot be null");
  }

  @Test
  @DisplayName("Should calculate time since last modification")
  void shouldCalculateTimeSinceLastModification() {
    // Given
    Instant updatedAt = Instant.now().minusSeconds(30); // 30 seconds ago

    // When
    long timeSinceModification = auditService.getTimeSinceLastModificationMillis(updatedAt);

    // Then
    assertThat(timeSinceModification).isBetween(29000L, 31000L); // Around 30 seconds
  }

  @Test
  @DisplayName("Should return zero for time since last modification when updatedAt is null")
  void shouldReturnZeroForTimeSinceLastModificationWhenUpdatedAtIsNull() {
    // When & Then
    assertThat(auditService.getTimeSinceLastModificationMillis(null)).isEqualTo(0L);
  }
}
