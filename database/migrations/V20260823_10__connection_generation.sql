CREATE TABLE IF NOT EXISTS aoo_connection_generation (
  user_id VARCHAR(128) NOT NULL,
  room_id VARCHAR(64) NOT NULL,
  seat_id INT NOT NULL,
  generation BIGINT NOT NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, room_id, seat_id),
  CONSTRAINT chk_connection_generation_positive CHECK (generation > 0)
) ENGINE=InnoDB;
