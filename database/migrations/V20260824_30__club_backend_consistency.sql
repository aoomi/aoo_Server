CREATE TABLE IF NOT EXISTS aoo_club_write_idempotency (
  scope_key VARCHAR(160) PRIMARY KEY,
  response_json TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_club_state (
  club_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  state_json JSON NOT NULL,
  row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT chk_club_state_revision CHECK (row_version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_club_template_index (
  club_id BIGINT UNSIGNED NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  sort_index INT UNSIGNED NOT NULL,
  PRIMARY KEY (club_id, template_code),
  UNIQUE KEY uk_club_template_sort (club_id, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_club_ledger (
  club_id BIGINT UNSIGNED NOT NULL,
  business_key VARCHAR(160) NOT NULL,
  player_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(20,4) NOT NULL,
  reason VARCHAR(160) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (club_id, business_key),
  KEY ix_club_ledger_player (club_id, player_id, created_at),
  CONSTRAINT chk_club_ledger_nonzero CHECK (amount <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_club_room_projection (
  club_id BIGINT UNSIGNED NOT NULL,
  room_id VARCHAR(64) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  room_status VARCHAR(16) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (club_id, room_id),
  KEY ix_club_room_display (club_id, room_status, created_at),
  CONSTRAINT chk_club_room_status CHECK (room_status IN ('OPEN','PLAYING','FINISHED','DISSOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
