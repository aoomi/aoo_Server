CREATE TABLE aoo_sequence (
 sequence_name VARCHAR(64) PRIMARY KEY,
 next_value BIGINT NOT NULL
) ENGINE=InnoDB;
INSERT INTO aoo_sequence(sequence_name,next_value) VALUES ('aoo_match_seq',100000),('aoo_room_seq',900000);

CREATE TABLE aoo_match_queue (
 player_id BIGINT NOT NULL, game_id BIGINT NOT NULL, region_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL, enqueued_at TIMESTAMP(3) NOT NULL, expires_at TIMESTAMP(3) NOT NULL, match_id BIGINT NULL, updated_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(player_id,game_id), KEY idx_match_queue_candidate(game_id,region_code,play_version,state,enqueued_at,player_id), KEY idx_match_queue_expiry(state,expires_at),
 CONSTRAINT chk_match_queue_state CHECK(state IN ('WAITING','MATCHED','COMPLETED','CANCELLED','TIMED_OUT'))
) ENGINE=InnoDB;
CREATE TABLE aoo_match (
 match_id BIGINT PRIMARY KEY, game_id BIGINT NOT NULL, room_id BIGINT NOT NULL UNIQUE, room_route VARCHAR(1024) NOT NULL, state VARCHAR(24) NOT NULL, confirm_deadline TIMESTAMP(3) NOT NULL, created_at TIMESTAMP(3) NOT NULL,
 KEY idx_match_confirm_expiry(state,confirm_deadline), CONSTRAINT chk_match_state CHECK(state IN ('PENDING_CONFIRM','CONFIRMED','EXPIRED'))
) ENGINE=InnoDB;
CREATE TABLE aoo_match_member (
 match_id BIGINT NOT NULL, player_id BIGINT NOT NULL, confirmed_at TIMESTAMP(3) NULL, PRIMARY KEY(match_id,player_id),
 CONSTRAINT fk_match_member_match FOREIGN KEY(match_id) REFERENCES aoo_match(match_id)
) ENGINE=InnoDB;
CREATE TABLE aoo_tournament (
 tournament_id BIGINT PRIMARY KEY, game_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, phase VARCHAR(24) NOT NULL, updated_at TIMESTAMP(3) NOT NULL,
 CONSTRAINT chk_tournament_phase CHECK(phase IN ('REGISTRATION','GROUPED','RUNNING','COMPLETED','CANCELLED'))
) ENGINE=InnoDB;
CREATE TABLE aoo_tournament_entry (
 tournament_id BIGINT NOT NULL, player_id BIGINT NOT NULL, status VARCHAR(24) NOT NULL, registered_at TIMESTAMP(3) NOT NULL, group_no INT NULL, seed_no INT NULL, score BIGINT NOT NULL DEFAULT 0, rank_no INT NULL,
 PRIMARY KEY(tournament_id,player_id), UNIQUE KEY uk_tournament_rank(tournament_id,rank_no), KEY idx_tournament_group(tournament_id,group_no,seed_no),
 CONSTRAINT fk_tournament_entry FOREIGN KEY(tournament_id) REFERENCES aoo_tournament(tournament_id), CONSTRAINT chk_tournament_entry_status CHECK(status IN ('REGISTERED','WITHDRAWN','ADVANCED'))
) ENGINE=InnoDB;
CREATE TABLE aoo_competition_idempotency (
 player_id BIGINT NOT NULL,idempotency_key VARCHAR(128) NOT NULL,operation VARCHAR(24) NOT NULL,response_json JSON NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(player_id,idempotency_key)
) ENGINE=InnoDB;
CREATE TABLE aoo_competition_event (
 event_id BIGINT NOT NULL AUTO_INCREMENT, player_id BIGINT NOT NULL, event_type VARCHAR(48) NOT NULL,
 payload_json JSON NOT NULL, created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(event_id), KEY idx_competition_event_player(player_id,event_id)
) ENGINE=InnoDB;
