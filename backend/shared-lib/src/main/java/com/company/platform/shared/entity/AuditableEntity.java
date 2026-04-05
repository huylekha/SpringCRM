package com.company.platform.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity<T extends Serializable> extends BaseEntity<T> {

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", updatable = false, columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "created_by_name", updatable = false, length = 200)
  private String createdByName;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", columnDefinition = "UUID")
  private UUID updatedBy;

  @Column(name = "updated_by_name", length = 200)
  private String updatedByName;

  public boolean isModified() {
    return updatedAt != null && createdAt != null && !updatedAt.equals(createdAt);
  }

  public Instant getRequiredCreatedAt() {
    if (this.createdAt == null) {
      throw new IllegalStateException("Entity creation timestamp is required but was null");
    }
    return this.createdAt;
  }

  public UUID getRequiredCreatedBy() {
    if (this.createdBy == null) {
      throw new IllegalStateException("Entity creator ID is required but was null");
    }
    return this.createdBy;
  }

  @Override
  public String toString() {
    return String.format(
        "%s{id=%s, createdAt=%s, createdBy=%s, updatedAt=%s, updatedBy=%s}",
        getClass().getSimpleName(), getId(), createdAt, createdBy, updatedAt, updatedBy);
  }
}
