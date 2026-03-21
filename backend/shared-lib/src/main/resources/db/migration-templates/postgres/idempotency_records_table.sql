-- Idempotency records table for request deduplication
-- Copy this into your service's migration with appropriate version number
CREATE TABLE idempotency_records (
    id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(255) NOT NULL,
    response_data JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_idempotency_key ON idempotency_records (idempotency_key);
CREATE INDEX idx_idempotency_expires ON idempotency_records (expires_at);
CREATE INDEX idx_idempotency_status ON idempotency_records (status, created_at);