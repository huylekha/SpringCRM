-- Auth Service Schema
-- Generated from JPA entity model (FullAuditEntityUUID hierarchy)

-- auth_permission
CREATE TABLE auth_permission (
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
    permission_code VARCHAR(150)    NOT NULL,
    resource_name   VARCHAR(80)     NOT NULL,
    action_name     VARCHAR(80)     NOT NULL,
    CONSTRAINT pk_auth_permission PRIMARY KEY (id),
    CONSTRAINT uk_auth_permission_code UNIQUE (permission_code)
);

-- auth_claim
CREATE TABLE auth_claim (
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
    claim_code      VARCHAR(120)    NOT NULL,
    claim_name      VARCHAR(150)    NOT NULL,
    CONSTRAINT pk_auth_claim PRIMARY KEY (id),
    CONSTRAINT uk_auth_claim_code UNIQUE (claim_code)
);

-- auth_role
CREATE TABLE auth_role (
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
    role_code       VARCHAR(80)     NOT NULL,
    role_name       VARCHAR(120)    NOT NULL,
    description     VARCHAR(300),
    is_seed         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_auth_role PRIMARY KEY (id),
    CONSTRAINT uk_auth_role_code UNIQUE (role_code)
);

-- auth_user
CREATE TABLE auth_user (
    id                    UUID            NOT NULL,
    created_at            TIMESTAMP       NOT NULL,
    created_by            UUID,
    created_by_name       VARCHAR(200),
    updated_at            TIMESTAMP,
    updated_by            UUID,
    updated_by_name       VARCHAR(200),
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP,
    tenant_id             VARCHAR(64)     NOT NULL,
    username              VARCHAR(100)    NOT NULL,
    email                 VARCHAR(255)    NOT NULL,
    password_hash         VARCHAR(255)    NOT NULL,
    status                VARCHAR(30)     NOT NULL,
    full_name             VARCHAR(200),
    last_login_at         TIMESTAMP,
    failed_login_attempts INTEGER         DEFAULT 0,
    CONSTRAINT pk_auth_user PRIMARY KEY (id),
    CONSTRAINT uk_auth_user_username UNIQUE (username),
    CONSTRAINT uk_auth_user_email UNIQUE (email)
);

-- auth_refresh_token (extends BaseEntityUUID, not FullAuditEntityUUID)
CREATE TABLE auth_refresh_token (
    id          UUID            NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    user_id     UUID            NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    CONSTRAINT pk_auth_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_auth_refresh_token_hash UNIQUE (token_hash)
);

-- Join tables

CREATE TABLE auth_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_auth_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_auth_user_role_user FOREIGN KEY (user_id) REFERENCES auth_user (id),
    CONSTRAINT fk_auth_user_role_role FOREIGN KEY (role_id) REFERENCES auth_role (id)
);

CREATE TABLE auth_role_claim (
    role_id  UUID NOT NULL,
    claim_id UUID NOT NULL,
    CONSTRAINT pk_auth_role_claim PRIMARY KEY (role_id, claim_id),
    CONSTRAINT fk_auth_role_claim_role  FOREIGN KEY (role_id)  REFERENCES auth_role (id),
    CONSTRAINT fk_auth_role_claim_claim FOREIGN KEY (claim_id) REFERENCES auth_claim (id)
);

CREATE TABLE auth_role_permission (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_auth_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_auth_role_permission_role       FOREIGN KEY (role_id)       REFERENCES auth_role (id),
    CONSTRAINT fk_auth_role_permission_permission FOREIGN KEY (permission_id) REFERENCES auth_permission (id)
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

CREATE INDEX idx_auth_user_status ON auth_user (status);
CREATE INDEX idx_auth_user_tenant ON auth_user (tenant_id);
CREATE INDEX idx_auth_role_tenant ON auth_role (tenant_id);
CREATE INDEX idx_auth_permission_tenant ON auth_permission (tenant_id);
CREATE INDEX idx_auth_claim_tenant ON auth_claim (tenant_id);
CREATE INDEX idx_auth_refresh_token_user ON auth_refresh_token (user_id);
CREATE INDEX idx_auth_refresh_token_expires ON auth_refresh_token (expires_at);
CREATE INDEX idx_outbox_messages_status ON outbox_messages (status);
CREATE INDEX idx_inbox_messages_event_type ON inbox_messages (event_type);
CREATE INDEX idx_idempotency_records_expires ON idempotency_records (expires_at);
