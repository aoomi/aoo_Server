-- Region is a gameplay catalog classification only. Player identity/profile APIs must not persist it.
-- The bounded legacy snapshot is owned by V20260825_92; migrations have one DDL owner per table.

SET @has_profile_region = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='player_profile' AND column_name='region_code');
SET @sql = IF(@has_profile_region > 0,
  'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''PLAYER_PROFILE'',player_id,region_code FROM player_profile WHERE region_code<>'''' ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@has_profile_region > 0, 'ALTER TABLE player_profile DROP COLUMN region_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_profile_region_visibility = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='player_profile' AND column_name='show_region');
SET @sql = IF(@has_profile_region_visibility > 0, 'ALTER TABLE player_profile DROP COLUMN show_region', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO aoo_region_semantics_policy(policy_key,policy_value) VALUES
 ('PLAYER_PROFILE_REGION_RUNTIME','FORBIDDEN'),
 ('PLAYER_PROFILE_REGION_QUARANTINE_OWNER','data-governance'),
 ('PLAYER_PROFILE_REGION_DELETE_AFTER','2027-08-25')
ON DUPLICATE KEY UPDATE policy_value=VALUES(policy_value),updated_at=CURRENT_TIMESTAMP(3);
