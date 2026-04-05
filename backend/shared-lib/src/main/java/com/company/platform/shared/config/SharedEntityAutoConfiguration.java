package com.company.platform.shared.config;

import com.company.platform.shared.audit.AuditConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.config.EnableJpaAuditing")
@Import({AuditConfiguration.class})
@ComponentScan(
    basePackages = {
      "com.company.platform.shared.security",
      "com.company.platform.shared.audit",
      "com.company.platform.shared.event",
      "com.company.platform.shared.cache"
    })
@Slf4j
public class SharedEntityAutoConfiguration {

  public SharedEntityAutoConfiguration() {
    log.info("Initializing Shared Entity Auto Configuration");
    log.info(
        "Entity hierarchy: BaseEntity -> AuditableEntity -> SoftDeletableEntity -> TenantEntity -> FullAuditEntity");
    log.info("Infrastructure: AuditTenantEntityListener, RequestContext, RequestContextFilter");
  }
}
