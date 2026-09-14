-- City login/game bans are retired. Gameplay availability is controlled by published catalog classification.
SET @legacy_ban_city_exists = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'banCity'
);
SET @legacy_ban_city_quarantine_exists = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'quarantine_ban_city_legacy'
);
SET @legacy_ban_city_sql = IF(@legacy_ban_city_exists > 0 AND @legacy_ban_city_quarantine_exists = 0,
    'RENAME TABLE banCity TO quarantine_ban_city_legacy', 'SELECT 1');
PREPARE legacy_ban_city_quarantine FROM @legacy_ban_city_sql;
EXECUTE legacy_ban_city_quarantine;
DEALLOCATE PREPARE legacy_ban_city_quarantine;
