-- r2-only minimal region seed extracted from V20260824_77; original migration untouched.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE aoo_r2_seed_chengdu_region_v20261018_03()
BEGIN
  DECLARE n INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  SELECT COUNT(*) INTO n FROM aoo_region WHERE region_code='CN-51-01';
  IF n>0 AND NOT EXISTS(SELECT 1 FROM aoo_region WHERE region_code='CN-51-01' AND parent_region_code='CN-51' AND region_type='CITY' AND country_code='CN' AND subdivision_code='5101' AND display_name='成都' AND path='/CN/CN-51/CN-51-01' AND depth=2) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_CHENGDU_REGION_CONFLICT';
  END IF;
  SELECT COUNT(*) INTO n FROM aoo_region_alias WHERE source_system='bootstrap-catalog' AND source_region_code='cd';
  IF n>0 AND NOT EXISTS(SELECT 1 FROM aoo_region_alias WHERE source_system='bootstrap-catalog' AND source_region_code='cd' AND region_code='CN-51-01' AND mapping_status='VERIFIED') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_CHENGDU_REGION_ALIAS_CONFLICT';
  END IF;
  START TRANSACTION;
  INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth)
  VALUES('CN-51-01','CN-51','CITY','CN','5101','成都','/CN/CN-51/CN-51-01',2)
  ON DUPLICATE KEY UPDATE region_code=VALUES(region_code);
  INSERT INTO aoo_region_alias(source_system,source_region_code,region_code,mapping_status,verified_at)
  VALUES('bootstrap-catalog','cd','CN-51-01','VERIFIED',CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE region_code=VALUES(region_code),mapping_status=VALUES(mapping_status),verified_at=VALUES(verified_at);
  COMMIT;
END$$
CALL aoo_r2_seed_chengdu_region_v20261018_03()$$
DROP PROCEDURE aoo_r2_seed_chengdu_region_v20261018_03$$
DELIMITER ;
