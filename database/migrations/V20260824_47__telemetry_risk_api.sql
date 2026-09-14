CREATE TABLE telemetry_signal (
 signal_id BIGINT NOT NULL AUTO_INCREMENT, event_id VARCHAR(64) NOT NULL, player_id BIGINT NOT NULL,
 signal_kind VARCHAR(24) NOT NULL, occurred_at TIMESTAMP(3) NOT NULL, received_at TIMESTAMP(3) NOT NULL,
 session_id VARCHAR(64) NULL, room_id BIGINT NULL, device_hash CHAR(64) NULL,
 network_prefix VARCHAR(64) NULL, geo_cell VARCHAR(32) NULL, attributes_json JSON NOT NULL,
 PRIMARY KEY(signal_id), UNIQUE KEY uk_telemetry_player_event(player_id,event_id),
 KEY idx_telemetry_player_time(player_id,received_at), KEY idx_telemetry_room_device(room_id,device_hash,received_at),
 KEY idx_telemetry_room_geo(room_id,geo_cell,received_at),
 CONSTRAINT chk_telemetry_kind CHECK(signal_kind IN ('PERFORMANCE','CRASH','NETWORK','DEVICE','LOCATION','OPERATION'))
);
CREATE TABLE risk_event (
 risk_event_id BIGINT NOT NULL AUTO_INCREMENT, player_id BIGINT NOT NULL, room_id BIGINT NULL,
 source_signal_id BIGINT NOT NULL, risk_score SMALLINT NOT NULL, reasons_json JSON NOT NULL,
 status VARCHAR(24) NOT NULL, created_at TIMESTAMP(3) NOT NULL, PRIMARY KEY(risk_event_id),
 UNIQUE KEY uk_risk_source(source_signal_id), KEY idx_risk_review(status,created_at),
 CONSTRAINT fk_risk_signal FOREIGN KEY(source_signal_id) REFERENCES telemetry_signal(signal_id),
 CONSTRAINT chk_risk_score CHECK(risk_score BETWEEN 0 AND 100),
 CONSTRAINT chk_risk_status CHECK(status IN ('PENDING_REVIEW','CONFIRMED','DISMISSED'))
);
CREATE TABLE risk_review_directive (
 directive_id BIGINT NOT NULL AUTO_INCREMENT, risk_event_id BIGINT NOT NULL,
 directive_type VARCHAR(32) NOT NULL, status VARCHAR(16) NOT NULL, created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(directive_id), UNIQUE KEY uk_review_risk(risk_event_id), KEY idx_review_queue(status,directive_id),
 CONSTRAINT fk_review_risk FOREIGN KEY(risk_event_id) REFERENCES risk_event(risk_event_id),
 CONSTRAINT chk_review_type CHECK(directive_type IN ('REVIEW_ACCOUNT','REVIEW_TABLE')),
 CONSTRAINT chk_review_status CHECK(status IN ('OPEN','CLAIMED','CLOSED'))
);
