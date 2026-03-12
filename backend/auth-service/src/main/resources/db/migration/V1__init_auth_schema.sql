CREATE TABLE auth_user (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(6) NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at DATETIME(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    INDEX idx_auth_user_status_deleted (status, deleted),
    INDEX idx_auth_user_created_at (created_at)
);

CREATE TABLE auth_role (
    id CHAR(36) PRIMARY KEY,
    role_code VARCHAR(80) NOT NULL UNIQUE,
    role_name VARCHAR(120) NOT NULL,
    description VARCHAR(300) NULL,
    is_seed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at DATETIME(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    INDEX idx_auth_role_deleted (deleted)
);

CREATE TABLE auth_claim (
    id CHAR(36) PRIMARY KEY,
    claim_code VARCHAR(120) NOT NULL UNIQUE,
    claim_name VARCHAR(150) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at DATETIME(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL
);

CREATE TABLE auth_permission (
    id CHAR(36) PRIMARY KEY,
    permission_code VARCHAR(150) NOT NULL UNIQUE,
    resource_name VARCHAR(80) NOT NULL,
    action_name VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at DATETIME(6) NULL,
    updated_by VARCHAR(36) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    INDEX idx_auth_permission_resource_action (resource_name, action_name)
);

CREATE TABLE auth_user_role (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES auth_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES auth_role(id)
);

CREATE TABLE auth_role_claim (
    role_id CHAR(36) NOT NULL,
    claim_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, claim_id),
    CONSTRAINT fk_role_claim_role FOREIGN KEY (role_id) REFERENCES auth_role(id),
    CONSTRAINT fk_role_claim_claim FOREIGN KEY (claim_id) REFERENCES auth_claim(id)
);

CREATE TABLE auth_role_permission (
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES auth_role(id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES auth_permission(id)
);

CREATE TABLE auth_refresh_token (
    id CHAR(36) PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id CHAR(36) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES auth_user(id),
    INDEX idx_refresh_token_user_revoked (user_id, revoked, expires_at)
);
