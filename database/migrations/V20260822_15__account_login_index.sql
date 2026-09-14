-- Login resolves the player by account_id. This index is mandatory before the
-- account population reaches the multi-million range.
DELIMITER $$
CREATE PROCEDURE aoo_add_account_login_index()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'db_player'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'db_player'
          AND index_name = 'idx_db_player_account'
    ) THEN
        ALTER TABLE db_player ADD INDEX idx_db_player_account (account_id),
            ALGORITHM=INPLACE, LOCK=NONE;
    END IF;
END$$
DELIMITER ;
CALL aoo_add_account_login_index();
DROP PROCEDURE aoo_add_account_login_index;
