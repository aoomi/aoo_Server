-- Repair the canonical Chengdu Mahjong publication when the historical 516/CD101
-- source rows are absent from a clean migration database.  The old identity is
-- never activated by this migration; any existing 516 rows remain compatibility
-- history and retain their current retired/offline state.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

SET @game_id = 1001;
SET @game_code = 'chengdu1001';
SET @play_version = 'chengdu1001-v1.0.0';
SET @region_code = 'CN-51-01';
SET @family_code = 'mahjong:xue-zhan';
SET @provider_key = 'catalog.chengdu1001';
SET @endpoint = 'ws://127.0.0.1:8080/api/v2/gateway/ws';
SET @source_hash = '9ca9df7a061bebadbe31ccd2acfc169bf968076c6dc459baafe7cf262dc0c87c';
SET @release_id = 100120261005;
SET @release_version = 2026100605;
SET @index_generation = 2026100605;
SET @route_id = 1001000005;

SET @rule_fields = CAST('[
  {"key":"setCount","label":"局数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},
  {"key":"playerNum","label":"人数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[3],"defaultValue":4,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false},{"value":4,"label":"4人","order":30,"disabled":false}]}
]' AS JSON);
SET @validator_fields = JSON_ARRAY_APPEND(@rule_fields,'$',JSON_OBJECT(
  'key','payerMode','control','radio','order',10000,'disabled',false,'required',true,
  'defaultValue','OWNER','options',JSON_ARRAY(JSON_OBJECT('value','OWNER','order',10,'disabled',false))));
SET @validator = JSON_OBJECT(
  'gameId',@game_id,'gameCode',@game_code,'playVersion',@play_version,
  'roomMeasureKind','ROUND_COUNT','source','成都血战麻将.xlsx',
  'authority','canonical Chengdu Mahjong server chain','fields',CAST(@validator_fields AS JSON));
SET @ui = JSON_OBJECT(
  'bundle','games-mahjong','scene','GameRoom2D','playFamily',@family_code,
  'roomRuleSourceHash',@source_hash,'roomRuleViewHash',@source_hash,'fields',CAST(@rule_fields AS JSON));

INSERT INTO aoo_game_family(family_code,category_code,display_name,status)
VALUES(@family_code,'MAHJONG','血战麻将','ACTIVE')
ON DUPLICATE KEY UPDATE category_code=VALUES(category_code),display_name=VALUES(display_name),status='ACTIVE';

INSERT INTO aoo_game_catalog(
  game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(@game_id,@game_code,'成都麻将','MAHJONG',@family_code,@provider_key,1,'ACTIVE')
ON DUPLICATE KEY UPDATE
  game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),
  family_code=VALUES(family_code),provider_key=VALUES(provider_key),
  catalog_schema_version=VALUES(catalog_schema_version),status='ACTIVE';

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(@game_id,@region_code,'AVAILABLE',1)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority);

INSERT INTO aoo_play_version(
  game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
  component_schema_version,content_hash,status,activated_at,created_by)
VALUES(@game_id,@play_version,@region_code,1,1,1,
       SHA2(CONCAT('canonical-mahjong|',@game_code,'|',@play_version,'|',@source_hash),256),
       'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE
  default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),
  ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),
  content_hash=VALUES(content_hash),status='ACTIVE',retired_at=NULL,
  activated_at=COALESCE(activated_at,VALUES(activated_at));

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(@route_id,@game_id,@play_version,@endpoint,1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),
  endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

-- The clean migration chain has no previous Mahjong price rows.  These are the
-- canonical local room dimensions already used by the rule source; cost remains
-- zero until a separately governed product pricing publication supplies prices.
INSERT INTO aoo_room_cost_policy(
  game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,
  payer_mode,currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version,content_hash,status)
SELECT @game_id,@play_version,@region_code,'ROUND_COUNT',r.round_count,r.round_count,p.player_count,
       'OWNER','ROOM_CARD',0,NULL,NULL,1,
       SHA2(CONCAT('canonical-mahjong|',@game_code,'|',@play_version,'|',@region_code,'|',r.round_count,'|',p.player_count,'|OWNER|ROOM_CARD|0'),256),
       'ACTIVE'
