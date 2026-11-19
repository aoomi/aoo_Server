-- Forward-only prerequisite for the current CD2999 publication.
--
-- Signed current inputs:
--   canonical-runtime-metadata-manifest.json SHA-256:
--     a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282
--   canonical identity source SHA-256:
--     438c51f1013bd7ac4dc9da45aac72fb2b405fa30205ae1403f0c4f3974b3f02b
--   current sourceHash/releaseId/indexGeneration:
--     f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5 /
--     2999001 / 3999001
--
-- This migration exists before V20261012_01 because that pricing migration is
-- intentionally bound to the current 2999 release/index.  It never edits a
-- historical migration, derives IDs from game_id/MAX()/time/defaults, or
-- promotes the historical 630 identity.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_cd2999_current_identity_v20261012_00()
BEGIN
  DECLARE current_release_matches INT DEFAULT 0;
  DECLARE current_index_matches INT DEFAULT 0;
  DECLARE current_pointer_matches INT DEFAULT 0;
  DECLARE v_source_hash CHAR(64) DEFAULT 'f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5';
  DECLARE catalog_snapshot JSON;
  DECLARE rule_snapshot JSON;
  DECLARE ui_snapshot JSON;
  DECLARE component_snapshot JSON;
  DECLARE component_chain JSON;
  DECLARE rule_validator JSON;
  DECLARE ui_schema JSON;
  DECLARE bundle_hash CHAR(64);

  IF EXISTS (
    SELECT 1 FROM aoo_game_catalog
     WHERE game_code='chengdu2999' AND game_id<>2999
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_CURRENT_CODE_BOUND_TO_NON_CURRENT_IDENTITY';
  END IF;

  IF EXISTS (
    SELECT 1 FROM aoo_game_catalog
     WHERE game_id=2999
       AND (game_code<>'chengdu2999' OR family_code<>'poker:cd299'
            OR provider_key<>'cd299-native' OR catalog_schema_version<>1)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_CURRENT_CATALOG_PREIMAGE_DRIFTED';
  END IF;

  IF EXISTS (
    SELECT 1 FROM aoo_play_version
     WHERE game_id=2999 AND play_version='cd299-v1.0.0'
       AND (default_region_code<>'CN-51-01' OR rule_schema_version<>1
            OR ui_schema_version<>1 OR component_schema_version<>1
            OR content_hash<>'eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_CURRENT_SCHEMA_TUPLE_DRIFTED';
  END IF;

  SET catalog_snapshot=JSON_OBJECT(
    'gameId',2999,'gameCode','chengdu2999','displayName','成都扯旋',
    'category','POKER','family','poker:cd299','providerKey','cd299-native',
    'providerClass','com.aoo.bcg.poker.cd299.Cd299GameProvider',
    'regionCode','CN-51-01','playVersion','cd299-v1.0.0',
    'contractVersion','common-room-v1','schemaTuple',JSON_OBJECT(
      'ruleSchemaVersion',1,'uiSchemaVersion',1,'componentSchemaVersion',1),
    'sourceHash',v_source_hash,
    'normalizedContentHash','eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d',
    'normalizedWorkbookHash','d6169a704925bb3af47e2fcfc2f80dc84718b40a5da4fdeb4eda4b7cb2e51853'
  );

  SET rule_validator=JSON_OBJECT(
    'gameId',2999,'gameCode','chengdu2999','playVersion','cd299-v1.0.0',
    'contractVersion','common-room-v1','schemaVersion',1,'source','成都扯旋CD299.xlsx',
    'sourceHash',v_source_hash,'provider','cd299-native',
    'fields',JSON_ARRAY(
      JSON_OBJECT('key','roomEndSelection'),JSON_OBJECT('key','startPlayers'),
      JSON_OBJECT('key','operationSeconds'),JSON_OBJECT('key','standPolicy'),
      JSON_OBJECT('key','mangoFlipMode'),JSON_OBJECT('key','mangoRaise'),
      JSON_OBJECT('key','mangoScore'),JSON_OBJECT('key','openingBet'),
      JSON_OBJECT('key','restMango'),JSON_OBJECT('key','beatMango'),
      JSON_OBJECT('key','everyHandMango'),JSON_OBJECT('key','firstRoundCanRest'),
      JSON_OBJECT('key','eachPlayerMustFollow'),JSON_OBJECT('key','earthNineKing'),
      JSON_OBJECT('key','fireproofCard'),JSON_OBJECT('key','bigHeadKeepsBase'),
      JSON_OBJECT('key','autoOperateAfterTimeouts'),JSON_OBJECT('key','maxPlayers'),
      JSON_OBJECT('key','payerMode')
    )
  );

  SET ui_schema=JSON_OBJECT(
    'gameId',2999,'gameCode','chengdu2999','playVersion','cd299-v1.0.0',
    'contractVersion','common-room-v1','schemaVersion',1,
    'bundle','poker-cx','scene','GameRoom2D','playFamily','poker:cd299',
    'roomRuleSourceHash',v_source_hash,
    'normalizedWorkbookHash','d6169a704925bb3af47e2fcfc2f80dc84718b40a5da4fdeb4eda4b7cb2e51853',
    'fields',JSON_ARRAY(
      JSON_OBJECT('key','roomEndSelection'),JSON_OBJECT('key','startPlayers'),
      JSON_OBJECT('key','operationSeconds'),JSON_OBJECT('key','standPolicy'),
      JSON_OBJECT('key','mangoFlipMode'),JSON_OBJECT('key','mangoRaise'),
      JSON_OBJECT('key','mangoScore'),JSON_OBJECT('key','openingBet'),
      JSON_OBJECT('key','restMango'),JSON_OBJECT('key','beatMango'),
      JSON_OBJECT('key','everyHandMango'),JSON_OBJECT('key','firstRoundCanRest'),
      JSON_OBJECT('key','eachPlayerMustFollow'),JSON_OBJECT('key','earthNineKing'),
      JSON_OBJECT('key','fireproofCard'),JSON_OBJECT('key','bigHeadKeepsBase'),
      JSON_OBJECT('key','autoOperateAfterTimeouts')
    )
  );

  SET component_snapshot=JSON_OBJECT(
    'contractVersion','common-room-v1','schemaVersion',1,
    'providerKey','cd299-native',
    'providerClass','com.aoo.bcg.poker.cd299.Cd299GameProvider',
    'providerFactory','com.aoo.bcg.poker.cd299.Cd299ProviderFactory',
    'rulesClass','com.aoo.bcg.poker.cd299.Cd299Rules',
    'sourceHash',v_source_hash,
    'normalizedComponentHash','eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d'
  );
  SET component_chain=JSON_ARRAY(
    'com.aoo.bcg.poker.cd299.Cd299ProviderFactory@cd299-v1.0.0',
    'com.aoo.bcg.poker.cd299.Cd299GameProvider',
    'common-room-v1'
  );
  SET bundle_hash=SHA2(CONCAT('cd2999-route-schema-contract-r1|',v_source_hash,'|2999001|3999001'),256);

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
  VALUES(2999,'CN-51-01','AVAILABLE',100)
  ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=100;

  INSERT INTO aoo_play_version(
      game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
      component_schema_version,content_hash,status,activated_at,created_by)
  VALUES(2999,'cd299-v1.0.0','CN-51-01',1,1,1,
      'eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d',
      'ACTIVE',CURRENT_TIMESTAMP(3),1)
  ON DUPLICATE KEY UPDATE
      default_region_code='CN-51-01',rule_schema_version=1,ui_schema_version=1,
      component_schema_version=1,content_hash=VALUES(content_hash),status='ACTIVE',
      activated_at=COALESCE(aoo_play_version.activated_at,VALUES(activated_at)),
      retired_at=NULL;

  INSERT INTO aoo_game_release(
      release_id,game_id,play_version,release_version,release_scope,
      catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
      catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
      status,rollout_percent,created_by,reason,validated_at,activated_at,
      snapshot_schema_version,card_codec_version,event_interpreter_version,
      protocol_version,source_hash)
  VALUES(
      2999001,2999,'cd299-v1.0.0',1,'REGIONAL',catalog_snapshot,rule_validator,
      ui_schema,component_snapshot,SHA2(CAST(catalog_snapshot AS CHAR),256),
      '8aa129dcd826a9d49b0a3872731c2452d8532d45cc1d714d43320885b3024e1c',
      v_source_hash,'eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d',
      bundle_hash,'ACTIVE',100,1,
      'Publish signed current CD2999 identity before current pricing and route/schema contract',
      CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),'room-snapshot-v1','card-v3','events-v4',
      'protocol-v2',v_source_hash)
  ON DUPLICATE KEY UPDATE
      status='ACTIVE',rollout_percent=100,validated_at=VALUES(validated_at),
      activated_at=VALUES(activated_at),retired_at=NULL;

  INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
  VALUES(2999001,'CN-51-01',100,'ACTIVE')
  ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

  INSERT INTO aoo_compiled_room_create_index(
      game_id,region_code,play_version,index_generation,release_id,component_chain,
      rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
      compiled_at,validated_at,activated_at)
  VALUES(
      2999,'CN-51-01','cd299-v1.0.0',3999001,2999001,component_chain,
      rule_validator,ui_schema,
      SHA2(CONCAT('cd2999|CN-51-01|cd299-v1.0.0|',v_source_hash),256),
      bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE
      lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),
      activated_at=VALUES(activated_at),retired_at=NULL;

  INSERT INTO aoo_compiled_index_active(
      game_id,region_code,play_version,index_generation,release_id,cache_epoch,
      activated_by,activation_reason,activated_at)
  VALUES(2999,'CN-51-01','cd299-v1.0.0',3999001,2999001,1,1,
      'Activate signed current CD2999 identity; historical 630 is not current',CURRENT_TIMESTAMP(3))
  ON DUPLICATE KEY UPDATE
      play_version=VALUES(play_version),index_generation=VALUES(index_generation),
      release_id=VALUES(release_id),cache_epoch=aoo_compiled_index_active.cache_epoch+1,
      activated_by=1,activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

  SELECT COUNT(*) INTO current_release_matches
    FROM aoo_game_release
   WHERE release_id=2999001 AND game_id=2999 AND play_version='cd299-v1.0.0'
     AND status='ACTIVE' AND aoo_game_release.source_hash=v_source_hash
     AND snapshot_schema_version='room-snapshot-v1' AND card_codec_version='card-v3'
     AND event_interpreter_version='events-v4' AND protocol_version='protocol-v2';
  SELECT COUNT(*) INTO current_index_matches
    FROM aoo_compiled_room_create_index
   WHERE game_id=2999 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0'
     AND index_generation=3999001 AND release_id=2999001 AND lifecycle_state='ACTIVE'
     AND lookup_hash=SHA2(CONCAT('cd2999|CN-51-01|cd299-v1.0.0|',v_source_hash),256)
     AND JSON_UNQUOTE(JSON_EXTRACT(rule_validator,'$.sourceHash'))=v_source_hash
     AND JSON_UNQUOTE(JSON_EXTRACT(ui_schema,'$.roomRuleSourceHash'))=v_source_hash;
  SELECT COUNT(*) INTO current_pointer_matches
    FROM aoo_compiled_index_active
   WHERE game_id=2999 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0'
     AND index_generation=3999001 AND release_id=2999001;

  IF current_release_matches<>1 OR current_index_matches<>1 OR current_pointer_matches<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_CURRENT_RELEASE_INDEX_BINDING_INCOMPLETE';
  END IF;
END$$

CALL publish_cd2999_current_identity_v20261012_00()$$
DROP PROCEDURE publish_cd2999_current_identity_v20261012_00$$

DELIMITER ;
