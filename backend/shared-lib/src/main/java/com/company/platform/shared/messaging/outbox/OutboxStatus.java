package com.company.platform.shared.messaging.outbox;

/** Status of an outbox message. */
public enum OutboxStatus {
  PENDING, // Message is waiting to be processed
  SENT, // Message has been successfully sent to Kafka
  FAILED // Message failed to send after max retries
}
