-- Aoo production business closure tables. MySQL 8.0+.
CREATE TABLE IF NOT EXISTS aoo_session (
  session_id VARCHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_id VARCHAR(128) NOT NULL,
  session_version BIGINT NOT NULL,
  refresh_token_hash CHAR(64) NOT NULL,
  issued_at DATETIME(3) NOT NULL,
  access_expires_at DATETIME(3) NOT NULL,
  refresh_expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  revoke_reason VARCHAR(64) NULL,
  UNIQUE KEY uk_session_user_version (user_id, session_version),
  KEY idx_session_user_active (user_id, revoked_at, refresh_expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_business_idempotency (
  request_id VARCHAR(64) PRIMARY KEY,
  request_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  response_payload JSON NULL,
  created_at DATETIME(3) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  KEY idx_idempotency_expiry (expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_ledger (
  ledger_id BIGINT PRIMARY KEY,
  business_id VARCHAR(128) NOT NULL,
  player_id BIGINT NOT NULL,
  currency VARCHAR(32) NOT NULL,
  delta BIGINT NOT NULL,
  balance_after BIGINT NOT NULL,
  reason_code VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_ledger_business (business_id),
  KEY idx_ledger_player_time (player_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_settlement (
  settlement_id BIGINT PRIMARY KEY,
  business_id VARCHAR(128) NOT NULL,
  room_id BIGINT NOT NULL,
  round_no INT NOT NULL,
  settlement_version VARCHAR(64) NOT NULL,
  result_payload JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_settlement_business (business_id),
  KEY idx_settlement_room_round (room_id, round_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_outbox (
  event_id VARCHAR(64) PRIMARY KEY,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  payload JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  published_at DATETIME(3) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  KEY idx_outbox_unpublished (published_at, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_game_profile_version (
  profile_id BIGINT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  profile_version VARCHAR(64) NOT NULL,
  scope_type VARCHAR(16) NOT NULL,
  province_code VARCHAR(16) NULL,
  city_code VARCHAR(16) NULL,
  profile_payload JSON NOT NULL,
  content_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  published_at DATETIME(3) NULL,
  UNIQUE KEY uk_game_profile_version (game_id, profile_version),
  KEY idx_game_profile_scope (scope_type, province_code, city_code, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_appeal (
  appeal_id BIGINT PRIMARY KEY,
  player_id BIGINT NOT NULL,
  club_id BIGINT NULL,
  room_id BIGINT NULL,
  appeal_type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  evidence_snapshot JSON NOT NULL,
  resolution JSON NULL,
  created_at DATETIME(3) NOT NULL,
  resolved_at DATETIME(3) NULL,
  KEY idx_appeal_status_time (status, created_at),
  KEY idx_appeal_player_time (player_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_admin_audit (
  audit_id BIGINT PRIMARY KEY,
  operator_id BIGINT NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_id VARCHAR(64) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  before_value JSON NULL,
  after_value JSON NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_admin_audit_resource (resource_type, resource_id, created_at),
  KEY idx_admin_audit_operator (operator_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_data_lifecycle_policy (
  data_type VARCHAR(32) PRIMARY KEY,
  hot_days INT NOT NULL,
  archive_days INT NOT NULL,
  legal_hold_supported BOOLEAN NOT NULL,
  anonymize_on_delete BOOLEAN NOT NULL,
  updated_at DATETIME(3) NOT NULL
) ENGINE=InnoDB;
