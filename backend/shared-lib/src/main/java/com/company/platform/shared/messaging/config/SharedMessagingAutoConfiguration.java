package com.company.platform.shared.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for shared messaging infrastructure. Note: Services need to
 * configure @EntityScan and @EnableJpaRepositories to include shared packages in their main
 * application class.
 */
@Configuration
public class SharedMessagingAutoConfiguration {

  /**
   * Configuration for Kafka-dependent components. Only activated when Kafka is on the classpath.
   */
  @Configuration
  @ConditionalOnClass(KafkaTemplate.class)
  static class KafkaMessagingConfiguration {
    // Kafka-specific beans are auto-configured through component scanning
    // OutboxPublisherWorker will be activated when EventTopicMapper is available
  }
}
