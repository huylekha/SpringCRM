package com.company.platform.shared.idempotency;

/** Status of an idempotency record. */
public enum IdempotencyStatus {
  PROCESSING, // Request is currently being processed
  COMPLETED // Request has been completed
}
