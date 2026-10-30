-- Forward-only proposal. No current database execution is authorized by this file.
CREATE TABLE aoo_room_carry_batch (
 batch_hash CHAR(64) PRIMARY KEY,
 batch_json TEXT NOT NULL,
 status VARCHAR(16) NOT NULL,
 proof_json TEXT NULL,
 created_at TIMESTAMP(3) NOT NULL,
 CONSTRAINT ck_carry_batch_status CHECK (status IN ('STAGED','FINALIZED'))
);
CREATE TABLE aoo_room_carry_attempt (
 attempt_id CHAR(64) PRIMARY KEY,
 attempt_json TEXT NOT NULL,
 stage VARCHAR(24) NULL,
 last_ordinal BIGINT NOT NULL,
 state_version BIGINT NOT NULL,
 original_json TEXT NULL,
 original_request_id VARCHAR(128) NULL UNIQUE,
 money_mode VARCHAR(16) NOT NULL,
 updated_at TIMESTAMP(3) NOT NULL,
 CONSTRAINT ck_carry_money_mode CHECK (money_mode IN ('UNDECIDED','BOUND','FACT_ONLY'))
);
CREATE TABLE aoo_room_carry_effect (
 effect_id CHAR(64) PRIMARY KEY,
 attempt_id CHAR(64) NOT NULL,
 batch_hash CHAR(64) NOT NULL,
 instance_id VARCHAR(128) NOT NULL,
 epoch BIGINT NOT NULL,
 native_ordinal BIGINT NOT NULL,
 effect_json TEXT NOT NULL,
 CONSTRAINT uq_carry_runtime_ordinal UNIQUE(instance_id,epoch,native_ordinal),
 CONSTRAINT fk_carry_effect_attempt FOREIGN KEY(attempt_id) REFERENCES aoo_room_carry_attempt(attempt_id),
 CONSTRAINT fk_carry_effect_batch FOREIGN KEY(batch_hash) REFERENCES aoo_room_carry_batch(batch_hash)
);
