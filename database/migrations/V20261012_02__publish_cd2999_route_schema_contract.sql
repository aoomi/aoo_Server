-- DRAFT ONLY: prepared outside the repository while the exact migration lock
-- is HELD by /root/pdk_compat_boundary. Do not apply until custody transfer is
-- recorded in the allowlisted lock and independent review is assigned.
--
-- Forward-only current CD2999 publication. The only current identity admitted
-- by this migration is game_id=2999/game_code=chengdu2999. Historical 630 is
-- retained as Compatibility history and is made non-current by retiring its
-- catalog, play, release, compiled index and route rows. No historical
-- migration or validated release content is rewritten.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_cd2999_route_schema_contract_v20261012_02()
BEGIN
  DECLARE old_catalog_count INT DEFAULT 0;
  DECLARE new_catalog_count INT DEFAULT 0;
  DECLARE release_drift_count INT DEFAULT 0;
  DECLARE index_drift_count INT DEFAULT 0;
  DECLARE release_count INT DEFAULT 0;
  DECLARE active_pointer_count INT DEFAULT 0;
  DECLARE lookup_hash_matches INT DEFAULT 0;

  SET @source_hash='f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5';
  SET @rule_content_hash='8aa129dcd826a9d49b0a3872731c2452d8532d45cc1d714d43320885b3024e1c';
  SET @component_content_hash='eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d';
  SET @release_id=2999001;
  SET @index_generation=3999001;
  SET @game_id=2999;
  SET @play_version='cd299-v1.0.0';
  SET @region_code='CN-51-01';
  SET @route_endpoint='ws://127.0.0.1:61384/api/v2/gateway/ws';
  SET @contract_version='common-room-v1';

  START TRANSACTION;

  SELECT COUNT(*) INTO old_catalog_count
    FROM aoo_game_catalog
   WHERE game_id=630
     AND game_code NOT IN ('chengdu2999','cd2999_compat_630');
  IF old_catalog_count<>0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_COMPATIBILITY_CATALOG_IDENTITY_DRIFT';
  END IF;

  SELECT COUNT(*) INTO new_catalog_count
    FROM aoo_game_catalog
   WHERE game_id=2999
     AND NOT (game_code='chengdu2999'
          AND family_code='poker:cd299'
          AND provider_key='cd299-native');
  IF new_catalog_count<>0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_CURRENT_CATALOG_IDENTITY_DRIFT';
  END IF;

  SELECT COUNT(*) INTO release_drift_count
    FROM aoo_game_release
   WHERE release_id=@release_id
     AND NOT (game_id=2999 AND play_version=@play_version
          AND snapshot_schema_version='room-snapshot-v1'
          AND card_codec_version='card-v3'
          AND event_interpreter_version='events-v4'
          AND protocol_version='protocol-v2'
          AND source_hash=@source_hash);
  IF release_drift_count<>0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_RELEASE_ID_ALREADY_BOUND_TO_DIFFERENT_CONTRACT';
  END IF;

  SELECT COUNT(*) INTO index_drift_count
    FROM aoo_compiled_room_create_index
   WHERE game_id=2999 AND region_code=@region_code AND play_version=@play_version
     AND index_generation=@index_generation
     AND NOT (release_id=@release_id AND lookup_hash=SHA2(CONCAT('cd2999|',@region_code,'|',@play_version,'|',@source_hash),256));
  IF index_drift_count<>0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_INDEX_GENERATION_ALREADY_BOUND_TO_DIFFERENT_CONTRACT';
  END IF;

  -- 630 remains queryable as history but cannot satisfy current catalog,
  -- configuration, create, snapshot, join or route admission.
  UPDATE aoo_game_service_route
     SET status='OFFLINE'
   WHERE game_id=630 AND play_version=@play_version;
  UPDATE aoo_compiled_room_create_index
     SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
   WHERE game_id=630 AND play_version=@play_version AND lifecycle_state='ACTIVE';
  UPDATE aoo_game_release_region rr
    JOIN aoo_game_release r ON r.release_id=rr.release_id
     SET rr.status='RETIRED'
   WHERE r.game_id=630 AND r.play_version=@play_version AND rr.status='ACTIVE';
  UPDATE aoo_game_release
     SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
   WHERE game_id=630 AND play_version=@play_version AND status='ACTIVE';
  UPDATE aoo_play_version
     SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
   WHERE game_id=630 AND play_version=@play_version AND status<>'RETIRED';
  UPDATE aoo_game_region
     SET availability='RETIRED'
   WHERE game_id=630 AND region_code=@region_code AND availability<>'RETIRED';
  UPDATE aoo_game_catalog
     SET game_code='cd2999_compat_630',
         provider_key='compatibility.cd2999.630',
         status='RETIRED'
   WHERE game_id=630;

  INSERT INTO aoo_game_catalog(
      game_id,game_code,display_name,category_code,family_code,provider_key,
      catalog_schema_version,status)
  VALUES(2999,'chengdu2999','成都扯旋','POKER','poker:cd299','cd299-native',1,'ACTIVE')
  ON DUPLICATE KEY UPDATE
      game_code=VALUES(game_code),display_name=VALUES(display_name),
      category_code=VALUES(category_code),family_code=VALUES(family_code),
      provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version),
      status='ACTIVE';

  INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
  VALUES(2999,'CN-51-01','AVAILABLE',1)
  ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;

  INSERT INTO aoo_play_version(
      game_id,play_version,default_region_code,rule_schema_version,
      ui_schema_version,component_schema_version,content_hash,status,
      activated_at,created_by)
  VALUES(2999,'cd299-v1.0.0','CN-51-01',1,1,1,@source_hash,'ACTIVE',CURRENT_TIMESTAMP(3),1)
  ON DUPLICATE KEY UPDATE
      default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),
      ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),
      content_hash=VALUES(content_hash),status='ACTIVE',activated_at=COALESCE(activated_at,CURRENT_TIMESTAMP(3));

  SET @fields=JSON_ARRAY(
    JSON_OBJECT('key','roomEndSelection','label','房间时长','control','radio','order',10,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue','ROUND_COUNT:10','options',JSON_ARRAY(
      JSON_OBJECT('value','DURATION_MINUTES:30','label','30分钟','order',10,'disabled',FALSE),
      JSON_OBJECT('value','DURATION_MINUTES:45','label','45分钟','order',20,'disabled',FALSE),
      JSON_OBJECT('value','DURATION_MINUTES:60','label','60分钟','order',30,'disabled',FALSE),
      JSON_OBJECT('value','ROUND_COUNT:10','label','10局','order',40,'disabled',FALSE),
      JSON_OBJECT('value','ROUND_COUNT:20','label','20局','order',50,'disabled',FALSE),
      JSON_OBJECT('value','ROUND_COUNT:30','label','30局','order',60,'disabled',FALSE))),
    JSON_OBJECT('key','startPlayers','label','开局人数','control','radio','order',20,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue',2,'options',JSON_ARRAY(2,4,6)),
    JSON_OBJECT('key','operationSeconds','label','操作时间','control','radio','order',30,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue',10,'options',JSON_ARRAY(10,15,30)),
    JSON_OBJECT('key','standPolicy','label','站起规则','control','radio','order',40,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue','LOSER_ONLY','options',JSON_ARRAY('LOSER_ONLY','EVERYONE','NOBODY')),
    JSON_OBJECT('key','mangoFlipMode','label','芒果翻法','control','radio','order',50,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue','LADDER','options',JSON_ARRAY('LADDER','ROLLING')),
    JSON_OBJECT('key','mangoRaise','label','升芒','control','radio','order',60,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue',3,'options',JSON_ARRAY(0,1,2,3,4,5)),
    JSON_OBJECT('key','mangoScore','label','芒果分数','control','number','order',70,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue',3,'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','openingBet','label','发招分数','control','number','order',80,'visible',TRUE,'disabled',FALSE,'required',TRUE,'defaultValue',3,'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','restMango','label','休芒','control','checkbox','order',90,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','beatMango','label','揍芒','control','checkbox','order',100,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','everyHandMango','label','手手芒','control','checkbox','order',110,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','firstRoundCanRest','label','首轮可休','control','checkbox','order',120,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','eachPlayerMustFollow','label','挨家必挨','control','checkbox','order',130,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',FALSE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','earthNineKing','label','地九王','control','checkbox','order',140,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','fireproofCard','label','防火牌','control','checkbox','order',150,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',TRUE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','bigHeadKeepsBase','label','头大不输底','control','checkbox','order',160,'visible',TRUE,'disabled',FALSE,'required',FALSE,'defaultValue',FALSE,'options',JSON_ARRAY(TRUE)),
    JSON_OBJECT('key','autoOperateAfterTimeouts','label','托管次数','control','radio','order',170,'visible',FALSE,'disabled',TRUE,'required',TRUE,'defaultValue',3,'options',JSON_ARRAY(3,4,5))
  );
  SET @rule_validator=JSON_OBJECT(
      'gameId',2999,'gameCode','chengdu2999','playVersion',@play_version,
      'family','poker:cd299','regionCode',@region_code,'contractVersion',@contract_version,
      'sourceHash',@source_hash,'releaseId',@release_id,'indexGeneration',@index_generation,
      'ruleSchemaVersion',1,'fields',@fields);
  SET @ui_schema=JSON_OBJECT(
      'gameId',2999,'gameCode','chengdu2999','playVersion',@play_version,
      'family','poker:cd299','bundle','poker-cx','scene','GameRoom2D',
      'landscapePrefab','CD299/Prefab/Landscape/CD299RoomLandscape',
      'portraitPrefab','CD299/Prefab/Portrait/CD299RoomPortrait',
      'roomRuleSourceHash',@source_hash,'roomRuleViewHash',@source_hash,
      'contractVersion',@contract_version,'releaseId',@release_id,
      'indexGeneration',@index_generation,'ruleSchemaVersion',1,
      'uiSchemaVersion',1,'fields',@fields);
  SET @catalog_snapshot=JSON_OBJECT(
      'gameId',2999,'gameCode','chengdu2999','displayName','成都扯旋',
      'category','POKER','family','poker:cd299','regionCode',@region_code,
      'playVersion',@play_version,'providerKey','cd299-native',
      'contractVersion',@contract_version,'sourceHash',@source_hash,
      'releaseId',@release_id,'indexGeneration',@index_generation,
      'ruleSchemaVersion',1,'uiSchemaVersion',1,'componentSchemaVersion',1);
  SET @component_chain=JSON_ARRAY('com.aoo.bcg.poker.cd299.Cd299GameProvider@cd299-v1.0.0');
  SET @component_snapshot=JSON_OBJECT(
      'providerKey','cd299-native','providerClass','com.aoo.bcg.poker.cd299.Cd299GameProvider',
      'factoryClass','com.aoo.bcg.poker.cd299.Cd299ProviderFactory',
      'componentContentHash',@component_content_hash,'sourceHash',@source_hash,
      'contractVersion',@contract_version);
  SET @bundle_hash=SHA2(CAST(JSON_OBJECT(
      'catalogSnapshot',@catalog_snapshot,'ruleValidator',@rule_validator,
      'uiSchema',@ui_schema,'componentSnapshot',@component_snapshot,
      'componentChain',@component_chain) AS CHAR CHARACTER SET utf8mb4),256);

  INSERT INTO aoo_game_release(
      release_id,game_id,play_version,snapshot_schema_version,card_codec_version,
      event_interpreter_version,protocol_version,source_hash,release_version,
      release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
      catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,
      created_by,reason,validated_at,activated_at)
  VALUES(@release_id,2999,@play_version,'room-snapshot-v1','card-v3','events-v4','protocol-v2',
      @source_hash,2026101202,'REGIONAL',@catalog_snapshot,@rule_validator,@ui_schema,@component_snapshot,
      SHA2(CAST(@catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256),
      SHA2(CAST(@rule_validator AS CHAR CHARACTER SET utf8mb4),256),@source_hash,
      SHA2(CAST(@component_chain AS CHAR CHARACTER SET utf8mb4),256),@bundle_hash,
      'ACTIVE',100,1,'Publish signed current CD2999 route/schema contract r1',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE
      status=VALUES(status),rollout_percent=VALUES(rollout_percent),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

  INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
  VALUES(@release_id,@region_code,100,'ACTIVE')
  ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

  INSERT INTO aoo_compiled_room_create_index(
      game_id,region_code,play_version,index_generation,release_id,component_chain,
      rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
  VALUES(2999,@region_code,@play_version,@index_generation,@release_id,@component_chain,
      @rule_validator,@ui_schema,
      SHA2(CONCAT('cd2999|',@region_code,'|',@play_version,'|',@source_hash),256),
      @bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

  INSERT INTO aoo_compiled_index_active(
      game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
  VALUES(2999,@region_code,@play_version,@index_generation,@release_id,1,1,
      'Activate signed current CD2999 route/schema contract r1',CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE
      play_version=VALUES(play_version),index_generation=VALUES(index_generation),release_id=VALUES(release_id),
      cache_epoch=aoo_compiled_index_active.cache_epoch+1,activated_by=VALUES(activated_by),
      activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

  INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
  VALUES(2999001,2999,@play_version,@route_endpoint,100,'ACTIVE')
  ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

  INSERT INTO aoo_published_game_configuration(game_id,play_version,release_id,configuration_payload,created_by,reason,created_at)
  VALUES(2999,@play_version,@release_id,
      JSON_OBJECT('gameId',2999,'gameCode','chengdu2999','playVersion',@play_version,
        'family','poker:cd299','regionCode',@region_code,'providerKey','cd299-native',
        'contractVersion',@contract_version,'sourceHash',@source_hash,'releaseId',@release_id,
        'indexGeneration',@index_generation,'rules',@rule_validator,'ui',@ui_schema),
      1,'Publish signed current CD2999 route/schema contract r1',CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),configuration_payload=VALUES(configuration_payload),
      reason=VALUES(reason),created_at=VALUES(created_at);

  SELECT COUNT(*) INTO release_count
    FROM aoo_game_release
   WHERE release_id=@release_id AND game_id=2999 AND play_version=@play_version
     AND status='ACTIVE' AND source_hash=@source_hash;
  SELECT COUNT(*) INTO active_pointer_count
    FROM aoo_compiled_index_active
   WHERE game_id=2999 AND region_code=@region_code AND play_version=@play_version
     AND index_generation=@index_generation AND release_id=@release_id;
  SELECT COUNT(*) INTO lookup_hash_matches
    FROM aoo_compiled_room_create_index
   WHERE game_id=2999 AND region_code=@region_code AND play_version=@play_version
     AND index_generation=@index_generation AND release_id=@release_id
     AND lookup_hash=SHA2(CONCAT('cd2999|',@region_code,'|',@play_version,'|',@source_hash),256)
     AND BINARY lookup_hash=BINARY SHA2(CONCAT('cd2999|',@region_code,'|',@play_version,'|',@source_hash),256);
  IF release_count<>1 OR active_pointer_count<>1 OR lookup_hash_matches<>1 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_ROUTE_SCHEMA_PUBLICATION_POSTCHECK_FAILED';
  END IF;

  COMMIT;
END$$

CALL publish_cd2999_route_schema_contract_v20261012_02()$$
DROP PROCEDURE publish_cd2999_route_schema_contract_v20261012_02$$

DELIMITER ;
