package com.company.platform.shared.entity;

import static org.assertj.core.api.Assertions.*;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseEntity Tests")
class BaseEntityTest {

  private TestBaseEntityUUID entity1;
  private TestBaseEntityUUID entity2;

  @BeforeEach
  void setUp() {
    entity1 = new TestBaseEntityUUID();
    entity2 = new TestBaseEntityUUID();
  }

  @Test
  @DisplayName("Should generate UUID v7 on prePersist")
  void shouldGenerateUuidV7OnPrePersist() {
    assertThat(entity1.getId()).isNull();
    entity1.onPrePersist();
    assertThat(entity1.getId()).isNotNull();
    assertThat(entity1.getId()).isInstanceOf(UUID.class);
  }

  @Test
  @DisplayName("Should not override existing ID")
  void shouldNotOverrideExistingId() {
    UUID existingId = UuidCreator.getTimeOrderedEpoch();
    entity1.setId(existingId);
    entity1.onPrePersist();
    assertThat(entity1.getId()).isEqualTo(existingId);
  }

  @Test
  @DisplayName("Should return true for isPersisted when ID exists")
  void shouldReturnTrueForPersistedEntity() {
    entity1.setId(UUID.randomUUID());
    assertThat(entity1.isPersisted()).isTrue();
  }

  @Test
  @DisplayName("Should return false for isPersisted when ID is null")
  void shouldReturnFalseForTransientEntity() {
    assertThat(entity1.isPersisted()).isFalse();
  }

  @Test
  @DisplayName("Should return required ID when ID exists")
  void shouldReturnRequiredId() {
    UUID id = UUID.randomUUID();
    entity1.setId(id);
    assertThat(entity1.getRequiredId()).isEqualTo(id);
  }

  @Test
  @DisplayName("Should throw exception when getting required ID but ID is null")
  void shouldThrowExceptionForRequiredIdWhenNull() {
    assertThatThrownBy(() -> entity1.getRequiredId()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should be equal when both entities have same ID")
  void shouldBeEqualWithSameId() {
    UUID id = UUID.randomUUID();
    entity1.setId(id);
    entity2.setId(id);
    assertThat(entity1).isEqualTo(entity2);
  }

  @Test
  @DisplayName("Should not be equal when entities have different IDs")
  void shouldNotBeEqualWithDifferentIds() {
    entity1.setId(UUID.randomUUID());
    entity2.setId(UUID.randomUUID());
    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  @DisplayName("Should support domain events")
  void shouldSupportDomainEvents() {
    Object event1 = new Object();
    Object event2 = new Object();
    entity1.addDomainEvent(event1);
    entity1.addDomainEvent(event2);
    assertThat(entity1.getDomainEvents()).hasSize(2);
    entity1.clearDomainEvents();
    assertThat(entity1.getDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("Domain events list should be unmodifiable")
  void domainEventsListShouldBeUnmodifiable() {
    entity1.addDomainEvent(new Object());
    assertThatThrownBy(() -> entity1.getDomainEvents().add(new Object()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  static class TestBaseEntityUUID extends BaseEntityUUID {}
}
