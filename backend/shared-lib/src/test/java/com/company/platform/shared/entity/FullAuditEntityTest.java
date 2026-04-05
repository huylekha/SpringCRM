package com.company.platform.shared.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FullAuditEntity Tests")
class FullAuditEntityTest {

  private TestFullAuditEntityUUID entity;

  @BeforeEach
  void setUp() {
    entity = new TestFullAuditEntityUUID();
    entity.onPrePersist();
  }

  @Test
  @DisplayName("Should perform business soft delete successfully")
  void shouldPerformBusinessSoftDeleteSuccessfully() {
    assertThat(entity.isActive()).isTrue();
    entity.businessSoftDelete();
    assertThat(entity.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Should throw when business soft deleting already deleted entity")
  void shouldThrowExceptionWhenBusinessSoftDeletingAlreadyDeleted() {
    entity.softDelete();
    assertThatThrownBy(() -> entity.businessSoftDelete()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should perform business restore successfully")
  void shouldPerformBusinessRestoreSuccessfully() {
    entity.softDelete();
    entity.businessRestore();
    assertThat(entity.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should throw when business restoring active entity")
  void shouldThrowExceptionWhenBusinessRestoringActiveEntity() {
    assertThatThrownBy(() -> entity.businessRestore()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("canBeDeleted returns true for active entity")
  void canBeDeletedReturnsTrueForActiveEntity() {
    assertThat(entity.canBeDeleted()).isTrue();
  }

  @Test
  @DisplayName("canBeDeleted returns false for deleted entity")
  void canBeDeletedReturnsFalseForDeletedEntity() {
    entity.softDelete();
    assertThat(entity.canBeDeleted()).isFalse();
  }

  @Test
  @DisplayName("canBeRestored returns true for deleted entity")
  void canBeRestoredReturnsTrueForDeletedEntity() {
    entity.softDelete();
    assertThat(entity.canBeRestored()).isTrue();
  }

  @Test
  @DisplayName("canBeRestored returns false for active entity")
  void canBeRestoredReturnsFalseForActiveEntity() {
    assertThat(entity.canBeRestored()).isFalse();
  }

  @Test
  @DisplayName("Should have tenantId field from TenantEntity")
  void shouldHaveTenantIdField() {
    entity.setTenantId("tenant-abc");
    assertThat(entity.getTenantId()).isEqualTo("tenant-abc");
  }

  @Test
  @DisplayName("Should call validation hooks during business operations")
  void shouldCallValidationHooksDuringBusinessOperations() {
    HookTrackingEntity hookEntity = new HookTrackingEntity();
    hookEntity.onPrePersist();

    hookEntity.businessSoftDelete();
    assertThat(hookEntity.validateBeforeDeleteCalled).isTrue();
    assertThat(hookEntity.onAfterSoftDeleteCalled).isTrue();

    hookEntity.businessRestore();
    assertThat(hookEntity.validateBeforeRestoreCalled).isTrue();
    assertThat(hookEntity.onAfterRestoreCalled).isTrue();
  }

  static class TestFullAuditEntityUUID extends FullAuditEntityUUID {}

  static class HookTrackingEntity extends FullAuditEntityUUID {
    boolean validateBeforeDeleteCalled = false;
    boolean validateBeforeRestoreCalled = false;
    boolean onAfterSoftDeleteCalled = false;
    boolean onAfterRestoreCalled = false;

    @Override
    protected void validateBeforeDelete() {
      validateBeforeDeleteCalled = true;
    }

    @Override
    protected void validateBeforeRestore() {
      validateBeforeRestoreCalled = true;
    }

    @Override
    protected void onAfterSoftDelete() {
      onAfterSoftDeleteCalled = true;
    }

    @Override
    protected void onAfterRestore() {
      onAfterRestoreCalled = true;
    }
  }
}
