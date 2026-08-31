-- These player tables are legacy deployment variants and may legitimately be
-- absent from a clean Aoo schema.
DELIMITER $$
CREATE PROCEDURE aoo_add_player_index_if_missing(
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
        SET @aoo_player_index_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` ADD INDEX `', REPLACE(index_name_value, '`', '``'),
            '` (', index_columns_value, ') ALGORITHM=INPLACE, LOCK=NONE'
        );
        PREPARE aoo_player_index_statement FROM @aoo_player_index_sql;
        EXECUTE aoo_player_index_statement;
        DEALLOCATE PREPARE aoo_player_index_statement;
    END IF;
END$$
DELIMITER ;

CALL aoo_add_player_index_if_missing('player', 'idx_player_phone', '`phone`');
CALL aoo_add_player_index_if_missing('player', 'idx_player_created_cursor', '`createTime`, `id`');
CALL aoo_add_player_index_if_missing('player', 'idx_player_last_login_cursor', '`lastLogin`, `id`');
CALL aoo_add_player_index_if_missing('db_player', 'idx_db_player_created_cursor', '`createTime`, `id`');
CALL aoo_add_player_index_if_missing('db_player', 'idx_db_player_last_login_cursor', '`lastLogin`, `id`');

DROP PROCEDURE aoo_add_player_index_if_missing;
