ALTER TABLE aoo_outbox
    ADD COLUMN next_attempt_at DATETIME(3) NULL,
    ADD COLUMN last_error VARCHAR(500) NULL,
    ADD KEY idx_outbox_retry (published_at, next_attempt_at, created_at);
