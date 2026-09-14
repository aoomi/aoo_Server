-- The 2026090710 LS201 publication was once executed by a mysql client whose
-- session character set was not UTF-8.  Preserve the authoritative JSON and
-- repair that single encoding boundary into a new immutable generation.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS ls201_utf8_publish;
CREATE TEMPORARY TABLE ls201_utf8_publish AS
SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,
       a.release_id old_release_id,i.component_chain,i.bundle_hash,
       CASE
         WHEN JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.fields[0].label'))='人数'
           THEN i.ui_schema
         ELSE CAST(CONVERT(CAST(CONVERT(i.ui_schema USING latin1) AS BINARY) USING utf8mb4) AS JSON)
       END corrected_schema
  FROM aoo_compiled_index_active a
  JOIN aoo_compiled_room_create_index i
    ON i.game_id=a.game_id AND i.region_code=a.region_code
   AND i.play_version=a.play_version AND i.index_generation=a.index_generation
 WHERE a.game_id=90005 AND a.play_version='xqp-equivalent-1';

DELIMITER //
DROP PROCEDURE IF EXISTS assert_ls201_utf8_publication//
CREATE PROCEDURE assert_ls201_utf8_publication()
BEGIN
  DECLARE publication_count INT DEFAULT 0;
  DECLARE player_label VARCHAR(64);
  DECLARE deal_label VARCHAR(64);
  DECLARE deal_option_label VARCHAR(128);
  DECLARE dealer_label VARCHAR(64);
  DECLARE play_label VARCHAR(64);
  SELECT COUNT(*),MAX(JSON_UNQUOTE(JSON_EXTRACT(corrected_schema,'$.fields[0].label'))),
         MAX(JSON_UNQUOTE(JSON_EXTRACT(corrected_schema,'$.fields[3].label'))),
         MAX(JSON_UNQUOTE(JSON_EXTRACT(corrected_schema,'$.fields[3].options[0].label'))),
         MAX(JSON_UNQUOTE(JSON_EXTRACT(corrected_schema,'$.fields[5].label'))),
         MAX(JSON_UNQUOTE(JSON_EXTRACT(corrected_schema,'$.fields[6].label')))
    INTO publication_count,player_label,deal_label,deal_option_label,dealer_label,play_label
    FROM ls201_utf8_publish;
  IF publication_count<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LS201 active publication must be unique';
  END IF;
  IF player_label<>'人数' OR deal_label<>'发牌张数'
     OR deal_option_label<>'8张(7-A，无2/王)'
     OR dealer_label<>'抢庄' OR play_label<>'玩法' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LS201 UTF-8 publication repair did not recover authoritative labels';
  END IF;
END//
DELIMITER ;

CALL assert_ls201_utf8_publication();
DROP PROCEDURE assert_ls201_utf8_publication;

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 900052026090711,p.game_id,p.play_version,2026090711,r.release_scope,r.catalog_snapshot,
 p.corrected_schema,p.corrected_schema,r.component_snapshot,r.catalog_hash,
 SHA2(CONCAT('90005|2026090711|',CAST(p.corrected_schema AS CHAR CHARACTER SET utf8mb4)),256),
 SHA2(CAST(p.corrected_schema AS CHAR CHARACTER SET utf8mb4),256),r.component_hash,
 SHA2('90005|2026090711|pdk-common-room|utf8',256),
 'ACTIVE',100,r.created_by,'Repair LS201 UTF-8 publication boundary',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
  FROM ls201_utf8_publish p JOIN aoo_game_release r ON r.release_id=p.old_release_id;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 900052026090711,region_code,100,'ACTIVE' FROM ls201_utf8_publish;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT game_id,region_code,play_version,2026090711,900052026090711,component_chain,
 corrected_schema,corrected_schema,
 SHA2(CONCAT('90005|2026090711|',CAST(corrected_schema AS CHAR CHARACTER SET utf8mb4)),256),
 SHA2('90005|2026090711|pdk-common-room|utf8',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
  FROM ls201_utf8_publish;

UPDATE aoo_compiled_room_create_index i JOIN ls201_utf8_publish p
 ON p.game_id=i.game_id AND p.region_code=i.region_code AND p.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.index_generation=p.old_generation AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release r JOIN ls201_utf8_publish p ON p.old_release_id=r.release_id
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3));

UPDATE aoo_game_release_region rr JOIN ls201_utf8_publish p ON p.old_release_id=rr.release_id
SET rr.status='RETIRED' WHERE rr.status='ACTIVE';

UPDATE aoo_compiled_index_active a JOIN ls201_utf8_publish p
 ON p.game_id=a.game_id AND p.region_code=a.region_code AND p.play_version=a.play_version
SET a.index_generation=2026090711,a.release_id=900052026090711,a.cache_epoch=a.cache_epoch+1,
    a.activated_by=1,a.activation_reason='Repair LS201 UTF-8 publication boundary',
    a.activated_at=CURRENT_TIMESTAMP(3);

UPDATE aoo_published_game_configuration
SET release_id=900052026090711,reason='Repair LS201 UTF-8 publication boundary',
    created_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005 AND play_version='xqp-equivalent-1';

DROP TEMPORARY TABLE ls201_utf8_publish;
COMMIT;
