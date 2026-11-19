-- Publish the LS201 room schema with distinct stable keys for:
--   triple_with_one         = only one single card
--   triple_with_one_or_pair = one single card or one pair
-- The source workbook's visible label remains “三带一”; the published key carries the
-- unambiguous common-rule meaning used by every PDK region.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS ls201_split_triple_rule;
CREATE TEMPORARY TABLE ls201_split_triple_rule AS
SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,
       a.release_id old_release_id,i.component_chain,i.bundle_hash,
       JSON_SET(
         CAST(REPLACE(REPLACE(CAST(i.ui_schema AS CHAR CHARACTER SET utf8mb4),
              '"triple_with_one"','"triple_with_one_or_pair"'),
              '"four_with_two"','"four_with_two_or_pairs"') AS JSON),
         '$.fields[6].options[1].label','三带一(单张或一对)',
         '$.fields[6].options[2].label','四带二(两张单牌或两对)') corrected_schema
  FROM aoo_compiled_index_active a
  JOIN aoo_compiled_room_create_index i
    ON i.game_id=a.game_id AND i.region_code=a.region_code
   AND i.play_version=a.play_version AND i.index_generation=a.index_generation
 WHERE a.game_id=90005 AND a.play_version='xqp-equivalent-1';

DELIMITER //
DROP PROCEDURE IF EXISTS assert_ls201_split_triple_rule//
CREATE PROCEDURE assert_ls201_split_triple_rule()
BEGIN
  DECLARE publication_count INT DEFAULT 0;
  DECLARE option_key VARCHAR(64);
  DECLARE four_option_key VARCHAR(64);
  SELECT COUNT(*),MAX(JSON_UNQUOTE(JSON_EXTRACT(
           corrected_schema,'$.fields[6].options[1].value'))),
         MAX(JSON_UNQUOTE(JSON_EXTRACT(
           corrected_schema,'$.fields[6].options[2].value')))
    INTO publication_count,option_key,four_option_key FROM ls201_split_triple_rule;
  IF publication_count<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LS201 active publication must be unique';
  END IF;
  IF option_key<>'triple_with_one_or_pair' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LS201 triple attachment key was not split';
  END IF;
  IF four_option_key<>'four_with_two_or_pairs' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LS201 four attachment key was not split';
  END IF;
END//
DELIMITER ;

CALL assert_ls201_split_triple_rule();
DROP PROCEDURE assert_ls201_split_triple_rule;

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 900052026091801,p.game_id,p.play_version,2026091801,r.release_scope,r.catalog_snapshot,
 p.corrected_schema,p.corrected_schema,r.component_snapshot,r.catalog_hash,
 SHA2(CONCAT('90005|2026091801|',CAST(p.corrected_schema AS CHAR CHARACTER SET utf8mb4)),256),
 SHA2(CAST(p.corrected_schema AS CHAR CHARACTER SET utf8mb4),256),r.component_hash,
 SHA2('90005|2026091801|pdk-common-room|triple-rule-split',256),
 'ACTIVE',100,r.created_by,'Split common PDK triple attachment rule keys',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
  FROM ls201_split_triple_rule p JOIN aoo_game_release r ON r.release_id=p.old_release_id;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 900052026091801,region_code,100,'ACTIVE' FROM ls201_split_triple_rule;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT game_id,region_code,play_version,2026091801,900052026091801,component_chain,
 corrected_schema,corrected_schema,
 SHA2(CONCAT('90005|2026091801|',CAST(corrected_schema AS CHAR CHARACTER SET utf8mb4)),256),
 SHA2('90005|2026091801|pdk-common-room|triple-rule-split',256),'ACTIVE',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
  FROM ls201_split_triple_rule;

UPDATE aoo_compiled_room_create_index i JOIN ls201_split_triple_rule p
 ON p.game_id=i.game_id AND p.region_code=i.region_code AND p.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.index_generation=p.old_generation AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release r JOIN ls201_split_triple_rule p ON p.old_release_id=r.release_id
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3));

UPDATE aoo_game_release_region rr JOIN ls201_split_triple_rule p
 ON p.old_release_id=rr.release_id SET rr.status='RETIRED' WHERE rr.status='ACTIVE';

UPDATE aoo_compiled_index_active a JOIN ls201_split_triple_rule p
 ON p.game_id=a.game_id AND p.region_code=a.region_code AND p.play_version=a.play_version
SET a.index_generation=2026091801,a.release_id=900052026091801,
    a.cache_epoch=a.cache_epoch+1,a.activated_by=1,
    a.activation_reason='Split common PDK triple attachment rule keys',
    a.activated_at=CURRENT_TIMESTAMP(3);

UPDATE aoo_published_game_configuration
SET release_id=900052026091801,reason='Split common PDK triple attachment rule keys',
    created_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005 AND play_version='xqp-equivalent-1';

UPDATE aoo_play_version SET rule_schema_version=4,ui_schema_version=4,
 row_version=row_version+1 WHERE game_id=90005 AND play_version='xqp-equivalent-1';

DROP TEMPORARY TABLE ls201_split_triple_rule;
COMMIT;
