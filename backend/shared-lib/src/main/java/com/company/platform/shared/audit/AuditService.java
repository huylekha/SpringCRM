package com.company.platform.shared.audit;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

/**
 * Service for manual audit operations outside of the standard JPA lifecycle. This service provides
 * utilities for audit field management and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

  private final AuditorAware<String> auditorAware;

  /**
   * Gets the current auditor (user ID).
   *
   * @return the current auditor, or "SYSTEM" if none is available
   */
  public String getCurrentAuditor() {
    return auditorAware.getCurrentAuditor().orElse("SYSTEM");
  }

  /**
   * Gets the current timestamp for audit operations.
   *
   * @return the current instant
   */
  public Instant getCurrentTimestamp() {
    return Instant.now();
  }

  /**
   * Creates an audit record for manual tracking.
   *
   * @param operation the operation being performed
   * @param entityType the type of entity being audited
   * @param entityId the ID of the entity
   * @param details additional details about the operation
   * @return the audit record
   */
  public AuditRecord createAuditRecord(
      String operation, String entityType, String entityId, String details) {
    return AuditRecord.builder()
        .operation(operation)
        .entityType(entityType)
        .entityId(entityId)
        .details(details)
        .performedBy(getCurrentAuditor())
        .performedAt(getCurrentTimestamp())
        .build();
  }

  /**
   * Validates audit fields for consistency.
   *
   * @param createdBy the creator ID
   * @param createdAt the creation timestamp
   * @param updatedBy the updater ID (can be null)
   * @param updatedAt the update timestamp (can be null)
   * @throws IllegalArgumentException if audit fields are inconsistent
   */
  public void validateAuditFields(
      String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
    if (createdBy == null || createdBy.trim().isEmpty()) {
      throw new IllegalArgumentException("createdBy cannot be null or empty");
    }

    if (createdAt == null) {
      throw new IllegalArgumentException("createdAt cannot be null");
    }

    // If update fields are provided, validate them
    if (updatedAt != null) {
      if (updatedBy == null || updatedBy.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "updatedBy cannot be null or empty when updatedAt is provided");
      }

      if (updatedAt.isBefore(createdAt)) {
        throw new IllegalArgumentException("updatedAt cannot be before createdAt");
      }
    }

    // If updatedBy is provided, updatedAt should also be provided
    if (updatedBy != null && !updatedBy.trim().isEmpty() && updatedAt == null) {
      log.warn(
          "updatedBy is provided but updatedAt is null - this may indicate an audit inconsistency");
    }
  }

  /**
   * Checks if audit fields indicate the entity has been modified.
   *
   * @param createdAt the creation timestamp
   * @param updatedAt the update timestamp
   * @return true if the entity has been modified, false otherwise
   */
  public boolean isModified(Instant createdAt, Instant updatedAt) {
    return updatedAt != null && createdAt != null && !updatedAt.equals(createdAt);
  }

  /**
   * Calculates the age of an entity in milliseconds.
   *
   * @param createdAt the creation timestamp
   * @return the age in milliseconds
   */
  public long getEntityAgeMillis(Instant createdAt) {
    if (createdAt == null) {
      throw new IllegalArgumentException("createdAt cannot be null");
    }
    return Instant.now().toEpochMilli() - createdAt.toEpochMilli();
  }

  /**
   * Calculates the time since last modification in milliseconds.
   *
   * @param updatedAt the last update timestamp
   * @return the time since last modification in milliseconds, or 0 if never updated
   */
  public long getTimeSinceLastModificationMillis(Instant updatedAt) {
    if (updatedAt == null) {
      return 0;
    }
    return Instant.now().toEpochMilli() - updatedAt.toEpochMilli();
  }

  /** Simple audit record for manual audit tracking. */
  @lombok.Builder
  @lombok.Data
  public static class AuditRecord {
    private final String operation;
    private final String entityType;
    private final String entityId;
    private final String details;
    private final String performedBy;
    private final Instant performedAt;
  }
}
