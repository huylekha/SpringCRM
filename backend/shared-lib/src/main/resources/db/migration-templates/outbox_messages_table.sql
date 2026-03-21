-- Outbox table for reliable event publishing
-- Copy this into your service's migration with appropriate version number
CREATE TABLE outbox_messages (
    id CHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    event_data JSON NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id),
    INDEX idx_outbox_retry (status, retry_count, created_at)
);