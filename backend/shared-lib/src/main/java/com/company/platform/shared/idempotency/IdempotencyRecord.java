package com.company.platform.shared.idempotency;

import com.company.platform.shared.entity.BaseEntityUUID;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity for idempotency records. Provides database-level fallback for Redis-based idempotency.
 */
@Entity
@Table(name = "idempotency_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord extends BaseEntityUUID {

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 255)
  private String requestHash;

  @Column(name = "response_data", columnDefinition = "JSON")
  private String responseData;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Check if this record has expired. */
  public boolean isExpired() {
    return Instant.now().isAfter(this.expiresAt);
  }

  /** Mark the record as completed with response data. */
  public void complete(String responseData) {
    this.status = IdempotencyStatus.COMPLETED;
    this.responseData = responseData;
  }
}
