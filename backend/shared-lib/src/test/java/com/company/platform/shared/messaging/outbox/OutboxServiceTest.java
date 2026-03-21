package com.company.platform.shared.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

  @Mock private OutboxMessageRepository outboxRepository;
  @Mock private ObjectMapper objectMapper;

  private OutboxService outboxService;

  @BeforeEach
  void setUp() {
    outboxService = new OutboxService(outboxRepository, objectMapper);
  }

  @Test
  void shouldStoreEventSuccessfully() throws JsonProcessingException {
    // Given
    String aggregateType = "Order";
    String aggregateId = "order-123";
    String eventType = "OrderCreatedEvent";
    Object eventData = new TestEvent("test-data");
    String expectedJson = "{\"data\":\"test-data\"}";

    when(objectMapper.writeValueAsString(eventData)).thenReturn(expectedJson);

    // When
    outboxService.storeEvent(aggregateType, aggregateId, eventType, eventData);

    // Then
    ArgumentCaptor<OutboxMessage> messageCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
    verify(outboxRepository).save(messageCaptor.capture());

    OutboxMessage savedMessage = messageCaptor.getValue();
    assertThat(savedMessage.getAggregateType()).isEqualTo(aggregateType);
    assertThat(savedMessage.getAggregateId()).isEqualTo(aggregateId);
    assertThat(savedMessage.getEventType()).isEqualTo(eventType);
    assertThat(savedMessage.getEventData()).isEqualTo(expectedJson);
    assertThat(savedMessage.getStatus()).isEqualTo(OutboxStatus.PENDING);
  }

  @Test
  void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
    // Given
    String aggregateType = "Order";
    String aggregateId = "order-123";
    String eventType = "OrderCreatedEvent";
    Object eventData = new TestEvent("test-data");

    when(objectMapper.writeValueAsString(eventData))
        .thenThrow(new JsonProcessingException("Serialization failed") {});

    // When & Then
    assertThatThrownBy(
            () -> outboxService.storeEvent(aggregateType, aggregateId, eventType, eventData))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to serialize event data");

    verify(outboxRepository, never()).save(any());
  }

  @Test
  void shouldStoreMultipleEvents() throws JsonProcessingException {
    // Given
    String aggregateType = "Order";
    String aggregateId = "order-123";
    Object event1 = new TestEvent("event1");
    Object event2 = new TestEvent("event2");

    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    // When
    outboxService.storeEvents(aggregateType, aggregateId, event1, event2);

    // Then
    verify(outboxRepository, times(2)).save(any(OutboxMessage.class));
  }

  private static class TestEvent {
    private final String data;

    TestEvent(String data) {
      this.data = data;
    }

    public String getData() {
      return data;
    }
  }
}
