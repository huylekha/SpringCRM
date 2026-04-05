package com.company.platform.shared.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditableEntity Tests")
class AuditableEntityTest {

  private TestAuditableEntityUUID entity;

  @BeforeEach
  void setUp() {
    entity = new TestAuditableEntityUUID();
  }

  @Test
  @DisplayName("Should return false for isModified when not modified")
  void shouldReturnFalseForNotModified() {
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    assertThat(entity.isModified()).isFalse();
  }

  @Test
  @DisplayName("Should return true for isModified when modified")
  void shouldReturnTrueForModified() {
    entity.setCreatedAt(Instant.now().minusSeconds(60));
    entity.setUpdatedAt(Instant.now());
    assertThat(entity.isModified()).isTrue();
  }

  @Test
  @DisplayName("Should return required createdAt when exists")
  void shouldReturnRequiredCreatedAt() {
    Instant created = Instant.now();
    entity.setCreatedAt(created);
    assertThat(entity.getRequiredCreatedAt()).isEqualTo(created);
  }

  @Test
  @DisplayName("Should throw exception when getting required createdAt but is null")
  void shouldThrowExceptionForRequiredCreatedAtWhenNull() {
    assertThatThrownBy(() -> entity.getRequiredCreatedAt())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should return required createdBy when exists")
  void shouldReturnRequiredCreatedBy() {
    UUID creator = UUID.randomUUID();
    entity.setCreatedBy(creator);
    assertThat(entity.getRequiredCreatedBy()).isEqualTo(creator);
  }

  @Test
  @DisplayName("Should throw when getting required createdBy but is null")
  void shouldThrowExceptionForRequiredCreatedByWhenNull() {
    assertThatThrownBy(() -> entity.getRequiredCreatedBy())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should have createdByName and updatedByName fields")
  void shouldHaveNameSnapshotFields() {
    entity.setCreatedByName("Alice");
    entity.setUpdatedByName("Bob");
    assertThat(entity.getCreatedByName()).isEqualTo("Alice");
    assertThat(entity.getUpdatedByName()).isEqualTo("Bob");
  }

  static class TestAuditableEntityUUID extends AuditableEntityUUID {}
}
