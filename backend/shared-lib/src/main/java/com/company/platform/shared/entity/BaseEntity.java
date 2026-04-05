package com.company.platform.shared.entity;

import com.company.platform.shared.audit.AuditTenantEntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditTenantEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity<T extends Serializable> implements Serializable {

  @Id protected T id;

  @Transient private final List<Object> domainEvents = new ArrayList<>();

  public void addDomainEvent(Object event) {
    Objects.requireNonNull(event, "Domain event must not be null");
    this.domainEvents.add(event);
  }

  public List<Object> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  public void clearDomainEvents() {
    this.domainEvents.clear();
  }

  public boolean isPersisted() {
    return this.id != null;
  }

  public T getRequiredId() {
    if (this.id == null) {
      throw new IllegalStateException("Entity ID is required but was null");
    }
    return this.id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    BaseEntity<?> other = (BaseEntity<?>) obj;
    if (this.id != null && other.id != null) {
      return Objects.equals(this.id, other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return String.format("%s{id=%s}", getClass().getSimpleName(), id);
  }
}
