package com.company.platform.shared.audit;

import com.company.platform.shared.entity.AuditableEntity;
import com.company.platform.shared.entity.TenantEntity;
import com.company.platform.shared.security.RequestContext;
import com.company.platform.shared.security.UserContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuditTenantEntityListener {

  @PrePersist
  public void onPrePersist(Object entity) {
    Instant now = Instant.now();
    UserContext ctx = RequestContext.current();

    if (entity instanceof AuditableEntity<?> auditable) {
      if (auditable.getCreatedAt() == null) {
        auditable.setCreatedAt(now);
      }
      auditable.setUpdatedAt(now);

      if (auditable.getCreatedBy() == null) {
        auditable.setCreatedBy(ctx.userId());
      }
      if (auditable.getCreatedByName() == null) {
        auditable.setCreatedByName(ctx.fullName());
      }

      auditable.setUpdatedBy(ctx.userId());
      auditable.setUpdatedByName(ctx.fullName());
    }

    if (entity instanceof TenantEntity<?> tenant) {
      if (tenant.getTenantId() == null) {
        tenant.setTenantId(ctx.tenantId());
      }
    }

    log.trace(
        "PrePersist: entity={}, userId={}, tenantId={}",
        entity.getClass().getSimpleName(),
        ctx.userId(),
        ctx.tenantId());
  }

  @PreUpdate
  public void onPreUpdate(Object entity) {
    Instant now = Instant.now();
    UserContext ctx = RequestContext.current();

    if (entity instanceof AuditableEntity<?> auditable) {
      auditable.setUpdatedAt(now);
      auditable.setUpdatedBy(ctx.userId());
      auditable.setUpdatedByName(ctx.fullName());
    }

    log.trace("PreUpdate: entity={}, userId={}", entity.getClass().getSimpleName(), ctx.userId());
  }
}
