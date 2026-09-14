-- Exact asset/scoring dimensions. Monetary assets remain scaled BIGINT minor
-- units; gameplay scores use bounded DECIMAL and never FLOAT/DOUBLE.

ALTER TABLE aoo_ledger
    ADD COLUMN currency_scope_id BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER currency,
    ADD KEY idx_ledger_player_scope_time (player_id,currency,currency_scope_id,created_at,ledger_id),
    ADD CONSTRAINT fk_ledger_currency_catalog FOREIGN KEY (currency)
        REFERENCES aoo_currency_catalog(currency_code) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_ledger_currency_scope CHECK (
        (currency IN ('ROOM_CARD','GOLD','CRYSTAL') AND currency_scope_id=0)
        OR (currency IN ('CITY_ROOM_CARD','SPORTS_POINT') AND currency_scope_id>0)
    );

CREATE TABLE aoo_settlement_score (
    settlement_id BIGINT UNSIGNED NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    score_scale TINYINT UNSIGNED NOT NULL DEFAULT 0,
    score_delta DECIMAL(38,9) NOT NULL,
    score_after DECIMAL(38,9) NULL,
    multiplier DECIMAL(20,9) NOT NULL DEFAULT 1.000000000,
    score_reason VARCHAR(64) NOT NULL,
    calculation_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (settlement_id,player_id),
    KEY idx_settlement_score_player (player_id,created_at,settlement_id),
    CONSTRAINT fk_settlement_score_settlement FOREIGN KEY (settlement_id)
        REFERENCES aoo_settlement(settlement_id) ON DELETE RESTRICT,
    CONSTRAINT chk_settlement_score_scale CHECK (score_scale<=9),
    CONSTRAINT chk_settlement_score_multiplier CHECK (multiplier>=0 AND multiplier<=99999999999.999999999),
    CONSTRAINT chk_settlement_score_hash CHECK (calculation_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_table_governance(
    table_name,owner_code,data_classification,authoritative_source,
    hot_retention_days,archive_retention_days,purge_after_days,
    legal_hold_supported,anonymization_strategy,backup_purge_sla_days
) VALUES(
    'aoo_settlement_score','GAME_RUNTIME','CONFIDENTIAL','AOO_DB',180,730,2555,
    1,'PLAYER_ID_TOKENIZE',35
);
