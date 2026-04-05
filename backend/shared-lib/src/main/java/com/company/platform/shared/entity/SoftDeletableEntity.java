package com.company.platform.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@MappedSuperclass
@Getter
@Setter
@SQLRestriction("deleted = false")
public abstract class SoftDeletableEntity<T extends Serializable> extends AuditableEntity<T> {

  @Column(nullable = false)
  private Boolean deleted = false;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public void softDelete() {
    if (isDeleted()) {
      throw new IllegalStateException("Entity is already deleted");
    }
    this.deleted = true;
    this.deletedAt = Instant.now();
  }

  public void restore() {
    if (!isDeleted()) {
      throw new IllegalStateException("Entity is not deleted, cannot restore");
    }
    this.deleted = false;
    this.deletedAt = null;
  }

  public boolean isDeleted() {
    return Boolean.TRUE.equals(this.deleted);
  }

  public boolean isActive() {
    return !isDeleted();
  }

  public Instant getDeletionTimestamp() {
    return isDeleted() ? deletedAt : null;
  }

  public void ensureActive() {
    if (isDeleted()) {
      throw new IllegalStateException("Operation not allowed on deleted entity");
    }
  }

  public void ensureDeleted() {
    if (!isDeleted()) {
      throw new IllegalStateException("Operation requires entity to be deleted");
    }
  }

  @Override
  public String toString() {
    return String.format(
        "%s{id=%s, createdAt=%s, createdBy=%s, updatedAt=%s, updatedBy=%s, deleted=%s, deletedAt=%s}",
        getClass().getSimpleName(),
        getId(),
        getCreatedAt(),
        getCreatedBy(),
        getUpdatedAt(),
        getUpdatedBy(),
        deleted,
        deletedAt);
  }
}
