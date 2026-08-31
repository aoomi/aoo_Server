CREATE TABLE activity_version (
 activity_code VARCHAR(64) NOT NULL, version BIGINT NOT NULL, title VARCHAR(128) NOT NULL,
 starts_at_ms BIGINT NOT NULL, ends_at_ms BIGINT NOT NULL, time_zone VARCHAR(64) NOT NULL,
 published BOOLEAN NOT NULL DEFAULT FALSE, created_at_ms BIGINT NOT NULL,
 PRIMARY KEY(activity_code,version), CHECK(ends_at_ms>starts_at_ms)
);
CREATE INDEX idx_activity_version_window ON activity_version(published,starts_at_ms,ends_at_ms);
CREATE TABLE activity_mission_version (
 activity_code VARCHAR(64) NOT NULL, activity_version BIGINT NOT NULL, mission_code VARCHAR(64) NOT NULL,
 event_type VARCHAR(64) NOT NULL, target_value BIGINT NOT NULL, reward_currency VARCHAR(32) NOT NULL, reward_amount BIGINT NOT NULL,
 PRIMARY KEY(activity_code,activity_version,mission_code),
 CONSTRAINT fk_activity_mission_version FOREIGN KEY(activity_code,activity_version) REFERENCES activity_version(activity_code,version),
 CHECK(target_value>0),CHECK(reward_amount>0)
);
CREATE TABLE activity_check_in (
 player_id BIGINT NOT NULL, activity_code VARCHAR(64) NOT NULL, activity_version BIGINT NOT NULL,
 business_date DATE NOT NULL, streak INT NOT NULL, created_at_ms BIGINT NOT NULL,
 PRIMARY KEY(player_id,activity_code,business_date),
 CONSTRAINT fk_activity_checkin_version FOREIGN KEY(activity_code,activity_version) REFERENCES activity_version(activity_code,version),CHECK(streak>0)
);
CREATE TABLE activity_progress_event (
 event_id VARCHAR(128) PRIMARY KEY,player_id BIGINT NOT NULL,activity_code VARCHAR(64) NOT NULL,activity_version BIGINT NOT NULL,
 mission_code VARCHAR(64) NOT NULL,delta_value BIGINT NOT NULL,created_at_ms BIGINT NOT NULL,CHECK(delta_value>0),
 CONSTRAINT fk_activity_progress_event_mission FOREIGN KEY(activity_code,activity_version,mission_code) REFERENCES activity_mission_version(activity_code,activity_version,mission_code)
);
CREATE TABLE activity_mission_progress (
 player_id BIGINT NOT NULL,activity_code VARCHAR(64) NOT NULL,activity_version BIGINT NOT NULL,mission_code VARCHAR(64) NOT NULL,progress_value BIGINT NOT NULL,
 PRIMARY KEY(player_id,activity_code,activity_version,mission_code),CHECK(progress_value>=0),
 CONSTRAINT fk_activity_progress_mission FOREIGN KEY(activity_code,activity_version,mission_code) REFERENCES activity_mission_version(activity_code,activity_version,mission_code)
);
CREATE TABLE activity_reward_claim (
 claim_key VARCHAR(191) PRIMARY KEY,player_id BIGINT NOT NULL,activity_code VARCHAR(64) NOT NULL,activity_version BIGINT NOT NULL,
 claim_type VARCHAR(16) NOT NULL,subject_code VARCHAR(64) NOT NULL,reward_currency VARCHAR(32) NOT NULL,reward_amount BIGINT NOT NULL,
 state VARCHAR(16) NOT NULL,failure_code VARCHAR(64),created_at_ms BIGINT NOT NULL,updated_at_ms BIGINT NOT NULL,
 CONSTRAINT fk_activity_claim_version FOREIGN KEY(activity_code,activity_version) REFERENCES activity_version(activity_code,version),CHECK(reward_amount>0)
);
CREATE INDEX idx_activity_claim_player ON activity_reward_claim(player_id,activity_code,activity_version,state);
