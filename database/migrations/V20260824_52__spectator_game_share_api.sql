-- Durable spectator authorization and opaque room-share authority.
CREATE TABLE aoo_spectator_admission (
 request_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, room_id BIGINT UNSIGNED NOT NULL, spectator_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(16) NOT NULL, requested_at DATETIME(3) NOT NULL, authorized_by BIGINT UNSIGNED NULL, authorized_at DATETIME(3) NULL, left_at DATETIME(3) NULL, row_version BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY(request_id), KEY idx_spectator_active(room_id,status,request_id), KEY idx_spectator_subject(spectator_id,status,room_id),
 CONSTRAINT fk_spectator_room FOREIGN KEY(room_id) REFERENCES aoo_hall_room(room_id) ON DELETE RESTRICT,
 CONSTRAINT chk_spectator_status CHECK(status IN('PENDING','AUTHORIZED','REJECTED','LEFT')),
 CONSTRAINT chk_spectator_decision CHECK((status='PENDING' AND authorized_by IS NULL AND authorized_at IS NULL AND left_at IS NULL) OR (status IN('AUTHORIZED','REJECTED') AND authorized_by IS NOT NULL AND authorized_at IS NOT NULL AND left_at IS NULL) OR (status='LEFT' AND authorized_by IS NOT NULL AND authorized_at IS NOT NULL AND left_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE UNIQUE INDEX uk_spectator_one_open ON aoo_spectator_admission(room_id,spectator_id,(CASE WHEN status IN('PENDING','AUTHORIZED') THEN 1 ELSE NULL END));
CREATE TABLE aoo_game_share (
 share_hash CHAR(64) NOT NULL, room_id BIGINT UNSIGNED NOT NULL, created_by BIGINT UNSIGNED NOT NULL, created_at DATETIME(3) NOT NULL, expires_at DATETIME(3) NOT NULL, revoked_at DATETIME(3) NULL,
 PRIMARY KEY(share_hash), KEY idx_game_share_expiry(expires_at,revoked_at), KEY idx_game_share_room(room_id,created_at),
 CONSTRAINT fk_game_share_room FOREIGN KEY(room_id) REFERENCES aoo_hall_room(room_id) ON DELETE RESTRICT,
 CONSTRAINT chk_game_share_expiry CHECK(expires_at>created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
