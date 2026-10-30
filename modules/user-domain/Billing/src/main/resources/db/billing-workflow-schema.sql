-- Billing-owned DDL reference. Promotion into the root migration chain is intentionally a separate boundary-owned step.
CREATE TABLE IF NOT EXISTS aoo_payment_order (
  order_id VARCHAR(128) PRIMARY KEY,
  buyer_player_id BIGINT UNSIGNED NOT NULL,
  asset_currency VARCHAR(32) NOT NULL,
  asset_scope_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  product_code VARCHAR(64) NOT NULL,
  product_version BIGINT UNSIGNED NOT NULL,
  asset_units BIGINT UNSIGNED NOT NULL,
  fiat_currency CHAR(3) NOT NULL,
  amount_minor BIGINT UNSIGNED NOT NULL,
  channel_code VARCHAR(32) NOT NULL,
  product_locked_at DATETIME(3) NOT NULL,
  product_fingerprint CHAR(64) NOT NULL,
  state VARCHAR(24) NOT NULL,
  provider_transaction_id VARCHAR(128) NULL,
  callback_digests MEDIUMTEXT NOT NULL,
  failure_code VARCHAR(64) NULL,
  version BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL,
  paid_at DATETIME(3) NULL,
  delivered_at DATETIME(3) NULL,
  refunded_at DATETIME(3) NULL,
  UNIQUE KEY uk_payment_provider_transaction (provider_transaction_id),
  KEY idx_payment_reconcile (channel_code,state,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_payment_transaction_claim (
  provider_transaction_id VARCHAR(128) PRIMARY KEY,
  order_id VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_payment_claim_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_asset_grant (
  business_id VARCHAR(128) PRIMARY KEY,
  kind VARCHAR(24) NOT NULL,
  source_player_id BIGINT UNSIGNED NULL,
  source_currency VARCHAR(32) NULL,
  source_scope_id BIGINT UNSIGNED NULL,
  target_player_id BIGINT UNSIGNED NOT NULL,
  target_currency VARCHAR(32) NOT NULL,
  target_scope_id BIGINT UNSIGNED NOT NULL,
  tax_player_id BIGINT UNSIGNED NULL,
  tax_currency VARCHAR(32) NULL,
  tax_scope_id BIGINT UNSIGNED NULL,
  gross_amount BIGINT UNSIGNED NOT NULL,
  tax_amount BIGINT UNSIGNED NOT NULL,
  net_amount BIGINT UNSIGNED NOT NULL,
  source_code VARCHAR(64) NOT NULL,
  business_date DATE NOT NULL,
  risk_decision VARCHAR(16) NOT NULL,
  state VARCHAR(24) NOT NULL,
  failure_code VARCHAR(64) NULL,
  version BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL,
  completed_at DATETIME(3) NULL,
  KEY idx_asset_grant_daily (source_code,business_date,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_asset_grant_limit_bucket (
  limit_key VARCHAR(256) NOT NULL,
  business_date DATE NOT NULL,
  used_amount BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (limit_key,business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_asset_grant_limit_reservation (
  limit_key VARCHAR(256) NOT NULL,
  business_date DATE NOT NULL,
  business_id VARCHAR(128) NOT NULL,
  amount BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (limit_key,business_date,business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
