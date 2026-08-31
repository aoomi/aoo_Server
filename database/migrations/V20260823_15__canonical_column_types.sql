-- Canonical identity type: positive numeric identities use BIGINT UNSIGNED everywhere.
ALTER TABLE aoo_room_snapshot MODIFY room_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_room_lease MODIFY room_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_room_event MODIFY room_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_settlement MODIFY room_id BIGINT UNSIGNED NOT NULL, MODIFY settlement_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_ledger MODIFY ledger_id BIGINT UNSIGNED NOT NULL, MODIFY player_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_currency_balance MODIFY player_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE aoo_connection_generation MODIFY room_id BIGINT UNSIGNED NOT NULL;

-- Canonical text type and collation for indexed identifiers and business codes.
ALTER TABLE aoo_business_idempotency CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_room_event CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_room_snapshot CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_room_lease CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_settlement CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_ledger CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_currency_balance CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_club_member CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_play_variant CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_room_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE aoo_connection_generation CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
