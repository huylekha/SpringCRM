package com.company.platform.shared.cqrs;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for CQRS infrastructure. Provides default implementations of CommandBus and
 * QueryBus if not already configured.
 */
@Configuration
public class CqrsConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public CommandBus commandBus(
      List<CommandHandler<?, ?>> commandHandlers, List<PipelineBehavior<?, ?>> pipelineBehaviors) {
    return new DefaultCommandBus(commandHandlers, pipelineBehaviors);
  }

  @Bean
  @ConditionalOnMissingBean
  public QueryBus queryBus(
      List<QueryHandler<?, ?>> queryHandlers, List<PipelineBehavior<?, ?>> pipelineBehaviors) {
    return new DefaultQueryBus(queryHandlers, pipelineBehaviors);
  }
}
