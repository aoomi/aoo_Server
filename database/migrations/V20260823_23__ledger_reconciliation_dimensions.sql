ALTER TABLE aoo_ledger
    ADD COLUMN entry_status VARCHAR(16) NOT NULL DEFAULT 'POSTED' AFTER reason_code,
    ADD COLUMN channel_code VARCHAR(32) NOT NULL DEFAULT 'GAME' AFTER entry_status,
    ADD KEY idx_ledger_reconcile_window (entry_status,channel_code,created_at,ledger_id),
    ADD KEY idx_ledger_status_window (entry_status,created_at,ledger_id),
    ADD KEY idx_ledger_channel_window (channel_code,created_at,ledger_id);
