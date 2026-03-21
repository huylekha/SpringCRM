package com.company.platform.crm.infrastructure.messaging.inbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** JPA entity for inbox messages. Used for deduplication of incoming Kafka messages. */
@Entity
@Table(name = "inbox_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxMessage {

  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "message_id", nullable = false, unique = true, length = 255)
  private String messageId;

  @Column(name = "event_type", nullable = false, length = 150)
  private String eventType;

  @Column(name = "source_service", nullable = false, length = 100)
  private String sourceService;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.processedAt == null) {
      this.processedAt = Instant.now();
    }
  }
}
