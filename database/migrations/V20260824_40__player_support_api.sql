-- Player-owned support/report/appeal/ticket lifecycle and immutable status feed.
CREATE TABLE support_case (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, player_id BIGINT UNSIGNED NOT NULL,
 case_kind ENUM('REPORT','APPEAL','TICKET') NOT NULL, subject VARCHAR(120) NOT NULL,
 description VARCHAR(4000) NOT NULL, status ENUM('OPEN','IN_REVIEW','RESOLVED','REJECTED','WITHDRAWN') NOT NULL,
 resolution_summary VARCHAR(1000) NULL, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
 row_version BIGINT UNSIGNED NOT NULL, PRIMARY KEY(id), KEY idx_support_player_list(player_id,id),
 KEY idx_support_handoff(id,status,updated_at), CONSTRAINT chk_support_version CHECK(row_version>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_case_evidence (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,case_id BIGINT UNSIGNED NOT NULL,
 evidence_type ENUM('REPLAY','ADMIN_CASE','URL') NOT NULL,reference_id VARCHAR(256) NOT NULL,label VARCHAR(80) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uq_support_evidence(case_id,evidence_type,reference_id),
 CONSTRAINT fk_support_evidence_case FOREIGN KEY(case_id) REFERENCES support_case(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_case_event (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,case_id BIGINT UNSIGNED NOT NULL,
 actor_type ENUM('PLAYER','SUPPORT','SYSTEM') NOT NULL,actor_id BIGINT UNSIGNED NOT NULL,event_type VARCHAR(40) NOT NULL,
 case_status ENUM('OPEN','IN_REVIEW','RESOLVED','REJECTED','WITHDRAWN') NOT NULL,created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id),KEY idx_support_event_case(case_id,id),
 CONSTRAINT fk_support_event_case FOREIGN KEY(case_id) REFERENCES support_case(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
