package com.company.platform.crm.infrastructure.messaging.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for outbox messages. */
@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {

  /** Find pending messages that can be processed, ordered by creation time. */
  @Query(
      "SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' AND o.retryCount < o.maxRetries ORDER BY o.createdAt ASC")
  List<OutboxMessage> findPendingMessages(Pageable pageable);

  /** Find failed messages for monitoring/cleanup. */
  List<OutboxMessage> findByStatus(OutboxStatus status);

  /** Find old processed messages for cleanup. */
  @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'SENT' AND o.processedAt < :cutoffTime")
  List<OutboxMessage> findOldProcessedMessages(Instant cutoffTime, Pageable pageable);

  /** Count pending messages for monitoring. */
  long countByStatus(OutboxStatus status);
}
