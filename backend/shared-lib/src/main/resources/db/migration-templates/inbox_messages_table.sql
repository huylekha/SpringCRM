-- Inbox table for deduplication of incoming events
-- Copy this into your service's migration with appropriate version number
CREATE TABLE inbox_messages (
    id CHAR(36) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(150) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_inbox_message_id (message_id),
    INDEX idx_inbox_event_type (event_type),
    INDEX idx_inbox_created (created_at)
);