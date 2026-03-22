-- V3: Add shared messaging tables for auth-service
-- These tables are required by shared-lib entities that are scanned by @EntityScan

-- Idempotency records table for request deduplication
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

-- Inbox table for deduplication of incoming events
CREATE TABLE inbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(150) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_inbox_message_id ON inbox_messages (message_id);
CREATE INDEX idx_inbox_event_type ON inbox_messages (event_type);
CREATE INDEX idx_inbox_created ON inbox_messages (created_at);

-- Outbox table for reliable event publishing
CREATE TABLE outbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    event_data JSON NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP(6) NOT NULL,
    processed_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_outbox_status_created ON outbox_messages (status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_messages (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_retry ON outbox_messages (status, retry_count, created_at);