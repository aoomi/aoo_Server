-- Activate the canonical Chengdu Mahjong identity for local and deployed
-- catalog routing.  The former 516/CD101 rows stay as retired history so
-- rooms created before the rename can still be recovered.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

SET @old_game_id = 516;
SET @new_game_id = 1001;
SET @old_code = 'CD101';
SET @new_code = 'chengdu1001';
SET @old_version = 'legacy-equivalent-1';
SET @new_version = 'chengdu1001-v1.0.0';
SET @new_release_id = 100120261003;
SET @new_generation = 2026100203;

INSERT INTO aoo_game_catalog
  (game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
SELECT @new_game_id,@new_code,display_name,category_code,family_code,
       CONCAT('catalog.',@new_code),catalog_schema_version,'ACTIVE'
FROM aoo_game_catalog WHERE game_id=@old_game_id
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),
  category_code=VALUES(category_code),family_code=VALUES(family_code),
  provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version),status='ACTIVE';

INSERT INTO aoo_game_region(game_id,region_code,availability,priority,available_from,available_until)
SELECT @new_game_id,region_code,'AVAILABLE',priority,available_from,available_until
FROM aoo_game_region WHERE game_id=@old_game_id AND availability='AVAILABLE'
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority),
  available_from=VALUES(available_from),available_until=VALUES(available_until);

INSERT INTO aoo_play_version
  (game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
   component_schema_version,content_hash,status,compatibility_floor,activated_at,created_by)
SELECT @new_game_id,@new_version,default_region_code,rule_schema_version,ui_schema_version,
       component_schema_version,SHA2(CONCAT('canonical-mahjong|',@new_code,'|',@new_version),256),
       'ACTIVE',compatibility_floor,CURRENT_TIMESTAMP(3),created_by
FROM aoo_play_version WHERE game_id=@old_game_id AND play_version=@old_version
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),
  rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),
  component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash),
  status='ACTIVE',activated_at=VALUES(activated_at);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
SELECT @new_game_id*1000000+1,@new_game_id,@new_version,endpoint,priority,'ACTIVE'
FROM aoo_game_service_route WHERE game_id=@old_game_id AND play_version=@old_version AND status='ACTIVE'
ORDER BY priority LIMIT 1
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

INSERT INTO aoo_room_cost_policy
  (game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,
   payer_mode,currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version,content_hash,status)
SELECT @new_game_id,@new_version,region_code,measure_kind,measure_value,round_count,player_count,
       payer_mode,currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version+1,
       SHA2(CONCAT('canonical-mahjong|',@new_code,'|',@new_version,'|',region_code,'|',measure_kind,'|',measure_value,'|',player_count,'|',payer_mode),256),'ACTIVE'
FROM aoo_room_cost_policy WHERE game_id=@old_game_id AND play_version=@old_version AND status='ACTIVE'
ON DUPLICATE KEY UPDATE cost_minor=VALUES(cost_minor),club_cost_minor=VALUES(club_cost_minor),
  union_cost_minor=VALUES(union_cost_minor),policy_version=VALUES(policy_version),
  content_hash=VALUES(content_hash),status='ACTIVE';

