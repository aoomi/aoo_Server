CREATE TABLE IF NOT EXISTS admin_operator_account (
    operator_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    credential_verifier VARCHAR(512) NOT NULL COMMENT 'PBKDF2-SHA256 iterations:base64-salt:base64-digest',
    enabled TINYINT NOT NULL DEFAULT 1,
    password_changed_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_admin_operator_username(username),
    CONSTRAINT chk_admin_operator_enabled CHECK(enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_browser_session (
    session_hash CHAR(64) NOT NULL PRIMARY KEY,
    operator_id BIGINT UNSIGNED NOT NULL,
    csrf_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    last_seen_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    KEY idx_admin_session_operator(operator_id,expires_at),
    KEY idx_admin_session_expiry(expires_at),
    CONSTRAINT fk_admin_session_operator FOREIGN KEY(operator_id) REFERENCES admin_operator_account(operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
