-- CRM Service Initial Schema
-- Tables for outbox pattern, inbox pattern, and idempotency

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

-- Orders table (sample aggregate for CQRS demonstration)
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    order_date TIMESTAMP(6) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_order_date ON orders (order_date);
CREATE INDEX idx_orders_deleted ON orders (deleted);
CREATE INDEX idx_orders_number ON orders (order_number);

-- Order items table
CREATE TABLE order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);