FROM (SELECT 8 AS round_count UNION ALL SELECT 12 UNION ALL SELECT 16) r
CROSS JOIN (SELECT 2 AS player_count UNION ALL SELECT 3 UNION ALL SELECT 4) p
WHERE 1=1
ON DUPLICATE KEY UPDATE
  currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),
  club_cost_minor=VALUES(club_cost_minor),union_cost_minor=VALUES(union_cost_minor),
  policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(
  @release_id,@game_id,@play_version,@release_version,'REGIONAL',
  JSON_OBJECT('gameId',@game_id,'gameCode',@game_code,'playVersion',@play_version,'family',@family_code,'provider',@provider_key),
  JSON_OBJECT('roomRuleSchema',CAST(@validator AS JSON)),CAST(@ui AS JSON),
  JSON_OBJECT('runtime','MahjongCatalogRuntimeRegistry','profile',@game_code),
  SHA2(CONCAT(@game_code,'|catalog|',@play_version,'|',@source_hash),256),
  SHA2(CONCAT(@game_code,'|rules|',@play_version,'|',@source_hash),256),
  @source_hash,
  SHA2(CONCAT(@game_code,'|components|',@play_version),256),
  SHA2(CONCAT(@game_code,'|bundle|',@play_version,'|',@source_hash),256),
  'ACTIVE',100,1,'Repair canonical Chengdu Mahjong publication closure',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
  release_id=release_id;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(@release_id,@region_code,100,'ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,component_chain,
  rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
  compiled_at,validated_at,activated_at)
VALUES(
  @game_id,@region_code,@play_version,@index_generation,@release_id,
  JSON_ARRAY(CONCAT('com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry@',@play_version)),
  CAST(@validator AS JSON),CAST(@ui AS JSON),
  SHA2(CONCAT(@game_code,'|',@region_code,'|',@play_version,'|',@source_hash),256),
  SHA2(CONCAT(@game_code,'|bundle|',@play_version,'|',@source_hash),256),
  'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
  release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),
  lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),
  lifecycle_state='ACTIVE',retired_at=NULL,validated_at=VALUES(validated_at),
  activated_at=VALUES(activated_at);

INSERT INTO aoo_compiled_index_active(
  game_id,region_code,play_version,index_generation,release_id,cache_epoch,
  activated_by,activation_reason,activated_at)
VALUES(@game_id,@region_code,@play_version,@index_generation,@release_id,1,1,
       'Repair canonical Chengdu Mahjong publication closure',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
  play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),cache_epoch=IF(
    aoo_compiled_index_active.release_id=VALUES(release_id)
    AND aoo_compiled_index_active.index_generation=VALUES(index_generation),
    aoo_compiled_index_active.cache_epoch,aoo_compiled_index_active.cache_epoch+1),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),
  activated_at=VALUES(activated_at);

INSERT INTO aoo_published_game_configuration(
  game_id,play_version,release_id,configuration_payload,created_by,reason)
VALUES(@game_id,@play_version,@release_id,
       JSON_OBJECT('gameId',@game_id,'gameCode',@game_code,'playVersion',@play_version,
                   'family',@family_code,'roomRuleSourceHash',@source_hash,'fields',CAST(@rule_fields AS JSON)),
       1,'Repair canonical Chengdu Mahjong publication closure')
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),configuration_payload=VALUES(configuration_payload),
  created_by=VALUES(created_by),reason=VALUES(reason),created_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_room_rule_definition(
  game_id,play_version,rule_key,rule_kind,value_type,required_value,default_value,
  minimum_value,maximum_value,validation_pattern,description,ordinal,schema_version,content_hash,status)
VALUES
  (@game_id,@play_version,'setCount','ROOM','INTEGER',1,JSON_EXTRACT('8','$'),NULL,NULL,NULL,'局数',80,1,
   SHA2(CONCAT(@game_code,'|',@play_version,'|setCount|1'),256),'ACTIVE'),
  (@game_id,@play_version,'playerNum','ROOM','INTEGER',1,JSON_EXTRACT('4','$'),NULL,NULL,NULL,'人数',90,1,
   SHA2(CONCAT(@game_code,'|',@play_version,'|playerNum|1'),256),'ACTIVE')
ON DUPLICATE KEY UPDATE
  rule_kind=VALUES(rule_kind),value_type=VALUES(value_type),required_value=VALUES(required_value),
  default_value=VALUES(default_value),description=VALUES(description),ordinal=VALUES(ordinal),
  schema_version=VALUES(schema_version),content_hash=VALUES(content_hash),status='ACTIVE';

-- Only stale rows for the same current 1001 play are retired.  Historical 516/CD101
-- rows are intentionally untouched and cannot become a current route through this file.
UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED'
WHERE r.game_id=@game_id AND r.play_version=@play_version
  AND r.release_id<>@release_id AND rr.status='ACTIVE';
UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=@game_id AND play_version=@play_version AND release_id<>@release_id AND status='ACTIVE';
UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=@game_id AND play_version=@play_version AND index_generation<>@index_generation
  AND lifecycle_state='ACTIVE';
UPDATE aoo_game_service_route
SET status='OFFLINE'
WHERE game_id=@game_id AND play_version=@play_version AND route_id<>@route_id AND status='ACTIVE';

COMMIT;
