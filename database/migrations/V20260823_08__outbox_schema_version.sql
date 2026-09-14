ALTER TABLE aoo_outbox
    ADD COLUMN schema_version INT NOT NULL DEFAULT 1 AFTER event_type,
    ADD KEY idx_outbox_event_schema (event_type,schema_version,created_at);
