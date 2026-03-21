package com.company.platform.shared.cqrs.behavior;

import com.company.platform.shared.cqrs.PipelineBehavior;
import com.company.platform.shared.cqrs.RequestHandlerDelegate;
import com.company.platform.shared.exception.FieldError;
import com.company.platform.shared.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pipeline behavior that validates requests using Jakarta Bean Validation. Executes before other
 * behaviors and throws BusinessException if validation fails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationBehavior<TRequest, TResponse>
    implements PipelineBehavior<TRequest, TResponse> {

  private final Validator validator;

  @Override
  public TResponse handle(TRequest request, RequestHandlerDelegate<TResponse> next) {
    log.debug("Validating request: {}", request.getClass().getSimpleName());

    Set<ConstraintViolation<TRequest>> violations = validator.validate(request);

    if (!violations.isEmpty()) {
      List<FieldError> fieldErrors =
          violations.stream()
              .map(
                  violation ->
                      new FieldError(
                          violation.getPropertyPath().toString(), violation.getMessage()))
              .toList();

      log.warn(
          "Validation failed for {}: {} violations",
          request.getClass().getSimpleName(),
          violations.size());

      throw new ValidationException(fieldErrors);
    }

    return next.handle();
  }

  @Override
  public int getOrder() {
    return 100; // Execute early in pipeline
  }

  @Override
  public boolean canHandle(Class<?> requestType) {
    // Apply validation to all requests
    return true;
  }
}
