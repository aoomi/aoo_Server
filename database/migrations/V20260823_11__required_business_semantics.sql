-- Mandatory domain values must not be represented by NULL, empty text or zero defaults.
-- Existing dirty data is intentionally not guessed here: deployment must repair it explicitly
-- before this migration, otherwise ALTER fails closed.
ALTER TABLE aoo_business_idempotency
  MODIFY request_id VARCHAR(64) NOT NULL,
  MODIFY status VARCHAR(16) NOT NULL,
  MODIFY expires_at DATETIME(3) NOT NULL;

ALTER TABLE aoo_room_snapshot
  MODIFY room_id BIGINT NOT NULL,
  MODIFY state_payload JSON NOT NULL;

ALTER TABLE aoo_room_lease
  MODIFY room_id BIGINT NOT NULL,
  MODIFY owner_node VARCHAR(128) NOT NULL,
  MODIFY expires_at DATETIME(3) NOT NULL;

ALTER TABLE aoo_settlement
  MODIFY business_id VARCHAR(128) NOT NULL,
  MODIFY room_id BIGINT NOT NULL,
  MODIFY result_payload JSON NOT NULL;

ALTER TABLE aoo_ledger
  MODIFY business_id VARCHAR(128) NOT NULL,
  MODIFY player_id BIGINT NOT NULL;

ALTER TABLE aoo_currency_balance
  MODIFY player_id BIGINT NOT NULL,
  MODIFY currency VARCHAR(32) NOT NULL;

ALTER TABLE aoo_club_member
  MODIFY club_id BIGINT UNSIGNED NOT NULL,
  MODIFY player_id BIGINT UNSIGNED NOT NULL,
  MODIFY member_status VARCHAR(16) NOT NULL;
