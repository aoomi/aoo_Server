-- Restore the current immutable Hall room-create confirmation receipt contract.
-- This is forward-only and intentionally performs no historical backfill.
CREATE TABLE aoo_room_create_confirmed_receipt (
 event_id CHAR(64) NOT NULL PRIMARY KEY,
 source_request_id VARCHAR(128) NOT NULL,
 request_hash VARCHAR(128) NOT NULL,
 creator_id BIGINT UNSIGNED NOT NULL,
 room_id BIGINT UNSIGNED NOT NULL,
 game_id BIGINT UNSIGNED NOT NULL,
 play_version VARCHAR(64) NOT NULL,
 occurred_at DATETIME(3) NOT NULL,
 schema_version INT NOT NULL,
 UNIQUE KEY uk_room_created_request(source_request_id),
 UNIQUE KEY uk_room_created_room(room_id),
 KEY idx_room_created_time(occurred_at,creator_id,game_id),
 CONSTRAINT ck_room_created_schema CHECK(schema_version=1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TRIGGER trg_room_created_no_update BEFORE UPDATE ON aoo_room_create_confirmed_receipt
 FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room creation receipt is immutable';

CREATE TRIGGER trg_room_created_no_delete BEFORE DELETE ON aoo_room_create_confirmed_receipt
 FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room creation receipt is retained';
