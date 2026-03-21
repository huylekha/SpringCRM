-- Outbox table for reliable event publishing
-- Copy this into your service's migration with appropriate version number
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