package com.company.platform.shared.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.platform.shared.cqrs.behavior.LoggingBehavior;
import com.company.platform.shared.cqrs.behavior.ValidationBehavior;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCommandBusTest {

  @Mock private Validator validator;

  private DefaultCommandBus commandBus;
  private TestCommandHandler testHandler;

  @BeforeEach
  void setUp() {
    testHandler = new TestCommandHandler();

    List<CommandHandler<?, ?>> handlers = List.of(testHandler);
    List<PipelineBehavior<?, ?>> behaviors =
        List.of(new ValidationBehavior<>(validator), new LoggingBehavior<>());

    commandBus = new DefaultCommandBus(handlers, behaviors);

    // Mock validator to return no violations
    when(validator.validate(any())).thenReturn(Set.of());
  }

  @Test
  void shouldDispatchCommandToCorrectHandler() {
    // Given
    TestCommand command = new TestCommand("test-data");

    // When
    String result = commandBus.send(command);

    // Then
    assertThat(result).isEqualTo("handled: test-data");
    assertThat(testHandler.wasHandled()).isTrue();
  }

  @Test
  void shouldExecuteBehaviorsInOrder() {
    // Given
    TestCommand command = new TestCommand("test-data");

    // When
    String result = commandBus.send(command);

    // Then
    assertThat(result).isEqualTo("handled: test-data");
    // Validation and logging behaviors should have executed
  }

  // Test command and handler
  static class TestCommand implements Command<String> {
    private final String data;

    public TestCommand(String data) {
      this.data = data;
    }

    public String getData() {
      return data;
    }
  }

  static class TestCommandHandler implements CommandHandler<TestCommand, String> {
    private boolean handled = false;

    @Override
    public String handle(TestCommand command) {
      handled = true;
      return "handled: " + command.getData();
    }

    @Override
    public Class<TestCommand> getCommandType() {
      return TestCommand.class;
    }

    public boolean wasHandled() {
      return handled;
    }
  }
}
