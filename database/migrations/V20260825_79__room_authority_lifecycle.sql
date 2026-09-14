CREATE TABLE IF NOT EXISTS aoo_room_authority_route (
 room_id BIGINT UNSIGNED NOT NULL, game_id BIGINT UNSIGNED NOT NULL, play_version VARCHAR(64) NOT NULL,
 node_id VARCHAR(128) NOT NULL, authority_endpoint VARCHAR(512) NOT NULL, fencing_token BIGINT UNSIGNED NOT NULL,
 lifecycle_state VARCHAR(24) NOT NULL, last_request_id VARCHAR(128) NOT NULL, trace_id VARCHAR(64) NOT NULL,
 lease_expires_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(room_id), UNIQUE KEY uk_authority_create_request(last_request_id), KEY idx_authority_node_state(node_id,lifecycle_state,lease_expires_at),
 CONSTRAINT chk_authority_fencing CHECK(fencing_token > 0), CONSTRAINT chk_authority_state CHECK(lifecycle_state IN('CREATING','ACTIVE','RECOVERING','REMOVING','REMOVED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS aoo_room_billing_reservation (
 request_id VARCHAR(128) NOT NULL, account_id BIGINT UNSIGNED NOT NULL, room_id BIGINT UNSIGNED NOT NULL,
 currency_code VARCHAR(32) NOT NULL, amount BIGINT UNSIGNED NOT NULL, state VARCHAR(16) NOT NULL, failure_code VARCHAR(64) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(request_id), UNIQUE KEY uk_room_billing_room(room_id), KEY idx_room_billing_account(account_id,state,updated_at),
 CONSTRAINT chk_room_billing_state CHECK(state IN('RESERVED','CONFIRMED','RELEASED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS aoo_room_create_saga (
 request_id VARCHAR(128) NOT NULL, request_hash VARCHAR(128) NOT NULL, account_id BIGINT UNSIGNED NOT NULL,
 room_id BIGINT UNSIGNED NOT NULL, game_id BIGINT UNSIGNED NOT NULL, play_version VARCHAR(64) NOT NULL,
 state_version BIGINT UNSIGNED NOT NULL, step VARCHAR(32) NOT NULL, response_json JSON NULL, failure_code VARCHAR(128) NULL,
 attempt_count INT UNSIGNED NOT NULL DEFAULT 0, next_retry_at DATETIME(3) NULL, trace_id VARCHAR(64) NOT NULL DEFAULT 'migration',
 created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(request_id), UNIQUE KEY uk_room_create_saga_room(room_id), KEY idx_room_saga_recovery(step,next_retry_at,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
