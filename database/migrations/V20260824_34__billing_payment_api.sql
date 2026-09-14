CREATE TABLE aoo_payment_product (
    product_code VARCHAR(64) NOT NULL,
    channel_code VARCHAR(32) NOT NULL,
    product_version BIGINT UNSIGNED NOT NULL,
    asset_currency VARCHAR(32) NOT NULL,
    asset_units BIGINT UNSIGNED NOT NULL,
    fiat_currency CHAR(3) NOT NULL,
    amount_minor BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    valid_from DATETIME(3) NOT NULL,
    valid_until DATETIME(3) NULL,
    PRIMARY KEY(product_code,channel_code,product_version),
    KEY idx_payment_product_active(product_code,channel_code,status,valid_from,valid_until),
    CONSTRAINT fk_payment_product_currency FOREIGN KEY(asset_currency) REFERENCES aoo_currency_catalog(currency_code),
    CONSTRAINT chk_payment_product_amounts CHECK(asset_units>0 AND amount_minor>0),
    CONSTRAINT chk_payment_product_status CHECK(status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- currency_scope_id and its player/scope/time index are introduced by
-- V20260824_07.  Re-adding either here breaks a clean ordered migration.

CREATE TABLE aoo_payment_order (
    order_id VARCHAR(128) PRIMARY KEY,buyer_player_id BIGINT UNSIGNED NOT NULL,asset_currency VARCHAR(32) NOT NULL,
    asset_scope_id BIGINT UNSIGNED NOT NULL DEFAULT 0,product_code VARCHAR(64) NOT NULL,product_version BIGINT UNSIGNED NOT NULL,
    asset_units BIGINT UNSIGNED NOT NULL,fiat_currency CHAR(3) NOT NULL,amount_minor BIGINT UNSIGNED NOT NULL,channel_code VARCHAR(32) NOT NULL,
    product_locked_at DATETIME(3) NOT NULL,product_fingerprint CHAR(64) NOT NULL,state VARCHAR(24) NOT NULL,
    provider_transaction_id VARCHAR(128) NULL,callback_digests MEDIUMTEXT NOT NULL,failure_code VARCHAR(64) NULL,version BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,paid_at DATETIME(3) NULL,delivered_at DATETIME(3) NULL,refunded_at DATETIME(3) NULL,
    UNIQUE KEY uk_payment_provider_transaction(provider_transaction_id),KEY idx_payment_reconcile(channel_code,state,created_at),
    CONSTRAINT chk_payment_order_state CHECK(state IN ('CREATED','PAID','DELIVERED','REFUND_PENDING','REFUNDED','CLOSED','REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_payment_transaction_claim (
    provider_transaction_id VARCHAR(128) PRIMARY KEY,order_id VARCHAR(128) NOT NULL,created_at DATETIME(3) NOT NULL,
    KEY idx_payment_claim_order(order_id),CONSTRAINT fk_payment_claim_order FOREIGN KEY(order_id) REFERENCES aoo_payment_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
