CREATE TABLE auth_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP(6) NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_auth_user_status_deleted ON auth_user (status, deleted);
CREATE INDEX idx_auth_user_created_at ON auth_user (created_at);

CREATE TABLE auth_role (
    id VARCHAR(36) PRIMARY KEY,
    role_code VARCHAR(80) NOT NULL UNIQUE,
    role_name VARCHAR(120) NOT NULL,
    description VARCHAR(300) NULL,
    is_seed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_auth_role_deleted ON auth_role (deleted);

CREATE TABLE auth_claim (
    id VARCHAR(36) PRIMARY KEY,
    claim_code VARCHAR(120) NOT NULL UNIQUE,
    claim_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL
);

CREATE TABLE auth_permission (
    id VARCHAR(36) PRIMARY KEY,
    permission_code VARCHAR(150) NOT NULL UNIQUE,
    resource_name VARCHAR(80) NOT NULL,
    action_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_auth_permission_resource_action ON auth_permission (resource_name, action_name);

CREATE TABLE auth_user_role (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES auth_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES auth_role(id)
);

CREATE TABLE auth_role_claim (
    role_id VARCHAR(36) NOT NULL,
    claim_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, claim_id),
    CONSTRAINT fk_role_claim_role FOREIGN KEY (role_id) REFERENCES auth_role(id),
    CONSTRAINT fk_role_claim_claim FOREIGN KEY (claim_id) REFERENCES auth_claim(id)
);

CREATE TABLE auth_role_permission (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES auth_role(id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES auth_permission(id)
);

CREATE TABLE auth_refresh_token (
    id VARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES auth_user(id)
);

CREATE INDEX idx_refresh_token_user_revoked ON auth_refresh_token (user_id, revoked, expires_at);