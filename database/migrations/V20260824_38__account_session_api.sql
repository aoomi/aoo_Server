CREATE TABLE IF NOT EXISTS aoo_account (
    account_id BIGINT NOT NULL AUTO_INCREMENT,
    login_name VARCHAR(64) NULL,
    password_hash VARCHAR(255) NULL,
    guest_credential_hash CHAR(64) NULL,
    recovery_credential_hash CHAR(64) NULL,
    guest BOOLEAN NOT NULL DEFAULT TRUE,
    auth_generation BIGINT NOT NULL DEFAULT 0,
    banned_until TIMESTAMP(3) NULL,
    ban_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_aoo_account_login (login_name),
    UNIQUE KEY uk_aoo_account_guest (guest_credential_hash),
    UNIQUE KEY uk_aoo_account_recovery (recovery_credential_hash)
);

CREATE TABLE IF NOT EXISTS aoo_account_device (
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    channel_name VARCHAR(64) NOT NULL,
    client_version VARCHAR(64) NOT NULL,
    first_seen_at TIMESTAMP(3) NOT NULL,
    last_seen_at TIMESTAMP(3) NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    PRIMARY KEY (account_id, device_id),
    CONSTRAINT fk_account_device_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
);

CREATE TABLE IF NOT EXISTS aoo_account_session (
    session_id CHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    token_family CHAR(36) NOT NULL,
    access_hash CHAR(64) NOT NULL,
    refresh_hash CHAR(64) NOT NULL,
    access_expires_at TIMESTAMP(3) NOT NULL,
    refresh_expires_at TIMESTAMP(3) NOT NULL,
    auth_generation BIGINT NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    revoke_reason VARCHAR(64) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (session_id),
    UNIQUE KEY uk_account_access_hash (access_hash),
    UNIQUE KEY uk_account_refresh_hash (refresh_hash),
    KEY idx_account_session_owner (account_id, revoked_at),
    CONSTRAINT fk_account_session_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
);

CREATE TABLE IF NOT EXISTS aoo_account_audit (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    detail_value VARCHAR(255) NOT NULL,
    source_ip VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_account_audit_owner_time (account_id, occurred_at),
    CONSTRAINT fk_account_audit_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
);
