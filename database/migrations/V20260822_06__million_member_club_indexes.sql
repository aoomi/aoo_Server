-- Legacy club tables are absent in a fresh Aoo deployment. Add each index only
-- when the migrated legacy table exists, so this migration is safe for both a
-- clean install and an in-place upgrade.
DELIMITER $$
CREATE PROCEDURE aoo_add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_columns_value VARCHAR(512)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = table_name_value
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @aoo_index_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` ADD INDEX `', REPLACE(index_name_value, '`', '``'),
            '` (', index_columns_value, ') ALGORITHM=INPLACE, LOCK=NONE'
        );
        PREPARE aoo_index_statement FROM @aoo_index_sql;
        EXECUTE aoo_index_statement;
        DEALLOCATE PREPARE aoo_index_statement;
    END IF;
END$$
DELIMITER ;

CALL aoo_add_index_if_missing('dbClubmember', 'idx_club_member_player', '`playerID`, `status`, `clubID`, `id`');
CALL aoo_add_index_if_missing('dbClubmember', 'idx_club_member_cursor', '`clubID`, `status`, `id`');
CALL aoo_add_index_if_missing('dbClubmember', 'idx_club_member_minister', '`clubID`, `isminister`, `status`, `id`');
CALL aoo_add_index_if_missing('dbClubmember', 'idx_club_member_partner', '`clubID`, `partnerPid`, `status`, `id`');
CALL aoo_add_index_if_missing('dbClubmember', 'idx_club_member_level', '`clubID`, `upLevelId`, `status`, `id`');

DROP PROCEDURE aoo_add_index_if_missing;
