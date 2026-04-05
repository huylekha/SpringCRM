package com.company.platform.shared.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

  @Query(
      "SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' AND o.retryCount < o.maxRetries ORDER BY o.createdAt ASC")
  List<OutboxMessage> findPendingMessages(Pageable pageable);

  List<OutboxMessage> findByStatus(OutboxStatus status);

  @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'SENT' AND o.processedAt < :cutoffTime")
  List<OutboxMessage> findOldProcessedMessages(Instant cutoffTime, Pageable pageable);

  long countByStatus(OutboxStatus status);
}
