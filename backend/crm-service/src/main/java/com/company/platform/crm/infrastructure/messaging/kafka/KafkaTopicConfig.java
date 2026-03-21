package com.company.platform.crm.infrastructure.messaging.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Configuration for Kafka topics used by the CRM service. Follows the naming convention:
 * crm.<aggregate>.<action>
 */
@Configuration
public class KafkaTopicConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return new KafkaAdmin(configs);
  }

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
