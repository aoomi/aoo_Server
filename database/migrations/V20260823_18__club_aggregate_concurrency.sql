CREATE TABLE IF NOT EXISTS aoo_club_guard (
  club_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE aoo_club_member ADD COLUMN row_version BIGINT UNSIGNED NOT NULL DEFAULT 1;
ALTER TABLE aoo_room_template ADD COLUMN row_version BIGINT UNSIGNED NOT NULL DEFAULT 1;
ALTER TABLE aoo_club_member DROP CHECK chk_club_member_status,
  ADD CONSTRAINT chk_club_member_status CHECK (member_status IN ('ACTIVE','SUSPENDED','LEFT') AND online IN (0,1)),
  ADD CONSTRAINT chk_club_member_revision CHECK (row_version > 0);
ALTER TABLE aoo_room_template ADD CONSTRAINT chk_room_template_revision CHECK (row_version > 0);
