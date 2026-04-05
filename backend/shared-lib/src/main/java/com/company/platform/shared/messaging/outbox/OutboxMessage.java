package com.company.platform.shared.messaging.outbox;

import com.company.platform.shared.entity.BaseEntityUUID;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** JPA entity for outbox messages. Stores domain events that need to be published to Kafka. */
@Entity
@Table(name = "outbox_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage extends BaseEntityUUID {

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 100)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 150)
  private String eventType;

  @Column(name = "event_data", nullable = false, columnDefinition = "JSON")
  private String eventData;

  @Column(name = "event_version", nullable = false)
  @Builder.Default
  private Integer eventVersion = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "retry_count", nullable = false)
  @Builder.Default
  private Integer retryCount = 0;

  @Column(name = "max_retries", nullable = false)
  @Builder.Default
  private Integer maxRetries = 3;

  @Column(name = "processed_at")
  private Instant processedAt;

  /** Mark the message as sent successfully. */
  public void markAsSent() {
    this.status = OutboxStatus.SENT;
    this.processedAt = Instant.now();
  }

  /** Mark the message as failed and increment retry count. */
  public void markAsFailed() {
    this.retryCount++;
    if (this.retryCount >= this.maxRetries) {
      this.status = OutboxStatus.FAILED;
    }
    this.processedAt = Instant.now();
  }

  /** Check if the message can be retried. */
  public boolean canRetry() {
    return this.status == OutboxStatus.PENDING && this.retryCount < this.maxRetries;
  }

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
