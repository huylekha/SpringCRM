package com.company.platform.crm.infrastructure.messaging.inbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for inbox messages. */
@Repository
public interface InboxMessageRepository extends JpaRepository<InboxMessage, String> {

  /** Check if a message has already been processed. */
  boolean existsByMessageId(String messageId);

  /** Find old processed messages for cleanup. */
  @Query("SELECT i FROM InboxMessage i WHERE i.createdAt < :cutoffTime")
  List<InboxMessage> findOldMessages(Instant cutoffTime, Pageable pageable);
}
