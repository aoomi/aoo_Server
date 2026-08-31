SET @has_table := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='db_player');
SET @has_old := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='db_player' AND index_name='idx_db_player_account');
SET @has_unique := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='db_player' AND index_name='uk_db_player_account' AND non_unique=0);
SET @sql := IF(@has_table=0 OR @has_unique>0, 'SELECT 1', IF(@has_old>0, 'ALTER TABLE db_player DROP INDEX idx_db_player_account, ADD UNIQUE KEY uk_db_player_account (account_id)', 'ALTER TABLE db_player ADD UNIQUE KEY uk_db_player_account (account_id)'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
