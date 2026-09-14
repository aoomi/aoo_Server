ALTER TABLE aoo_business_idempotency
    ADD COLUMN user_id VARCHAR(128) NULL AFTER request_hash,
    ADD COLUMN operation VARCHAR(128) NULL AFTER user_id,
    ADD COLUMN room_id BIGINT NOT NULL DEFAULT 0 AFTER operation,
    ADD COLUMN round_no INT NOT NULL DEFAULT 0 AFTER room_id,
    ADD COLUMN client_request_id VARCHAR(128) NULL AFTER round_no,
    ADD KEY idx_idempotency_business_scope (user_id,operation,room_id,round_no,client_request_id);

UPDATE aoo_business_idempotency
SET user_id='migrated',operation='migrated.unknown',client_request_id=request_id
WHERE user_id IS NULL;
