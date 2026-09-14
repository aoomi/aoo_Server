CREATE TABLE gift_policy (
 asset_code VARCHAR(64) NOT NULL, per_gift_limit BIGINT NOT NULL, daily_sender_limit BIGINT NOT NULL,
 min_account_age_hours BIGINT NOT NULL DEFAULT 24, enabled BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(asset_code), CONSTRAINT ck_gift_policy_limits CHECK(per_gift_limit>0 AND daily_sender_limit>=per_gift_limit)
);
CREATE TABLE gift_transfer (
 gift_id CHAR(36) NOT NULL,idempotency_key VARCHAR(128) NOT NULL,request_hash CHAR(64) NOT NULL,
 sender_id BIGINT NOT NULL,recipient_id BIGINT NOT NULL,asset_code VARCHAR(64) NOT NULL,quantity BIGINT NOT NULL,
 state VARCHAR(24) NOT NULL,risk_code VARCHAR(64) NOT NULL,risk_score SMALLINT NOT NULL,failure_code VARCHAR(64) NULL,
 device_id VARCHAR(128) NOT NULL,ip_address VARCHAR(64) NOT NULL,business_date DATE NOT NULL,created_at TIMESTAMP(3) NOT NULL,updated_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(gift_id),UNIQUE KEY uk_gift_idempotency(idempotency_key),KEY idx_gift_sender_daily(sender_id,asset_code,business_date,state),KEY idx_gift_recipient(recipient_id,created_at),
 CONSTRAINT fk_gift_sender FOREIGN KEY(sender_id) REFERENCES aoo_account(account_id),CONSTRAINT fk_gift_recipient FOREIGN KEY(recipient_id) REFERENCES aoo_account(account_id),
 CONSTRAINT ck_gift_quantity CHECK(quantity>0),CONSTRAINT ck_gift_distinct CHECK(sender_id<>recipient_id),CONSTRAINT ck_gift_state CHECK(state IN('RESERVED','DEBITED','COMPLETED','FAILED','COMPENSATED','COMPENSATION_PENDING'))
);
CREATE TABLE gift_ledger (
 ledger_id BIGINT NOT NULL AUTO_INCREMENT,gift_id CHAR(36) NOT NULL,player_id BIGINT NOT NULL,counterparty_id BIGINT NOT NULL,direction VARCHAR(3) NOT NULL,asset_code VARCHAR(64) NOT NULL,quantity BIGINT NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(ledger_id),UNIQUE KEY uk_gift_ledger_side(gift_id,player_id,direction),KEY idx_gift_ledger_player(player_id,created_at),CONSTRAINT fk_gift_ledger_transfer FOREIGN KEY(gift_id) REFERENCES gift_transfer(gift_id),CONSTRAINT ck_gift_direction CHECK(direction IN('IN','OUT'))
);
CREATE TABLE gift_audit (
 sequence_no BIGINT NOT NULL AUTO_INCREMENT,gift_id CHAR(36) NOT NULL,event_type VARCHAR(32) NOT NULL,actor_id BIGINT NOT NULL,payload_json JSON NOT NULL,previous_hash CHAR(64) NOT NULL,event_hash CHAR(64) NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(sequence_no),UNIQUE KEY uk_gift_audit_hash(event_hash),KEY idx_gift_audit_chain(gift_id,sequence_no),CONSTRAINT fk_gift_audit_transfer FOREIGN KEY(gift_id) REFERENCES gift_transfer(gift_id)
);
