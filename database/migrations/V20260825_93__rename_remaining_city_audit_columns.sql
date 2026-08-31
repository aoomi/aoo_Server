-- Remaining regional values are classification/audit provenance only.
-- Identity, asset and route partitions were removed by V81-V92.
DELIMITER $$
DROP PROCEDURE IF EXISTS aoo_rename_city_audit_columns$$
CREATE PROCEDURE aoo_rename_city_audit_columns()
BEGIN
  DECLARE finished INTEGER DEFAULT 0;
  DECLARE source_table VARCHAR(128);
  DECLARE target_exists INTEGER DEFAULT 0;
  DECLARE city_columns CURSOR FOR
    SELECT table_name
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND column_name = 'cityId'
       AND table_name NOT LIKE 'quarantine\\_%';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

  OPEN city_columns;
  migrate_columns: LOOP
    FETCH city_columns INTO source_table;
    IF finished = 1 THEN
      LEAVE migrate_columns;
    END IF;

    SELECT COUNT(*) INTO target_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = source_table
       AND column_name = 'classificationRegionId';

    IF target_exists = 0 THEN
      SET @rename_sql = CONCAT(
        'ALTER TABLE `', REPLACE(source_table, '`', '``'),
        '` RENAME COLUMN `cityId` TO `classificationRegionId`');
      PREPARE rename_stmt FROM @rename_sql;
      EXECUTE rename_stmt;
      DEALLOCATE PREPARE rename_stmt;
    ELSE
      SET @merge_sql = CONCAT(
        'UPDATE `', REPLACE(source_table, '`', '``'),
        '` SET `classificationRegionId`=`cityId` ',
        'WHERE `classificationRegionId`=0 AND `cityId`<>0');
      PREPARE merge_stmt FROM @merge_sql;
      EXECUTE merge_stmt;
      DEALLOCATE PREPARE merge_stmt;
      SET @drop_sql = CONCAT(
        'ALTER TABLE `', REPLACE(source_table, '`', '``'),
        '` DROP COLUMN `cityId`');
      PREPARE drop_stmt FROM @drop_sql;
      EXECUTE drop_stmt;
      DEALLOCATE PREPARE drop_stmt;
    END IF;
  END LOOP;
  CLOSE city_columns;
END$$
CALL aoo_rename_city_audit_columns()$$
DROP PROCEDURE aoo_rename_city_audit_columns$$
DELIMITER ;

INSERT INTO aoo_region_semantics_policy(policy_key,policy_value) VALUES
 ('LEGACY_CITY_ID_COLUMN_RUNTIME','FORBIDDEN'),
 ('CLASSIFICATION_REGION_AUDIT_ONLY','TRUE')
ON DUPLICATE KEY UPDATE policy_value=VALUES(policy_value),updated_at=CURRENT_TIMESTAMP(3);
