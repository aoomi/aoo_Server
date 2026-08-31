ALTER TABLE aoo_consumed_event
    ADD COLUMN claim_token CHAR(36) NULL AFTER claimed_at,
    ADD KEY idx_consumed_event_cleanup (consumer_name,status,processed_at,event_id);
