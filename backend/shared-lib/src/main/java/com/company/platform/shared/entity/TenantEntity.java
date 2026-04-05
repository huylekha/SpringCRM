package com.company.platform.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@Getter
@Setter
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantEntity<T extends Serializable> extends SoftDeletableEntity<T> {

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Override
  public String toString() {
    return String.format(
        "%s{id=%s, tenantId=%s, deleted=%s}",
        getClass().getSimpleName(), getId(), tenantId, getDeleted());
  }
}
