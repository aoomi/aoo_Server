CREATE TABLE aoo_currency_catalog (
    currency_code VARCHAR(32) PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    scale_digits TINYINT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_currency_catalog_scope CHECK (scope_type IN ('GLOBAL','CITY','CLUB','UNION')),
    CONSTRAINT chk_currency_catalog_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT chk_currency_catalog_scale CHECK (scale_digits <= 6)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_currency_catalog(currency_code,display_name,scope_type,scale_digits,status) VALUES
('ROOM_CARD','公共房卡','GLOBAL',0,'ACTIVE'),
('CITY_ROOM_CARD','城市房卡','CITY',0,'ACTIVE'),
('GOLD','乐豆','GLOBAL',0,'ACTIVE'),
('CRYSTAL','水晶','GLOBAL',0,'ACTIVE'),
('SPORTS_POINT','赛事积分','UNION',2,'ACTIVE');

ALTER TABLE aoo_currency_balance
    ADD COLUMN currency_scope_id BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER currency,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (player_id,currency,currency_scope_id),
    ADD KEY idx_currency_balance_catalog (currency,player_id,currency_scope_id),
    ADD CONSTRAINT fk_currency_balance_catalog FOREIGN KEY (currency)
        REFERENCES aoo_currency_catalog(currency_code),
    ADD CONSTRAINT chk_currency_balance_scope CHECK (
        (currency IN ('ROOM_CARD','GOLD','CRYSTAL') AND currency_scope_id=0)
        OR (currency IN ('CITY_ROOM_CARD','SPORTS_POINT') AND currency_scope_id>0)
    );

