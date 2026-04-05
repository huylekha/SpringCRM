package com.company.platform.shared.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Centralized audit configuration for Spring Data JPA auditing. This configuration enables
 * automatic population of audit fields (createdBy, updatedBy, createdAt, updatedAt) across all
 * services that include the shared-lib dependency.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@RequiredArgsConstructor
@Slf4j
public class AuditConfiguration {

  /**
   * Provides the AuditorAware implementation for automatic user tracking. Uses
   * SecurityContextAuditorAware by default, but can be overridden by services if they need custom
   * auditor resolution logic.
   *
   * @return the AuditorAware implementation
   */
  @Bean
  @ConditionalOnMissingBean(AuditorAware.class)
  public AuditorAware<String> auditorAware() {
    log.info("Configuring SecurityContextAuditorAware for JPA auditing");
    return new SecurityContextAuditorAware();
  }

  /**
   * Provides a standalone audit service for manual audit operations. This can be used when you need
   * to perform audit operations outside of the standard JPA lifecycle.
   *
   * @param auditorAware the auditor aware implementation
   * @return the audit service
   */
  @Bean
  @ConditionalOnMissingBean(AuditService.class)
  public AuditService auditService(AuditorAware<String> auditorAware) {
    log.info("Configuring AuditService for manual audit operations");
    return new AuditService(auditorAware);
  }
}
