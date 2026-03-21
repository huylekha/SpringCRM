package com.company.platform.crm.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database operations service for idempotency management. Separated to ensure proper transaction
 * proxy behavior.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyDatabaseService {

  private final IdempotencyRecordRepository idempotencyRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public boolean tryDatabaseLock(String idempotencyKey, long ttlSeconds) {
    try {
      // Check if record already exists
      Optional<IdempotencyRecord> existing =
          idempotencyRepository.findByIdempotencyKey(idempotencyKey);
      if (existing.isPresent()) {
        IdempotencyRecord record = existing.get();
        if (record.isExpired()) {
          // Clean up expired record
          idempotencyRepository.delete(record);
        } else {
          log.debug("Database idempotency record already exists for key: {}", idempotencyKey);
          return false;
        }
      }

      // Create new record
      IdempotencyRecord record =
          IdempotencyRecord.builder()
              .idempotencyKey(idempotencyKey)
              .requestHash(idempotencyKey) // Simplified for now
              .expiresAt(Instant.now().plusSeconds(ttlSeconds))
              .build();

      idempotencyRepository.save(record);
      log.debug("Acquired database lock for idempotency key: {}", idempotencyKey);
      return true;

    } catch (DataIntegrityViolationException e) {
      // Race condition - another thread created the record
      log.debug("Database lock race condition for idempotency key: {}", idempotencyKey);
      return false;
    }
  }

  @Transactional
  public void storeResponseInDatabase(String idempotencyKey, String responseJson, long ttlSeconds) {
    try {
      Optional<IdempotencyRecord> existing =
          idempotencyRepository.findByIdempotencyKey(idempotencyKey);
      if (existing.isPresent()) {
        IdempotencyRecord record = existing.get();
        record.complete(responseJson);
        idempotencyRepository.save(record);
        log.debug("Updated database response for idempotency key: {}", idempotencyKey);
      }
    } catch (Exception e) {
      log.warn("Failed to store response in database for key: {}", idempotencyKey, e);
    }
  }

  public <T> Optional<T> getDatabaseResponse(String idempotencyKey, Class<T> responseType) {
    try {
      Optional<IdempotencyRecord> record =
          idempotencyRepository.findByIdempotencyKey(idempotencyKey);
      if (record.isPresent() && record.get().getResponseData() != null) {
        IdempotencyRecord idempotencyRecord = record.get();
        if (!idempotencyRecord.isExpired()) {
          T response = objectMapper.readValue(idempotencyRecord.getResponseData(), responseType);
          log.debug("Retrieved response from database for idempotency key: {}", idempotencyKey);
          return Optional.of(response);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to retrieve response from database for key: {}", idempotencyKey, e);
    }

    return Optional.empty();
  }

  @Transactional
  public void releaseDatabaseLock(String idempotencyKey) {
    try {
      Optional<IdempotencyRecord> record =
          idempotencyRepository.findByIdempotencyKey(idempotencyKey);
      if (record.isPresent() && record.get().getStatus() == IdempotencyStatus.PROCESSING) {
        idempotencyRepository.delete(record.get());
        log.debug("Released database lock for idempotency key: {}", idempotencyKey);
      }
    } catch (Exception e) {
      log.warn("Failed to release database lock for key: {}", idempotencyKey, e);
    }
  }
}
