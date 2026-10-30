CREATE TABLE IF NOT EXISTS aoo_room_create_saga (
 request_id VARCHAR(128) PRIMARY KEY, request_hash VARCHAR(64) NOT NULL, account_id BIGINT NOT NULL,
 room_id BIGINT NOT NULL UNIQUE, game_id INT NOT NULL, play_version VARCHAR(64) NOT NULL,
 state_version BIGINT NOT NULL, step VARCHAR(32) NOT NULL, response_json JSON NULL, failure_code VARCHAR(128) NULL,
 created_at TIMESTAMP(3) NOT NULL, updated_at TIMESTAMP(3) NOT NULL,
 INDEX idx_room_create_saga_recovery(step,updated_at)
);
