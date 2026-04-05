package com.company.platform.shared.entity;

import java.io.Serializable;

public abstract class FullAuditEntity<T extends Serializable> extends TenantEntity<T> {

  public void businessSoftDelete() {
    ensureActive();
    validateBeforeDelete();
    softDelete();
    onAfterSoftDelete();
  }

  public void businessRestore() {
    ensureDeleted();
    validateBeforeRestore();
    restore();
    onAfterRestore();
  }

  protected void validateBeforeDelete() {}

  protected void validateBeforeRestore() {}

  protected void onAfterSoftDelete() {}

  protected void onAfterRestore() {}

  public boolean canBeDeleted() {
    try {
      ensureActive();
      validateBeforeDelete();
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  public boolean canBeRestored() {
    try {
      ensureDeleted();
      validateBeforeRestore();
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }
}
