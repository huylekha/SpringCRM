package com.company.platform.crm.infrastructure.idempotency;

/** Status of an idempotency record. */
public enum IdempotencyStatus {
  PROCESSING, // Request is currently being processed
  COMPLETED // Request has been completed
}
