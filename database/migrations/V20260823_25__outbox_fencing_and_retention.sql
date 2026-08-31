ALTER TABLE aoo_outbox
    ADD COLUMN claim_token CHAR(36) NULL AFTER locked_by,
    ADD KEY idx_outbox_dead_cleanup (status, dead_at);
