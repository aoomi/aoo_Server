-- Region-partitioned balances are merged exactly once into an account-wide wallet.
CREATE TABLE IF NOT EXISTS playerWallet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pid BIGINT NOT NULL,
    value INT NOT NULL DEFAULT 0,
    time INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_player_wallet_pid (pid),
    CONSTRAINT chk_player_wallet_value CHECK (value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='account-wide room-card wallet';

SET @legacy_wallet_exists = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'playerCityCurrency'
);
SET @legacy_wallet_merge_sql = IF(@legacy_wallet_exists > 0,
    'INSERT INTO playerWallet(pid,value,time) SELECT pid,LEAST(1999999999,SUM(GREATEST(value,0))),MAX(time) FROM playerCityCurrency GROUP BY pid ON DUPLICATE KEY UPDATE value=GREATEST(playerWallet.value,VALUES(value)),time=GREATEST(playerWallet.time,VALUES(time))',
    'SELECT 1');
PREPARE legacy_wallet_merge FROM @legacy_wallet_merge_sql;
EXECUTE legacy_wallet_merge;
DEALLOCATE PREPARE legacy_wallet_merge;

SET @legacy_wallet_quarantine_exists = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'quarantine_player_city_currency_legacy'
);
SET @legacy_wallet_quarantine_sql = IF(@legacy_wallet_exists > 0 AND @legacy_wallet_quarantine_exists = 0,
    'RENAME TABLE playerCityCurrency TO quarantine_player_city_currency_legacy', 'SELECT 1');
PREPARE legacy_wallet_quarantine FROM @legacy_wallet_quarantine_sql;
EXECUTE legacy_wallet_quarantine;
DEALLOCATE PREPARE legacy_wallet_quarantine;

SET @legacy_wallet_comment_sql = IF(@legacy_wallet_exists > 0 OR @legacy_wallet_quarantine_exists > 0,
    'ALTER TABLE quarantine_player_city_currency_legacy COMMENT=''QUARANTINED owner=data-governance delete-after=2027-08-25 runtime-access=forbidden''',
    'SELECT 1');
PREPARE legacy_wallet_comment FROM @legacy_wallet_comment_sql;
EXECUTE legacy_wallet_comment;
DEALLOCATE PREPARE legacy_wallet_comment;
