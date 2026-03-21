-- Inbox table for deduplication of incoming events
-- Copy this into your service's migration with appropriate version number
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