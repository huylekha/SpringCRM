package com.company.platform.shared.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing inbox messages and ensuring idempotent message processing. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

  private final InboxMessageRepository inboxRepository;

  /**
   * Try to record a message as processed. Returns true if this is the first time processing this
   * message, false if it's a duplicate.
   */
  @Transactional
  public boolean tryProcessMessage(String messageId, String eventType, String sourceService) {
    try {
      // Check if already processed
      if (inboxRepository.existsByMessageId(messageId)) {
        log.debug("Message already processed: messageId={}, eventType={}", messageId, eventType);
        return false;
      }

      // Record the message as processed
      InboxMessage inboxMessage =
          InboxMessage.builder()
              .messageId(messageId)
              .eventType(eventType)
              .sourceService(sourceService)
              .build();

      inboxRepository.save(inboxMessage);

      log.debug(
          "Recorded new message for processing: messageId={}, eventType={}", messageId, eventType);
      return true;

    } catch (DataIntegrityViolationException e) {
      // Race condition - another thread processed this message
      log.debug("Race condition detected for messageId={}, message already processed", messageId);
      return false;
    }
  }
}
