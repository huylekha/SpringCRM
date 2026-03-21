package com.company.platform.crm.infrastructure.messaging.kafka;

/**
 * Constants for Kafka topic names used by the CRM service. Follows the naming convention:
 * crm.<aggregate>.<action>
 */
public final class KafkaTopics {

  // Order topics
  public static final String ORDER_CREATED = "crm.order.created";
  public static final String ORDER_CREATED_DLQ = "crm.order.created.dlq";
  public static final String ORDER_UPDATED = "crm.order.updated";
  public static final String ORDER_UPDATED_DLQ = "crm.order.updated.dlq";

  // Customer topics (for future use)
  public static final String CUSTOMER_CREATED = "crm.customer.created";
  public static final String CUSTOMER_CREATED_DLQ = "crm.customer.created.dlq";
  public static final String CUSTOMER_UPDATED = "crm.customer.updated";
  public static final String CUSTOMER_UPDATED_DLQ = "crm.customer.updated.dlq";

  private KafkaTopics() {
    // Utility class
  }
}
