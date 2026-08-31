CREATE TABLE IF NOT EXISTS aoo_room_snapshot (
  room_id BIGINT PRIMARY KEY,
  game_id INT NOT NULL,
  play_version VARCHAR(64) NOT NULL,
  component_version VARCHAR(64) NOT NULL,
  fencing_token BIGINT NOT NULL,
  last_event_sequence BIGINT NOT NULL,
  captured_at DATETIME(3) NOT NULL,
  state_payload JSON NOT NULL,
  KEY idx_room_snapshot_capture (captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_room_lease (
  room_id BIGINT PRIMARY KEY,
  owner_node VARCHAR(128) NOT NULL,
  fencing_token BIGINT NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  KEY idx_room_lease_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_room_event (
  room_id BIGINT NOT NULL,
  event_sequence BIGINT NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  event_payload JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (room_id, event_sequence),
  KEY idx_room_event_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
