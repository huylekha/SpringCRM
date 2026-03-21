package com.company.platform.shared.cqrs.behavior;

import com.company.platform.shared.cqrs.IdempotencyStore;
import com.company.platform.shared.cqrs.Idempotent;
import com.company.platform.shared.cqrs.PipelineBehavior;
import com.company.platform.shared.cqrs.RequestHandlerDelegate;
import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Pipeline behavior that implements idempotency for commands marked with @Idempotent. Uses an
 * IdempotencyStore to cache responses and prevent duplicate processing.
 */
@Component
@ConditionalOnBean(IdempotencyStore.class)
@RequiredArgsConstructor
@Slf4j
public class IdempotencyBehavior<TRequest, TResponse>
    implements PipelineBehavior<TRequest, TResponse> {

  private final IdempotencyStore idempotencyStore;

  @Override
  @SuppressWarnings("unchecked")
  public TResponse handle(TRequest request, RequestHandlerDelegate<TResponse> next) {
    Idempotent idempotentAnnotation = request.getClass().getAnnotation(Idempotent.class);
    if (idempotentAnnotation == null) {
      // Not an idempotent request, continue normally
      return next.handle();
    }

    String idempotencyKey = generateIdempotencyKey(request);
    long ttlSeconds = idempotentAnnotation.ttlSeconds();

    log.debug("Processing idempotent request: key={}, ttl={}s", idempotencyKey, ttlSeconds);

    // Check if we already have a cached response
    Optional<TResponse> cachedResponse =
        idempotencyStore.getResponse(idempotencyKey, (Class<TResponse>) Object.class);

    if (cachedResponse.isPresent()) {
      log.debug("Returning cached response for idempotency key: {}", idempotencyKey);
      return cachedResponse.get();
    }

    // Try to acquire lock for processing
    if (!idempotencyStore.tryLock(idempotencyKey, ttlSeconds)) {
      // Another request is already processing or completed
      // Wait a bit and check for cached response again
      try {
        Thread.sleep(100); // Brief wait
        Optional<TResponse> retryResponse =
            idempotencyStore.getResponse(idempotencyKey, (Class<TResponse>) Object.class);
        if (retryResponse.isPresent()) {
          return retryResponse.get();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      throw new BusinessException(ErrorCode.SYSTEM_TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);
    }

    try {
      // Process the request
      TResponse response = next.handle();

      // Cache the response
      idempotencyStore.storeResponse(idempotencyKey, response, ttlSeconds);

      log.debug("Stored response for idempotency key: {}", idempotencyKey);
      return response;

    } catch (Exception ex) {
      // Release lock on failure
      idempotencyStore.releaseLock(idempotencyKey);
      throw ex;
    }
  }

  @Override
  public int getOrder() {
    return 200; // Execute after validation, before transaction
  }

  @Override
  public boolean canHandle(Class<?> requestType) {
    return requestType.isAnnotationPresent(Idempotent.class);
  }

  /**
   * Generate a unique idempotency key for the request. Uses SHA-256 hash of the request class name
   * and serialized content.
   */
  private String generateIdempotencyKey(TRequest request) {
    try {
      String content = request.getClass().getName() + ":" + request.toString();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
