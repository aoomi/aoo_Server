-- Durable Hall catalog, room, route, idempotency and single-use ticket authority.
CREATE TABLE aoo_game_service_route (
 route_id BIGINT UNSIGNED NOT NULL, game_id BIGINT UNSIGNED NOT NULL, play_version VARCHAR(64) NOT NULL,
 endpoint VARCHAR(512) NOT NULL, priority SMALLINT UNSIGNED NOT NULL DEFAULT 100, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(route_id), UNIQUE KEY uk_game_route_endpoint(game_id,play_version,endpoint), KEY idx_game_route_lookup(game_id,play_version,status,priority,route_id),
 CONSTRAINT fk_game_route_play FOREIGN KEY(game_id,play_version) REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
 CONSTRAINT chk_game_route_status CHECK(status IN('ACTIVE','DRAINING','OFFLINE')), CONSTRAINT chk_game_route_endpoint CHECK(endpoint REGEXP '^wss?://')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aoo_hall_room (
 room_id BIGINT UNSIGNED NOT NULL, game_id BIGINT UNSIGNED NOT NULL, region_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL, release_id BIGINT UNSIGNED NOT NULL, index_generation BIGINT UNSIGNED NOT NULL,
 owner_account_id BIGINT UNSIGNED NOT NULL, state VARCHAR(16) NOT NULL, route_endpoint VARCHAR(512) NOT NULL, rules_json JSON NOT NULL, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(room_id), KEY idx_hall_room_owner(owner_account_id,state,updated_at,room_id), KEY idx_hall_room_route(game_id,play_version,state,room_id),
 CONSTRAINT fk_hall_room_index FOREIGN KEY(game_id,region_code,play_version,index_generation) REFERENCES aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation) ON DELETE RESTRICT,
 CONSTRAINT fk_hall_room_release FOREIGN KEY(release_id) REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT, CONSTRAINT chk_hall_room_state CHECK(state IN('OPEN','PLAYING','FINISHED','DISSOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aoo_hall_room_member (
 room_id BIGINT UNSIGNED NOT NULL, account_id BIGINT UNSIGNED NOT NULL, seat_no SMALLINT UNSIGNED NOT NULL, status VARCHAR(16) NOT NULL, joined_at DATETIME(3) NOT NULL, left_at DATETIME(3) NULL,
 PRIMARY KEY(room_id,account_id), UNIQUE KEY uk_hall_room_seat(room_id,seat_no), KEY idx_hall_member_account(account_id,status,room_id),
 CONSTRAINT fk_hall_member_room FOREIGN KEY(room_id) REFERENCES aoo_hall_room(room_id) ON DELETE RESTRICT, CONSTRAINT chk_hall_member_status CHECK(status IN('JOINED','LEFT','KICKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aoo_hall_game_ticket (
 ticket_hash CHAR(64) NOT NULL, room_id BIGINT UNSIGNED NOT NULL, account_id BIGINT UNSIGNED NOT NULL, device_fingerprint VARCHAR(128) NOT NULL, route_endpoint VARCHAR(512) NOT NULL, expires_at DATETIME(3) NOT NULL, consumed_at DATETIME(3) NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(ticket_hash), KEY idx_hall_ticket_expiry(expires_at,consumed_at), KEY idx_hall_ticket_subject(room_id,account_id,expires_at), CONSTRAINT fk_hall_ticket_member FOREIGN KEY(room_id,account_id) REFERENCES aoo_hall_room_member(room_id,account_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aoo_hall_idempotency (
 account_id BIGINT UNSIGNED NOT NULL, idempotency_key VARCHAR(128) NOT NULL, operation VARCHAR(16) NOT NULL, response_json JSON NOT NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(account_id,idempotency_key), KEY idx_hall_idempotency_retention(created_at), CONSTRAINT chk_hall_idempotency_operation CHECK(operation IN('CREATE','JOIN','LEAVE','TICKET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
