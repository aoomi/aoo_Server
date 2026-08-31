-- Publish the existing native PDK provider as a real Chengdu catalog entry.
-- This is a catalog classification only; it does not create regional account,
-- wallet, room-number, club, history, replay, or service-route isolation.

UPDATE aoo_game_catalog
SET display_name='成都跑得快', family_code='poker:pao-de-kuai', row_version=row_version+1
WHERE game_id=8;

UPDATE aoo_play_version
SET default_region_code='CN-51-01', row_version=row_version+1
WHERE game_id=8 AND play_version='1.0.0';

UPDATE aoo_game_region SET availability='RETIRED'
WHERE game_id=8 AND region_code='GLOBAL';

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(8,'CN-51-01','AVAILABLE',10)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority);

INSERT INTO aoo_game_release(
 release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
 catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
 status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(
 800000001,8,'1.0.0',1,'REGIONAL',
 JSON_OBJECT('gameId',8,'family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider'),
 JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),
 JSON_OBJECT('scene','GameRoom2D','bundle','poker01-prefab','playFamily','poker:pao-de-kuai','playerCounts',JSON_ARRAY(2,3,4),'smallSettleTemplate','SmallSettleTpl_pdk_00','bigSettleTemplate','BigSettleTpl_00'),
 JSON_OBJECT('providerVersion','1.0.0'),
 SHA2('pdk-8-catalog-v1',256),SHA2('pdk-8-rules-v1',256),SHA2('pdk-8-ui-v1',256),SHA2('pdk-8-components-v1',256),SHA2('poker01-prefab|pdk-room-v1',256),
 'ACTIVE',100,1,'Publish existing native PDK as Chengdu catalog play',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE status='ACTIVE',rollout_percent=100,validated_at=CURRENT_TIMESTAMP(3),activated_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(800000001,'CN-51-01',100,'ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
 game_id,region_code,play_version,index_generation,release_id,
 component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
 lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(
 8,'CN-51-01','1.0.0',1,800000001,
 JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@1.0.0'),
 JSON_OBJECT('seatLimit',JSON_OBJECT('min',2,'max',4)),
 JSON_OBJECT(
   'scene','GameRoom2D','bundle','poker01-prefab','playFamily','poker:pao-de-kuai',
   'smallSettleTemplate','SmallSettleTpl_pdk_00','bigSettleTemplate','BigSettleTpl_00',
   'fields',JSON_ARRAY()),
 SHA2('pdk-8-index-v1',256),SHA2('poker01-prefab|pdk-room-v1',256),
 'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),lifecycle_state='ACTIVE',validated_at=CURRENT_TIMESTAMP(3),activated_at=CURRENT_TIMESTAMP(3),retired_at=NULL;

INSERT INTO aoo_compiled_index_active(
 game_id,region_code,play_version,index_generation,release_id,cache_epoch,
 activated_by,activation_reason,activated_at)
VALUES(8,'CN-51-01','1.0.0',1,800000001,1,1,'Publish Chengdu native PDK catalog',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE play_version='1.0.0',index_generation=1,release_id=800000001,cache_epoch=cache_epoch+1,activated_by=1,activation_reason='Publish Chengdu native PDK catalog',activated_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(800000001,8,'1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE priority=100,status='ACTIVE';

INSERT INTO aoo_room_cost_policy(
 game_id,play_version,region_code,round_count,player_count,payer_mode,
 currency_code,cost_minor,policy_version,content_hash,status)
SELECT 8,'1.0.0','CN-51-01',round_count,player_count,'OWNER','ROOM_CARD',0,1,
       SHA2(CONCAT('8|1.0.0|CN-51-01|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 UNION ALL SELECT 16) rounds
CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 UNION ALL SELECT 4) players
WHERE TRUE
ON DUPLICATE KEY UPDATE cost_minor=0,content_hash=VALUES(content_hash),status='ACTIVE';
