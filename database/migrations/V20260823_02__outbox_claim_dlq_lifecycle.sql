ALTER TABLE aoo_outbox
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING' AFTER payload,
    ADD COLUMN locked_by VARCHAR(128) NULL AFTER retry_count,
    ADD COLUMN locked_until DATETIME(3) NULL AFTER locked_by,
    ADD COLUMN dead_at DATETIME(3) NULL AFTER locked_until,
    ADD KEY idx_outbox_claim (status, next_attempt_at, locked_until, created_at),
    ADD KEY idx_outbox_cleanup (status, published_at);

UPDATE aoo_outbox SET status='PUBLISHED' WHERE published_at IS NOT NULL;
