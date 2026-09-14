-- Region is gameplay catalog classification only. Preserve a time-bounded audit snapshot,
-- then remove identity/asset partition columns from player, club, union and family tables.
CREATE TABLE IF NOT EXISTS aoo_region_identity_legacy_snapshot (
  entity_type VARCHAR(32) NOT NULL,
  entity_id BIGINT NOT NULL,
  legacy_value VARCHAR(500) NOT NULL,
  captured_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  quarantine_owner VARCHAR(64) NOT NULL DEFAULT 'data-governance',
  delete_after DATE NOT NULL DEFAULT '2027-08-25',
  PRIMARY KEY (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @has_player_city = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='player' AND column_name='cityId');
SET @sql = IF(@has_player_city > 0,
  'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''PLAYER'',id,CAST(cityId AS CHAR) FROM player WHERE cityId<>0 ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_player_city > 0, 'ALTER TABLE player DROP COLUMN cityId', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_club_city = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='clubList' AND column_name='cityId');
SET @sql = IF(@has_club_city > 0,
  'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''CLUB'',id,CAST(cityId AS CHAR) FROM clubList WHERE cityId<>0 ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_club_city > 0, 'ALTER TABLE clubList DROP COLUMN cityId', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_union_city = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='union' AND column_name='cityId');
SET @sql = IF(@has_union_city > 0,
  'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''UNION'',id,CAST(cityId AS CHAR) FROM `union` WHERE cityId<>0 ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_union_city > 0, 'ALTER TABLE `union` DROP COLUMN cityId', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_family_city = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='family' AND column_name='cityId');
SET @has_family_city_list = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='family' AND column_name='cityIdList');
SET @sql = IF(@has_family_city > 0 AND @has_family_city_list > 0,
  'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''FAMILY'',id,CONCAT(''cityId='',cityId,'';cityIdList='',cityIdList) FROM family WHERE cityId<>0 OR cityIdList<>'''' ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
  IF(@has_family_city > 0,
    'INSERT INTO aoo_region_identity_legacy_snapshot(entity_type,entity_id,legacy_value) SELECT ''FAMILY'',id,CAST(cityId AS CHAR) FROM family WHERE cityId<>0 ON DUPLICATE KEY UPDATE legacy_value=VALUES(legacy_value),captured_at=CURRENT_TIMESTAMP(3)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_family_city > 0, 'ALTER TABLE family DROP COLUMN cityId', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_family_city_list > 0, 'ALTER TABLE family DROP COLUMN cityIdList', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Transaction history keeps classification provenance under an explicit non-authoritative name.
SET @old_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='playerrecharge' AND column_name='cityId');
SET @new_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='playerrecharge' AND column_name='classificationRegionId');
SET @sql = IF(@old_column > 0 AND @new_column = 0,
  'ALTER TABLE playerrecharge CHANGE COLUMN cityId classificationRegionId INT NOT NULL DEFAULT 0 COMMENT ''Gameplay classification statistic only; never asset or route partition''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rebate' AND column_name='cityId');
SET @new_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rebate' AND column_name='classificationRegionId');
SET @sql = IF(@old_column > 0 AND @new_column = 0,
  'ALTER TABLE rebate CHANGE COLUMN cityId classificationRegionId INT NOT NULL DEFAULT 0 COMMENT ''Gameplay classification statistic only; never asset or route partition''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zlerecharge' AND column_name='cityId');
SET @new_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zlerecharge' AND column_name='classificationRegionId');
SET @sql = IF(@old_column > 0 AND @new_column = 0,
  'ALTER TABLE zlerecharge CHANGE COLUMN cityId classificationRegionId INT NOT NULL DEFAULT 0 COMMENT ''Gameplay classification statistic only; never asset or route partition''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- The retired per-city gift table cannot remain runtime reachable.
SET @city_give_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='cityGive');
SET @city_give_quarantine_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='quarantine_city_give_legacy');
SET @sql = IF(@city_give_exists > 0 AND @city_give_quarantine_exists = 0,
  'RENAME TABLE cityGive TO quarantine_city_give_legacy', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO aoo_region_semantics_policy(policy_key,policy_value) VALUES
 ('LEGACY_IDENTITY_SNAPSHOT_OWNER','data-governance'),
 ('LEGACY_IDENTITY_SNAPSHOT_DELETE_AFTER','2027-08-25'),
 ('LEGACY_CITY_GIFT_RUNTIME','FORBIDDEN'),
 ('LEGACY_ROOM_COST_REGION_CONFIG_RUNTIME','FORBIDDEN'),
 ('LEGACY_ROOM_COST_REGION_CONFIG_OWNER','data-governance'),
 ('LEGACY_ROOM_COST_REGION_CONFIG_DELETE_AFTER','2027-08-25')
ON DUPLICATE KEY UPDATE policy_value=VALUES(policy_value),updated_at=CURRENT_TIMESTAMP(3);
