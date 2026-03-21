-- Idempotency records table for request deduplication
-- Copy this into your service's migration with appropriate version number
CREATE TABLE idempotency_records (
    id CHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(255) NOT NULL,
    response_data JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_idempotency_expires (expires_at),
    INDEX idx_idempotency_status (status, created_at)
);