-- CRM Service Schema
-- Generated from JPA entity model (FullAuditEntityUUID hierarchy)

-- orders (extends FullAuditEntityUUID)
CREATE TABLE orders (
    id              UUID            NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    created_by      UUID,
    created_by_name VARCHAR(200),
    updated_at      TIMESTAMP,
    updated_by      UUID,
    updated_by_name VARCHAR(200),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    tenant_id       VARCHAR(64)     NOT NULL,
    order_number    VARCHAR(50)     NOT NULL,
    customer_id     UUID            NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(10, 2)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'USD',
    order_date      TIMESTAMP       NOT NULL,
    notes           TEXT,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);

-- order_items (extends BaseEntityUUID)
CREATE TABLE order_items (
    id           UUID            NOT NULL,
    order_id     UUID            NOT NULL,
    product_name VARCHAR(255)    NOT NULL,
    quantity     INTEGER         NOT NULL,
    unit_price   NUMERIC(10, 2)  NOT NULL,
    total_price  NUMERIC(10, 2)  NOT NULL,
    created_at   TIMESTAMP       NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Messaging infrastructure tables

CREATE TABLE outbox_messages (
    id             UUID            NOT NULL,
    aggregate_type VARCHAR(100)    NOT NULL,
    aggregate_id   VARCHAR(100)    NOT NULL,
    event_type     VARCHAR(150)    NOT NULL,
    event_data     TEXT            NOT NULL,
    event_version  INTEGER         NOT NULL DEFAULT 1,
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER         NOT NULL DEFAULT 0,
    max_retries    INTEGER         NOT NULL DEFAULT 3,
    processed_at   TIMESTAMP,
    created_at     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_outbox_messages PRIMARY KEY (id)
);

CREATE TABLE inbox_messages (
    id             UUID            NOT NULL,
    message_id     VARCHAR(255)    NOT NULL,
    event_type     VARCHAR(150)    NOT NULL,
    source_service VARCHAR(100)    NOT NULL,
    processed_at   TIMESTAMP       NOT NULL,
    created_at     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_inbox_messages PRIMARY KEY (id),
    CONSTRAINT uk_inbox_messages_message_id UNIQUE (message_id)
);

CREATE TABLE idempotency_records (
    id              UUID            NOT NULL,
    idempotency_key VARCHAR(255)    NOT NULL,
    request_hash    VARCHAR(255)    NOT NULL,
    response_data   TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING',
    created_at      TIMESTAMP       NOT NULL,
    expires_at      TIMESTAMP       NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_records_key UNIQUE (idempotency_key)
);

-- Indexes for query performance

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_tenant ON orders (tenant_id);
CREATE INDEX idx_orders_order_date ON orders (order_date);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_outbox_messages_status ON outbox_messages (status);
CREATE INDEX idx_inbox_messages_event_type ON inbox_messages (event_type);
CREATE INDEX idx_idempotency_records_expires ON idempotency_records (expires_at);
