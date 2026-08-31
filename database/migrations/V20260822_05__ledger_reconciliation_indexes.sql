ALTER TABLE aoo_ledger
    ADD INDEX idx_ledger_business_date (created_at, business_id),
    ADD INDEX idx_ledger_balance_chain (player_id, currency, created_at, ledger_id);
