package com.company.platform.shared.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class SoftDeletableEntityLong extends SoftDeletableEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Override
  public Long getId() {
    return super.getId();
  }

  @PrePersist
  protected void onPrePersist() {
    if (this.getDeleted() == null) {
      this.setDeleted(false);
    }
  }
}
