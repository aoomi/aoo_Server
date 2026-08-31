-- Chapter 14: account_id is immutable authority; every sign-in identifier is a mapping.
CREATE TABLE IF NOT EXISTS aoo_display_id_segment (
    digit_length INT NOT NULL,
    range_start DECIMAL(65,0) NOT NULL,
    range_end DECIMAL(65,0) NOT NULL,
    capacity DECIMAL(65,0) NOT NULL,
    occupied_count DECIMAL(65,0) NOT NULL DEFAULT 0,
    allocation_threshold_bps INT NOT NULL DEFAULT 8500,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (digit_length),
    CONSTRAINT ck_display_segment_digits CHECK (digit_length >= 6),
    CONSTRAINT ck_display_segment_threshold CHECK (allocation_threshold_bps BETWEEN 1 AND 10000)
);

INSERT INTO aoo_display_id_segment(digit_length,range_start,range_end,capacity,occupied_count,allocation_threshold_bps,status,updated_at)
VALUES(6,100000,999999,900000,0,8500,'ACTIVE',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE digit_length=VALUES(digit_length);

CREATE TABLE IF NOT EXISTS aoo_account_identity (
    identity_id BIGINT NOT NULL AUTO_INCREMENT,
    identity_type VARCHAR(24) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    value_hash CHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    active_value_hash CHAR(64) GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN value_hash ELSE NULL END) STORED,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY(identity_id),
    UNIQUE KEY uk_account_identity_active(identity_type,active_value_hash),
    KEY idx_account_identity_owner(account_id,status,identity_type),
    CONSTRAINT fk_account_identity_owner FOREIGN KEY(account_id) REFERENCES aoo_account(account_id),
    CONSTRAINT ck_account_identity_type CHECK(identity_type IN ('DISPLAY_ID','ALIAS','PHONE','WECHAT_UNION','WECHAT_OPEN','TRUSTED_DEVICE')),
    CONSTRAINT ck_account_identity_status CHECK(status IN ('ACTIVE','DISABLED','MIGRATED'))
);

CREATE TABLE IF NOT EXISTS aoo_display_id_history (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    old_display_id VARCHAR(65) NULL,
    new_display_id VARCHAR(65) NOT NULL,
    valid_from TIMESTAMP(3) NOT NULL,
    valid_until TIMESTAMP(3) NULL,
    operator_id BIGINT NULL,
    reason VARCHAR(255) NOT NULL,
    source_ip VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    PRIMARY KEY(history_id),
    KEY idx_display_history_value_time(new_display_id,valid_from,valid_until),
    KEY idx_display_history_owner_time(account_id,valid_from),
    CONSTRAINT fk_display_history_owner FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
);

CREATE TABLE IF NOT EXISTS aoo_identity_admin_request (
    request_id VARCHAR(128) NOT NULL,
    result_json TEXT NOT NULL,
    completed_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY(request_id)
);

CREATE TABLE IF NOT EXISTS aoo_device_pin (
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    public_key_pem TEXT NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    frozen_at TIMESTAMP(3) NULL,
    credential_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY(account_id,device_id),
    CONSTRAINT fk_device_pin_account FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
);

CREATE TABLE IF NOT EXISTS aoo_device_challenge (
    challenge_id CHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    challenge_hash CHAR(64) NOT NULL,
    request_sequence BIGINT NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    consumed_at TIMESTAMP(3) NULL,
    source_ip VARCHAR(64) NOT NULL,
    PRIMARY KEY(challenge_id),
    UNIQUE KEY uk_device_challenge_sequence(account_id,device_id,request_sequence),
    CONSTRAINT fk_device_challenge_account FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
);

CREATE TABLE IF NOT EXISTS aoo_identity_migration (
    migration_id CHAR(36) NOT NULL,
    source_account_id BIGINT NOT NULL,
    target_account_id BIGINT NOT NULL,
    identity_type VARCHAR(24) NOT NULL,
    value_hash CHAR(64) NOT NULL,
    source_proof_hash CHAR(64) NOT NULL,
    target_proof_hash CHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY(migration_id),
    CONSTRAINT ck_identity_migration_accounts CHECK(source_account_id <> target_account_id)
);

-- Existing login names become aliases. The account table column remains temporarily only
-- so a rolling deployment can backfill before the destructive cleanup migration.
INSERT IGNORE INTO aoo_account_identity(identity_type,normalized_value,value_hash,account_id,verified,status,created_at,updated_at)
SELECT 'ALIAS',LOWER(TRIM(login_name)),SHA2(LOWER(TRIM(login_name)),256),account_id,TRUE,'ACTIVE',created_at,updated_at
FROM aoo_account WHERE login_name IS NOT NULL;

INSERT IGNORE INTO admin_role_permission(role_code,permission_code,granted_at)
SELECT role_code,'account.identity.read',CURRENT_TIMESTAMP(3) FROM admin_role WHERE role_code='super-admin';
INSERT IGNORE INTO admin_role_permission(role_code,permission_code,granted_at)
SELECT role_code,'account.identity.mutate',CURRENT_TIMESTAMP(3) FROM admin_role WHERE role_code='super-admin';
INSERT IGNORE INTO admin_role_permission(role_code,permission_code,granted_at)
SELECT role_code,'account.identity.migrate',CURRENT_TIMESTAMP(3) FROM admin_role WHERE role_code='super-admin';
