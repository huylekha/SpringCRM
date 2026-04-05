package com.company.platform.shared.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SoftDeletableEntity Tests")
class SoftDeletableEntityTest {

  private TestSoftDeletableEntityUUID entity;

  @BeforeEach
  void setUp() {
    entity = new TestSoftDeletableEntityUUID();
    entity.onPrePersist();
  }

  @Test
  @DisplayName("Should initialize with deleted=false")
  void shouldInitializeWithDeletedFalse() {
    assertThat(entity.isDeleted()).isFalse();
    assertThat(entity.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should perform soft delete successfully")
  void shouldPerformSoftDeleteSuccessfully() {
    Instant beforeDelete = Instant.now();
    entity.softDelete();
    assertThat(entity.isDeleted()).isTrue();
    assertThat(entity.isActive()).isFalse();
    assertThat(entity.getDeletedAt()).isAfterOrEqualTo(beforeDelete);
  }

  @Test
  @DisplayName("Should throw when soft deleting already deleted entity")
  void shouldThrowExceptionWhenSoftDeletingAlreadyDeleted() {
    entity.softDelete();
    assertThatThrownBy(() -> entity.softDelete()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should restore soft deleted entity successfully")
  void shouldRestoreSoftDeletedEntitySuccessfully() {
    entity.softDelete();
    entity.restore();
    assertThat(entity.isDeleted()).isFalse();
    assertThat(entity.isActive()).isTrue();
    assertThat(entity.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("Should throw when restoring non-deleted entity")
  void shouldThrowExceptionWhenRestoringNonDeletedEntity() {
    assertThatThrownBy(() -> entity.restore()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should return null for deletion timestamp when not deleted")
  void shouldReturnNullForDeletionTimestampWhenNotDeleted() {
    assertThat(entity.getDeletionTimestamp()).isNull();
  }

  @Test
  @DisplayName("ensureActive should not throw for active entity")
  void ensureActiveShouldNotThrowForActiveEntity() {
    assertThatCode(() -> entity.ensureActive()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ensureActive should throw for deleted entity")
  void ensureActiveShouldThrowForDeletedEntity() {
    entity.softDelete();
    assertThatThrownBy(() -> entity.ensureActive()).isInstanceOf(IllegalStateException.class);
  }

  static class TestSoftDeletableEntityUUID extends SoftDeletableEntityUUID {}
}
