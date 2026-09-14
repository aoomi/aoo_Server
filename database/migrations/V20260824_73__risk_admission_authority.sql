CREATE TABLE risk_admission_decision (
 request_id VARCHAR(128) NOT NULL, action_name VARCHAR(24) NOT NULL, player_id BIGINT NOT NULL,
 device_hash CHAR(64) NULL, room_id BIGINT NULL, decision_code VARCHAR(16) NOT NULL,
 risk_score SMALLINT NOT NULL, reason_codes VARCHAR(255) NOT NULL, decided_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(request_id,action_name), KEY idx_admission_player_time(player_id,action_name,decided_at),
 KEY idx_admission_room_time(room_id,decided_at),
 CONSTRAINT chk_admission_action CHECK(action_name IN ('LOGIN','CREATE_ROOM','JOIN_ROOM')),
 CONSTRAINT chk_admission_decision CHECK(decision_code IN ('ALLOW','REJECT')),
 CONSTRAINT chk_admission_score CHECK(risk_score BETWEEN 0 AND 100)
) ENGINE=InnoDB;
