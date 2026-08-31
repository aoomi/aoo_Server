CREATE TABLE IF NOT EXISTS admin_security_nonce (
    nonce_namespace VARCHAR(32) NOT NULL,
    nonce_value VARCHAR(128) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    consumed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (nonce_namespace, nonce_value),
    KEY idx_admin_security_nonce_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