INSERT INTO aoo_game_release
  (release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,
   ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
   status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT @new_release_id,@new_game_id,@new_version,release_version+1,release_scope,
       JSON_SET(catalog_snapshot,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       JSON_SET(rule_snapshot,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       JSON_SET(ui_snapshot,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       JSON_SET(component_snapshot,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       SHA2(CONCAT(@new_code,'|catalog|',@new_version),256),SHA2(CONCAT(@new_code,'|rules|',@new_version),256),
       SHA2(CONCAT(@new_code,'|ui|',@new_version),256),SHA2(CONCAT(@new_code,'|components|',@new_version),256),
       SHA2(CONCAT(@new_code,'|bundle|',@new_version),256),'ACTIVE',rollout_percent,created_by,
       CONCAT('Activate canonical Mahjong identity ',@new_code),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release
WHERE game_id=@old_game_id AND play_version=@old_version AND status='ACTIVE'
ORDER BY release_id DESC LIMIT 1
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),
  release_version=VALUES(release_version),catalog_snapshot=VALUES(catalog_snapshot),
  rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),
  catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),
  component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),status='ACTIVE',
  rollout_percent=VALUES(rollout_percent),reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT @new_release_id,region_code,rollout_percent,'ACTIVE'
FROM aoo_game_release_region
WHERE release_id=(SELECT MAX(release_id) FROM aoo_game_release WHERE game_id=@old_game_id AND play_version=@old_version AND status='ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index
  (game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,
   lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
SELECT @new_game_id,region_code,@new_version,@new_generation,@new_release_id,
       JSON_ARRAY(CONCAT('com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry@',@new_version)),
       JSON_SET(rule_validator,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       ui_schema,SHA2(CONCAT(@new_code,'|index|',@new_generation,'|',@new_version),256),
       SHA2(CONCAT(@new_code,'|bundle|',@new_version),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index
WHERE game_id=@old_game_id AND play_version=@old_version AND lifecycle_state='ACTIVE'
ORDER BY index_generation DESC LIMIT 1
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),
  bundle_hash=VALUES(bundle_hash),lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

INSERT INTO aoo_compiled_index_active
  (game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT @new_game_id,region_code,@new_version,@new_generation,@new_release_id,cache_epoch+1,activated_by,
       CONCAT('Activate canonical Mahjong identity ',@new_code),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active WHERE game_id=@old_game_id AND play_version=@old_version
ON DUPLICATE KEY UPDATE play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),cache_epoch=VALUES(cache_epoch),activated_by=VALUES(activated_by),
  activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

INSERT INTO aoo_published_game_configuration
  (game_id,play_version,release_id,configuration_payload,created_by,reason)
SELECT @new_game_id,@new_version,@new_release_id,
       JSON_SET(configuration_payload,'$.gameId',@new_game_id,'$.gameCode',@new_code,'$.playVersion',@new_version),
       created_by,CONCAT('Activate canonical Mahjong identity ',@new_code)
FROM aoo_published_game_configuration
WHERE game_id=@old_game_id AND play_version=@old_version
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),configuration_payload=VALUES(configuration_payload),
  created_by=VALUES(created_by),reason=VALUES(reason),created_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_room_rule_definition
  (game_id,play_version,rule_key,rule_kind,value_type,required_value,default_value,minimum_value,
   maximum_value,validation_pattern,description,ordinal,schema_version,content_hash,status)
SELECT @new_game_id,@new_version,rule_key,rule_kind,value_type,required_value,default_value,minimum_value,
       maximum_value,validation_pattern,description,ordinal,schema_version,
       SHA2(CONCAT(@new_code,'|',@new_version,'|',rule_key,'|',schema_version),256),'ACTIVE'
FROM aoo_room_rule_definition WHERE game_id=@old_game_id AND play_version=@old_version AND status='ACTIVE'
ON DUPLICATE KEY UPDATE default_value=VALUES(default_value),minimum_value=VALUES(minimum_value),
  maximum_value=VALUES(maximum_value),validation_pattern=VALUES(validation_pattern),description=VALUES(description),
  ordinal=VALUES(ordinal),schema_version=VALUES(schema_version),content_hash=VALUES(content_hash),status='ACTIVE';

-- Keep the old identity only for historical room recovery and replay.
UPDATE aoo_game_service_route SET status='OFFLINE' WHERE game_id=@old_game_id AND status='ACTIVE';
UPDATE aoo_game_region SET availability='RETIRED' WHERE game_id=@old_game_id AND availability='AVAILABLE';
UPDATE aoo_play_version SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3)) WHERE game_id=@old_game_id AND status='ACTIVE';
UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3)) WHERE game_id=@old_game_id AND status='ACTIVE';
UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3)) WHERE game_id=@old_game_id AND lifecycle_state='ACTIVE';
DELETE FROM aoo_compiled_index_active WHERE game_id=@old_game_id;
UPDATE aoo_game_catalog SET status='RETIRED' WHERE game_id=@old_game_id AND status='ACTIVE';
COMMIT;
