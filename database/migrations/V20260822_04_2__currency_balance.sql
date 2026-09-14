CREATE TABLE IF NOT EXISTS aoo_currency_balance (
    player_id BIGINT NOT NULL,
    currency VARCHAR(32) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (player_id, currency),
    CONSTRAINT chk_currency_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
