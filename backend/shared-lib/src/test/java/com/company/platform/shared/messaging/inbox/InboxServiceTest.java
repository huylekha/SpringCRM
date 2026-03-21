package com.company.platform.shared.messaging.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

  @Mock private InboxMessageRepository inboxRepository;

  private InboxService inboxService;

  @BeforeEach
  void setUp() {
    inboxService = new InboxService(inboxRepository);
  }

  @Test
  void shouldProcessNewMessageSuccessfully() {
    // Given
    String messageId = "msg-123";
    String eventType = "OrderCreatedEvent";
    String sourceService = "order-service";

    when(inboxRepository.existsByMessageId(messageId)).thenReturn(false);

    // When
    boolean result = inboxService.tryProcessMessage(messageId, eventType, sourceService);

    // Then
    assertThat(result).isTrue();

    ArgumentCaptor<InboxMessage> messageCaptor = ArgumentCaptor.forClass(InboxMessage.class);
    verify(inboxRepository).save(messageCaptor.capture());

    InboxMessage savedMessage = messageCaptor.getValue();
    assertThat(savedMessage.getMessageId()).isEqualTo(messageId);
    assertThat(savedMessage.getEventType()).isEqualTo(eventType);
    assertThat(savedMessage.getSourceService()).isEqualTo(sourceService);
  }

  @Test
  void shouldReturnFalseForDuplicateMessage() {
    // Given
    String messageId = "msg-123";
    String eventType = "OrderCreatedEvent";
    String sourceService = "order-service";

    when(inboxRepository.existsByMessageId(messageId)).thenReturn(true);

    // When
    boolean result = inboxService.tryProcessMessage(messageId, eventType, sourceService);

    // Then
    assertThat(result).isFalse();
    verify(inboxRepository, never()).save(any());
  }

  @Test
  void shouldReturnFalseOnRaceCondition() {
    // Given
    String messageId = "msg-123";
    String eventType = "OrderCreatedEvent";
    String sourceService = "order-service";

    when(inboxRepository.existsByMessageId(messageId)).thenReturn(false);
    when(inboxRepository.save(any(InboxMessage.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    // When
    boolean result = inboxService.tryProcessMessage(messageId, eventType, sourceService);

    // Then
    assertThat(result).isFalse();
  }
}
