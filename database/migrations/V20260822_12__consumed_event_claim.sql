ALTER TABLE aoo_consumed_event
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PROCESSED' AFTER event_id,
    ADD COLUMN claimed_at DATETIME(3) NULL AFTER status,
    MODIFY COLUMN processed_at DATETIME(3) NULL,
    ADD KEY idx_consumed_event_claim (status, claimed_at);
