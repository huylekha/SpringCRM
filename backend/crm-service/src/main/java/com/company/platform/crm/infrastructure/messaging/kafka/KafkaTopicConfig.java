package com.company.platform.crm.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration for Kafka topics used by the CRM service. Follows the naming convention:
 * crm.<aggregate>.<action>
 */
@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(KafkaTopics.ORDER_CREATED).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic orderCreatedDlqTopic() {
    return TopicBuilder.name(KafkaTopics.ORDER_CREATED_DLQ).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic orderUpdatedTopic() {
    return TopicBuilder.name(KafkaTopics.ORDER_UPDATED).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic orderUpdatedDlqTopic() {
    return TopicBuilder.name(KafkaTopics.ORDER_UPDATED_DLQ).partitions(3).replicas(1).build();
  }
}
