package com.company.platform.shared.cqrs;

import java.util.Optional;

/**
 * Interface for storing and retrieving idempotency information. Implementations should provide both
 * caching and persistent storage for reliability across system restarts and cache evictions.
 */
public interface IdempotencyStore {

  /**
   * Try to acquire an idempotency lock for the given key. Returns true if the lock was acquired
   * (first time processing), false if the request is already being processed or completed.
   *
   * @param idempotencyKey The unique key for this request
   * @param ttlSeconds Time-to-live for the lock in seconds
   * @return true if lock acquired, false if already exists
   */
  boolean tryLock(String idempotencyKey, long ttlSeconds);

  /**
   * Store the response for an idempotent request.
   *
   * @param idempotencyKey The unique key for this request
   * @param response The response to cache
   * @param ttlSeconds Time-to-live for the cached response
   */
  void storeResponse(String idempotencyKey, Object response, long ttlSeconds);

  /**
   * Retrieve a cached response for an idempotent request.
   *
   * @param idempotencyKey The unique key for this request
   * @param responseType The expected response type
   * @return The cached response if available
   */
  <T> Optional<T> getResponse(String idempotencyKey, Class<T> responseType);

  /**
   * Release the idempotency lock (in case of processing failure).
   *
   * @param idempotencyKey The unique key for this request
   */
  void releaseLock(String idempotencyKey);
}
