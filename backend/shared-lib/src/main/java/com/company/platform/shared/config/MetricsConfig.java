package com.company.platform.shared.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
      @Value("${spring.application.name}") String serviceName,
      @Value("${SPRING_PROFILES_ACTIVE:default}") String environment) {
    return registry ->
        registry
            .config()
            .commonTags(
                "service", serviceName,
                "environment", environment);
  }

  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
