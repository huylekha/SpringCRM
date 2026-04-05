package com.company.platform.shared.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@MappedSuperclass
public abstract class SoftDeletableEntityUUID extends SoftDeletableEntity<UUID> {

  @Override
  @Column(columnDefinition = "UUID")
  public UUID getId() {
    return super.getId();
  }

  @PrePersist
  protected void onPrePersist() {
    if (this.id == null) {
      this.id = UuidCreator.getTimeOrderedEpoch();
    }
    if (this.getDeleted() == null) {
      this.setDeleted(false);
    }
  }
}
