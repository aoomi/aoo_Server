-- Repair the active publication metadata contract without modifying historical
-- release/index content. Every current identity receives a fresh immutable
-- release and compiled-index generation; only the active pointer moves.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE repair_active_release_snapshot_hashes_v20261008_03()
BEGIN
  DECLARE expected_identity_count INT;
  DECLARE active_row_count INT;
  DECLARE distinct_identity_count INT;
  DECLARE repaired_row_count INT;

  SET expected_identity_count=32;
  SET active_row_count=0;
  SET distinct_identity_count=0;
  SET repaired_row_count=0;

  START TRANSACTION;

  CREATE TEMPORARY TABLE v246_current_identity (
    game_id BIGINT UNSIGNED NOT NULL,
    game_code VARCHAR(64) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    PRIMARY KEY (game_id,play_version),
    UNIQUE KEY uk_v246_current_identity_code (game_code,play_version)
  ) ENGINE=MEMORY;

  INSERT INTO v246_current_identity(game_id,game_code,play_version) VALUES
    (5,'quanguo2998','cn298-v1.0.0'),
    (9,'quanguo2997','cn297-v1.0.0'),
    (630,'chengdu2999','cd299-v1.0.0'),
    (1001,'chengdu1001','chengdu1001-v1.0.0'),
    (2001,'chengdu2001','chengdu2001-v1.0.0'),
    (2002,'neijiang2002','neijiang2002-v1.0.0'),
    (2003,'liangshan2003','liangshan2003-v1.0.0'),
    (2004,'mianning2004','mianning2004-v1.0.0'),
    (2005,'anyue2005','anyue2005-v1.0.0'),
    (2006,'panzhihua2006','panzhihua2006-v1.0.0'),
    (2007,'wannian2007','wannian2007-v1.0.0'),
    (2008,'leping2008','leping2008-v1.0.0'),
    (2009,'bozhou2009','bozhou2009-v1.0.0'),
    (2010,'yugan2010','yugan2010-v1.0.0'),
    (2011,'jian2011','jian2011-v1.0.0'),
    (2012,'gushi2012','gushi2012-v1.0.0'),
    (2013,'xiaogan2013','xiaogan2013-v1.0.0'),
    (2014,'ningdu2014','ningdu2014-v1.0.0'),
    (2015,'huaihua2015','huaihua2015-v1.0.0'),
    (2016,'xinxian2016','xinxian2016-v1.0.0'),
    (2017,'jingdezhen2017','jingdezhen2017-v1.0.0'),
    (2018,'jiangsu2018','jiangsu2018-v1.0.0'),
    (2019,'henan2019','henan2019-v1.0.0'),
    (2020,'taizhou2020','taizhou2020-v1.0.0'),
    (2021,'huaibin2021','huaibin2021-v1.0.0'),
    (2022,'huangchuan2022','huangchuan2022-v1.0.0'),
    (2023,'poyang2023','poyang2023-v1.0.0'),
    (2024,'jintan2024','jintan2024-v1.0.0'),
    (2025,'gaoan2025','gaoan2025-v1.0.0'),
    (2026,'yixing2026','yixing2026-v1.0.0'),
    (2027,'xuancheng2027','xuancheng2027-v1.0.0'),
    (2028,'fuzhou2028','fuzhou2028-v1.0.0');

  CREATE TEMPORARY TABLE v246_active_preimage AS
  SELECT identity_row.game_id,identity_row.game_code,identity_row.play_version,
         active.region_code,active.index_generation old_generation,active.release_id old_release_id,
         release_row.release_scope,release_row.snapshot_schema_version,release_row.card_codec_version,
         release_row.event_interpreter_version,release_row.protocol_version,release_row.source_hash,
         release_row.catalog_snapshot,release_row.rule_snapshot,release_row.ui_snapshot,
         release_row.component_snapshot,release_row.ui_hash,
         compiled.component_chain,compiled.rule_validator,compiled.ui_schema
    FROM v246_current_identity identity_row
    JOIN aoo_game_catalog catalog
      ON catalog.game_id=identity_row.game_id AND catalog.game_code=identity_row.game_code
    JOIN aoo_compiled_index_active active
      ON active.game_id=identity_row.game_id AND active.play_version=identity_row.play_version
    JOIN aoo_compiled_room_create_index compiled
      ON compiled.game_id=active.game_id AND compiled.region_code=active.region_code
     AND compiled.play_version=active.play_version AND compiled.index_generation=active.index_generation
    JOIN aoo_game_release release_row ON release_row.release_id=active.release_id
    JOIN aoo_game_release_region release_region
      ON release_region.release_id=release_row.release_id AND release_region.region_code=active.region_code
   WHERE release_row.status='ACTIVE' AND release_region.status='ACTIVE'
     AND compiled.lifecycle_state='ACTIVE';

  SELECT COUNT(*),COUNT(DISTINCT CONCAT(game_id,'|',play_version))
    INTO active_row_count,distinct_identity_count
    FROM v246_active_preimage;
  IF active_row_count<>expected_identity_count OR distinct_identity_count<>expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v2.46 requires exactly one active release/region/index for every current identity';
  END IF;

  IF EXISTS (
    SELECT 1 FROM v246_active_preimage
     WHERE JSON_TYPE(catalog_snapshot)<>'OBJECT' OR JSON_TYPE(rule_validator)<>'OBJECT'
        OR JSON_TYPE(ui_snapshot)<>'OBJECT' OR JSON_TYPE(ui_schema)<>'OBJECT'
        OR JSON_TYPE(component_snapshot)<>'OBJECT' OR JSON_TYPE(component_chain)<>'ARRAY'
        OR source_hash IS NULL OR source_hash NOT REGEXP '^[0-9a-f]{64}$'
        OR snapshot_schema_version IS NULL OR card_codec_version IS NULL
        OR event_interpreter_version IS NULL OR protocol_version IS NULL
        OR JSON_EXTRACT(catalog_snapshot,'$.gameId') IS NULL
        OR JSON_EXTRACT(catalog_snapshot,'$.gameId')<>game_id
        OR JSON_UNQUOTE(JSON_EXTRACT(catalog_snapshot,'$.gameCode')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(catalog_snapshot,'$.gameCode'))<>game_code
        OR JSON_UNQUOTE(JSON_EXTRACT(catalog_snapshot,'$.playVersion')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(catalog_snapshot,'$.playVersion'))<>play_version
        OR JSON_EXTRACT(rule_validator,'$.gameId') IS NULL
        OR JSON_EXTRACT(rule_validator,'$.gameId')<>game_id
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.gameCode')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.gameCode'))<>game_code
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.playVersion')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.playVersion'))<>play_version
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.sourceHash')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.sourceHash'))<>source_hash
        OR JSON_UNQUOTE(JSON_EXTRACT(ui_snapshot,'$.roomRuleSourceHash')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(ui_snapshot,'$.roomRuleSourceHash'))<>source_hash
        OR JSON_UNQUOTE(JSON_EXTRACT(ui_schema,'$.roomRuleSourceHash')) IS NULL
        OR JSON_UNQUOTE(JSON_EXTRACT(ui_schema,'$.roomRuleSourceHash'))<>source_hash
        OR ui_hash<>source_hash
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v2.46 active publication identity/runtime/source-hash precondition failed';
  END IF;

  CREATE TEMPORARY TABLE v246_repair_candidate AS
  SELECT candidate.*,
         SHA2(CAST(candidate.catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256) catalog_hash,
         SHA2(CAST(candidate.rule_validator AS CHAR CHARACTER SET utf8mb4),256) rule_hash,
         SHA2(CAST(candidate.component_chain AS CHAR CHARACTER SET utf8mb4),256) component_hash,
         SHA2(CAST(candidate.bundle_snapshot AS CHAR CHARACTER SET utf8mb4),256) bundle_hash
    FROM (
      SELECT preimage.*,
             preimage.game_id*1000000000+2026100803 release_id,
             COALESCE((SELECT MAX(existing.release_version)
                         FROM aoo_game_release existing
                        WHERE existing.game_id=preimage.game_id
                          AND existing.play_version=preimage.play_version),0)+1 release_version,
             COALESCE((SELECT MAX(existing.index_generation)
                         FROM aoo_compiled_room_create_index existing
                        WHERE existing.game_id=preimage.game_id
                          AND existing.region_code=preimage.region_code
                          AND existing.play_version=preimage.play_version),0)+1 new_generation,
             JSON_OBJECT(
               'catalogSnapshot',JSON_EXTRACT(preimage.catalog_snapshot,'$'),
               'ruleValidator',JSON_EXTRACT(preimage.rule_validator,'$'),
               'uiSchema',JSON_EXTRACT(preimage.ui_schema,'$'),
               'componentSnapshot',JSON_EXTRACT(preimage.component_snapshot,'$'),
               'componentChain',JSON_EXTRACT(preimage.component_chain,'$')
             ) bundle_snapshot
        FROM v246_active_preimage preimage
    ) candidate;

  IF EXISTS (
    SELECT 1 FROM v246_repair_candidate
     WHERE catalog_hash<>SHA2(CAST(catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256)
        OR rule_hash<>SHA2(CAST(rule_validator AS CHAR CHARACTER SET utf8mb4),256)
        OR component_hash<>SHA2(CAST(component_chain AS CHAR CHARACTER SET utf8mb4),256)
        OR bundle_hash<>SHA2(CAST(bundle_snapshot AS CHAR CHARACTER SET utf8mb4),256)
        OR release_id IN (SELECT release_id FROM aoo_game_release)
        OR EXISTS (SELECT 1 FROM aoo_compiled_room_create_index existing
                    WHERE existing.game_id=v246_repair_candidate.game_id
                      AND existing.region_code=v246_repair_candidate.region_code
                      AND existing.play_version=v246_repair_candidate.play_version
                      AND existing.index_generation=v246_repair_candidate.new_generation)
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v2.46 cannot create a unique content-hash release/index generation';
  END IF;

  INSERT INTO aoo_game_release(
    release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,
    protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,
    component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,
    created_by,reason,validated_at,activated_at
  )
  SELECT release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,
         protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,
         component_snapshot,catalog_hash,rule_hash,source_hash,component_hash,bundle_hash,'ACTIVE',100,
         1,'Repair v2.46 active release snapshot content hashes',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
    FROM v246_repair_candidate;

  INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
  SELECT release_id,region_code,100,'ACTIVE' FROM v246_repair_candidate;

  INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,
    lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at
  )
  SELECT game_id,region_code,play_version,new_generation,release_id,component_chain,rule_validator,ui_schema,
         SHA2(CONCAT(game_id,'|',region_code,'|',play_version,'|',source_hash),256),bundle_hash,
         'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
    FROM v246_repair_candidate;

  -- This is the sole UPDATE-equivalent operation: it moves the active pointer
  -- atomically. Historical release/index rows remain byte-for-byte unchanged.
  INSERT INTO aoo_compiled_index_active(
    game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at
  )
  SELECT game_id,region_code,play_version,new_generation,release_id,1,1,
         'Repair v2.46 release snapshot content hashes',CURRENT_TIMESTAMP(3)
    FROM v246_repair_candidate
  ON DUPLICATE KEY UPDATE
    play_version=VALUES(play_version),index_generation=VALUES(index_generation),release_id=VALUES(release_id),
    cache_epoch=aoo_compiled_index_active.cache_epoch+1,activated_by=VALUES(activated_by),
    activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

  SELECT COUNT(*) INTO repaired_row_count
    FROM v246_repair_candidate candidate
    JOIN aoo_compiled_index_active active
      ON active.game_id=candidate.game_id AND active.region_code=candidate.region_code
     AND active.play_version=candidate.play_version AND active.index_generation=candidate.new_generation
     AND active.release_id=candidate.release_id
    JOIN aoo_compiled_room_create_index compiled
      ON compiled.game_id=active.game_id AND compiled.region_code=active.region_code
     AND compiled.play_version=active.play_version AND compiled.index_generation=active.index_generation
    JOIN aoo_game_release release_row ON release_row.release_id=active.release_id
   WHERE release_row.catalog_hash=SHA2(CAST(release_row.catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256)
     AND release_row.rule_hash=SHA2(CAST(compiled.rule_validator AS CHAR CHARACTER SET utf8mb4),256)
     AND release_row.component_hash=SHA2(CAST(compiled.component_chain AS CHAR CHARACTER SET utf8mb4),256)
     AND release_row.ui_hash=release_row.source_hash
     AND release_row.bundle_hash=compiled.bundle_hash
     AND release_row.bundle_hash=SHA2(CAST(JSON_OBJECT(
       'catalogSnapshot',JSON_EXTRACT(release_row.catalog_snapshot,'$'),
       'ruleValidator',JSON_EXTRACT(compiled.rule_validator,'$'),
       'uiSchema',JSON_EXTRACT(compiled.ui_schema,'$'),
       'componentSnapshot',JSON_EXTRACT(release_row.component_snapshot,'$'),
       'componentChain',JSON_EXTRACT(compiled.component_chain,'$')
     ) AS CHAR CHARACTER SET utf8mb4),256)
     AND JSON_UNQUOTE(JSON_EXTRACT(compiled.rule_validator,'$.sourceHash'))=release_row.source_hash
     AND JSON_UNQUOTE(JSON_EXTRACT(compiled.ui_schema,'$.roomRuleSourceHash'))=release_row.source_hash;

  IF repaired_row_count<>expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v2.46 post-insert content-hash verification failed';
  END IF;

  COMMIT;
END$$

CALL repair_active_release_snapshot_hashes_v20261008_03()$$
DROP PROCEDURE repair_active_release_snapshot_hashes_v20261008_03$$

DELIMITER ;
