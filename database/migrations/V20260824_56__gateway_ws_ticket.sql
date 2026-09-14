CREATE TABLE IF NOT EXISTS gateway_ws_ticket (
    ticket_hash CHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    session_id CHAR(36) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    allowed_origin VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    issued_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (ticket_hash),
    UNIQUE KEY uk_gateway_ws_ticket_request (account_id, idempotency_key),
    KEY idx_gateway_ws_ticket_expiry (expires_at),
    CONSTRAINT fk_gateway_ws_ticket_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id),
    CONSTRAINT fk_gateway_ws_ticket_session FOREIGN KEY (session_id) REFERENCES aoo_account_session(session_id)
);
