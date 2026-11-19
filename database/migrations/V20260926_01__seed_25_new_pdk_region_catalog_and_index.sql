-- Seed 25 independently published regional PDK identities. Regions are
-- classification tags only; all games continue to use the common PDK provider.

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth,status) VALUES
('CN-51-20','CN-51','CITY','CN','5120','资阳','/CN/CN-51/CN-51-20',2,'ACTIVE'),
('CN-36-11','CN-36','CITY','CN','3611','上饶','/CN/CN-36/CN-36-11',2,'ACTIVE'),
('CN-36-02','CN-36','CITY','CN','3602','景德镇','/CN/CN-36/CN-36-02',2,'ACTIVE'),
('CN-34-16','CN-34','CITY','CN','3416','亳州','/CN/CN-34/CN-34-16',2,'ACTIVE'),
('CN-36-08','CN-36','CITY','CN','3608','吉安','/CN/CN-36/CN-36-08',2,'ACTIVE'),
('CN-41-15','CN-41','CITY','CN','4115','信阳','/CN/CN-41/CN-41-15',2,'ACTIVE'),
('CN-42-09','CN-42','CITY','CN','4209','孝感','/CN/CN-42/CN-42-09',2,'ACTIVE'),
('CN-36-07','CN-36','CITY','CN','3607','赣州','/CN/CN-36/CN-36-07',2,'ACTIVE'),
('CN-43-12','CN-43','CITY','CN','4312','怀化','/CN/CN-43/CN-43-12',2,'ACTIVE'),
('CN-32-12','CN-32','CITY','CN','3212','泰州','/CN/CN-32/CN-32-12',2,'ACTIVE'),
('CN-32-04','CN-32','CITY','CN','3204','常州','/CN/CN-32/CN-32-04',2,'ACTIVE'),
('CN-36-09','CN-36','CITY','CN','3609','宜春','/CN/CN-36/CN-36-09',2,'ACTIVE'),
('CN-32-02','CN-32','CITY','CN','3202','无锡','/CN/CN-32/CN-32-02',2,'ACTIVE'),
('CN-34-18','CN-34','CITY','CN','3418','宣城','/CN/CN-34/CN-34-18',2,'ACTIVE'),
('CN-36-10','CN-36','CITY','CN','3610','抚州','/CN/CN-36/CN-36-10',2,'ACTIVE')
ON DUPLICATE KEY UPDATE parent_region_code=VALUES(parent_region_code),region_type=VALUES(region_type),country_code=VALUES(country_code),subdivision_code=VALUES(subdivision_code),display_name=VALUES(display_name),path=VALUES(path),depth=VALUES(depth);

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth,status) VALUES
('CN-51-34-33','CN-51-34','DISTRICT','CN','513433','冕宁','/CN/CN-51/CN-51-34/CN-51-34-33',3,'ACTIVE'),
('CN-51-20-21','CN-51-20','DISTRICT','CN','512021','安岳','/CN/CN-51/CN-51-20/CN-51-20-21',3,'ACTIVE'),
('CN-51-04','CN-51','CITY','CN','5104','攀枝花','/CN/CN-51/CN-51-04',2,'ACTIVE'),
('CN-36-11-29','CN-36-11','DISTRICT','CN','361129','万年','/CN/CN-36/CN-36-11/CN-36-11-29',3,'ACTIVE'),
('CN-36-02-81','CN-36-02','DISTRICT','CN','360281','乐平','/CN/CN-36/CN-36-02/CN-36-02-81',3,'ACTIVE'),
('CN-36-11-27','CN-36-11','DISTRICT','CN','361127','余干','/CN/CN-36/CN-36-11/CN-36-11-27',3,'ACTIVE'),
('CN-41-15-25','CN-41-15','DISTRICT','CN','411525','固始','/CN/CN-41/CN-41-15/CN-41-15-25',3,'ACTIVE'),
('CN-36-07-30','CN-36-07','DISTRICT','CN','360730','宁都','/CN/CN-36/CN-36-07/CN-36-07-30',3,'ACTIVE'),
('CN-41-15-23','CN-41-15','DISTRICT','CN','411523','新县','/CN/CN-41/CN-41-15/CN-41-15-23',3,'ACTIVE'),
('CN-41-15-27','CN-41-15','DISTRICT','CN','411527','淮滨','/CN/CN-41/CN-41-15/CN-41-15-27',3,'ACTIVE'),
('CN-41-15-26','CN-41-15','DISTRICT','CN','411526','潢川','/CN/CN-41/CN-41-15/CN-41-15-26',3,'ACTIVE'),
('CN-36-11-28','CN-36-11','DISTRICT','CN','361128','鄱阳','/CN/CN-36/CN-36-11/CN-36-11-28',3,'ACTIVE'),
('CN-32-04-13','CN-32-04','DISTRICT','CN','320413','金坛','/CN/CN-32/CN-32-04/CN-32-04-13',3,'ACTIVE'),
('CN-36-09-83','CN-36-09','DISTRICT','CN','360983','高安','/CN/CN-36/CN-36-09/CN-36-09-83',3,'ACTIVE'),
('CN-32-02-82','CN-32-02','DISTRICT','CN','320282','宜兴','/CN/CN-32/CN-32-02/CN-32-02-82',3,'ACTIVE')
ON DUPLICATE KEY UPDATE parent_region_code=VALUES(parent_region_code),region_type=VALUES(region_type),country_code=VALUES(country_code),subdivision_code=VALUES(subdivision_code),display_name=VALUES(display_name),path=VALUES(path),depth=VALUES(depth);


-- MN201 冕宁跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(631,'MN201','冕宁跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:MN201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(631,'mn201-v1.0.0','CN-51-34-33',1,1,1,SHA2('MN201|mn201-v1.0.0|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(631,'CN-51-34-33','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(631000001,631,'mn201-v1.0.0',1,'REGIONAL',JSON_OBJECT('gameId',631,'gameCode','MN201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:MN201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','mn201-v1.0.0'),SHA2('MN201|catalog-v1',256),SHA2('MN201|rules-v1',256),SHA2('MN201|ui-v1',256),SHA2('MN201|components-v1',256),SHA2('MN201|bundle-v1',256),'ACTIVE',100,1,'Seed MN201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 631000001,'CN-51-34-33',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=631000001 AND region_code='CN-51-34-33'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(631,'CN-51-34-33','mn201-v1.0.0',1,631000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@mn201-v1.0.0'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('MN201|index-v1',256),SHA2('MN201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 631,'CN-51-34-33','mn201-v1.0.0',1,631000001,1,1,'Seed MN201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=631 AND region_code='CN-51-34-33'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(631000001,631,'mn201-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 631,'mn201-v1.0.0','CN-51-34-33','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('631|mn201-v1.0.0|CN-51-34-33|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 631
    AND existing.play_version = 'mn201-v1.0.0'
    AND existing.region_code = 'CN-51-34-33'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- AY201 安岳跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(632,'AY201','安岳跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:AY201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(632,'ay201-v1.0.0','CN-51-20-21',1,1,1,SHA2('AY201|ay201-v1.0.0|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(632,'CN-51-20-21','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(632000001,632,'ay201-v1.0.0',1,'REGIONAL',JSON_OBJECT('gameId',632,'gameCode','AY201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:AY201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','ay201-v1.0.0'),SHA2('AY201|catalog-v1',256),SHA2('AY201|rules-v1',256),SHA2('AY201|ui-v1',256),SHA2('AY201|components-v1',256),SHA2('AY201|bundle-v1',256),'ACTIVE',100,1,'Seed AY201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 632000001,'CN-51-20-21',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=632000001 AND region_code='CN-51-20-21'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(632,'CN-51-20-21','ay201-v1.0.0',1,632000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@ay201-v1.0.0'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('AY201|index-v1',256),SHA2('AY201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 632,'CN-51-20-21','ay201-v1.0.0',1,632000001,1,1,'Seed AY201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=632 AND region_code='CN-51-20-21'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(632000001,632,'ay201-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 632,'ay201-v1.0.0','CN-51-20-21','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('632|ay201-v1.0.0|CN-51-20-21|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 632
    AND existing.play_version = 'ay201-v1.0.0'
    AND existing.region_code = 'CN-51-20-21'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- PZ201 攀枝花跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90006,'PZ201','攀枝花跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:PZ201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90006,'pz201-v1.0.0','CN-51-04',1,1,1,SHA2('PZ201|pz201-v1.0.0|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90006,'CN-51-04','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90006000001,90006,'pz201-v1.0.0',1,'REGIONAL',JSON_OBJECT('gameId',90006,'gameCode','PZ201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:PZ201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','pz201-v1.0.0'),SHA2('PZ201|catalog-v1',256),SHA2('PZ201|rules-v1',256),SHA2('PZ201|ui-v1',256),SHA2('PZ201|components-v1',256),SHA2('PZ201|bundle-v1',256),'ACTIVE',100,1,'Seed PZ201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90006000001,'CN-51-04',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90006000001 AND region_code='CN-51-04'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90006,'CN-51-04','pz201-v1.0.0',1,90006000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@pz201-v1.0.0'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('PZ201|index-v1',256),SHA2('PZ201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90006,'CN-51-04','pz201-v1.0.0',1,90006000001,1,1,'Seed PZ201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90006 AND region_code='CN-51-04'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90006000001,90006,'pz201-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90006,'pz201-v1.0.0','CN-51-04','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90006|pz201-v1.0.0|CN-51-04|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count UNION ALL SELECT 4 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90006
    AND existing.play_version = 'pz201-v1.0.0'
    AND existing.region_code = 'CN-51-04'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- WN201 万年跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90007,'WN201','万年跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:WN201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90007,'legacy-equivalent-1','CN-36-11-29',1,1,1,SHA2('WN201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90007,'CN-36-11-29','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90007000001,90007,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90007,'gameCode','WN201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:WN201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('WN201|catalog-v1',256),SHA2('WN201|rules-v1',256),SHA2('WN201|ui-v1',256),SHA2('WN201|components-v1',256),SHA2('WN201|bundle-v1',256),'ACTIVE',100,1,'Seed WN201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90007000001,'CN-36-11-29',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90007000001 AND region_code='CN-36-11-29'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90007,'CN-36-11-29','legacy-equivalent-1',1,90007000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('WN201|index-v1',256),SHA2('WN201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90007,'CN-36-11-29','legacy-equivalent-1',1,90007000001,1,1,'Seed WN201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90007 AND region_code='CN-36-11-29'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90007000001,90007,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90007,'legacy-equivalent-1','CN-36-11-29','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90007|legacy-equivalent-1|CN-36-11-29|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 4 round_count UNION ALL SELECT 8 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90007
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-11-29'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- LP201 乐平跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90008,'LP201','乐平跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:LP201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90008,'legacy-equivalent-1','CN-36-02-81',1,1,1,SHA2('LP201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90008,'CN-36-02-81','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90008000001,90008,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90008,'gameCode','LP201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:LP201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('LP201|catalog-v1',256),SHA2('LP201|rules-v1',256),SHA2('LP201|ui-v1',256),SHA2('LP201|components-v1',256),SHA2('LP201|bundle-v1',256),'ACTIVE',100,1,'Seed LP201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90008000001,'CN-36-02-81',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90008000001 AND region_code='CN-36-02-81'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90008,'CN-36-02-81','legacy-equivalent-1',1,90008000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('LP201|index-v1',256),SHA2('LP201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90008,'CN-36-02-81','legacy-equivalent-1',1,90008000001,1,1,'Seed LP201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90008 AND region_code='CN-36-02-81'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90008000001,90008,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90008,'legacy-equivalent-1','CN-36-02-81','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90008|legacy-equivalent-1|CN-36-02-81|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90008
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-02-81'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- BZ201 亳州跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90009,'BZ201','亳州跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:BZ201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90009,'legacy-equivalent-1','CN-34-16',1,1,1,SHA2('BZ201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90009,'CN-34-16','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90009000001,90009,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90009,'gameCode','BZ201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:BZ201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('BZ201|catalog-v1',256),SHA2('BZ201|rules-v1',256),SHA2('BZ201|ui-v1',256),SHA2('BZ201|components-v1',256),SHA2('BZ201|bundle-v1',256),'ACTIVE',100,1,'Seed BZ201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90009000001,'CN-34-16',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90009000001 AND region_code='CN-34-16'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90009,'CN-34-16','legacy-equivalent-1',1,90009000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('BZ201|index-v1',256),SHA2('BZ201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90009,'CN-34-16','legacy-equivalent-1',1,90009000001,1,1,'Seed BZ201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90009 AND region_code='CN-34-16'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90009000001,90009,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90009,'legacy-equivalent-1','CN-34-16','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90009|legacy-equivalent-1|CN-34-16|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 10 round_count UNION ALL SELECT 20 round_count UNION ALL SELECT 30 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90009
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-34-16'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- YG201 余干跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90010,'YG201','余干跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:YG201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90010,'legacy-equivalent-1','CN-36-11-27',1,1,1,SHA2('YG201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90010,'CN-36-11-27','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90010000001,90010,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90010,'gameCode','YG201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:YG201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('YG201|catalog-v1',256),SHA2('YG201|rules-v1',256),SHA2('YG201|ui-v1',256),SHA2('YG201|components-v1',256),SHA2('YG201|bundle-v1',256),'ACTIVE',100,1,'Seed YG201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90010000001,'CN-36-11-27',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90010000001 AND region_code='CN-36-11-27'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90010,'CN-36-11-27','legacy-equivalent-1',1,90010000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('YG201|index-v1',256),SHA2('YG201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90010,'CN-36-11-27','legacy-equivalent-1',1,90010000001,1,1,'Seed YG201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90010 AND region_code='CN-36-11-27'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90010000001,90010,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90010,'legacy-equivalent-1','CN-36-11-27','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90010|legacy-equivalent-1|CN-36-11-27|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 6 round_count UNION ALL SELECT 9 round_count UNION ALL SELECT 12 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90010
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-11-27'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- JA201 吉安跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90011,'JA201','吉安跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:JA201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90011,'legacy-equivalent-1','CN-36-08',1,1,1,SHA2('JA201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90011,'CN-36-08','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90011000001,90011,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90011,'gameCode','JA201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:JA201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('JA201|catalog-v1',256),SHA2('JA201|rules-v1',256),SHA2('JA201|ui-v1',256),SHA2('JA201|components-v1',256),SHA2('JA201|bundle-v1',256),'ACTIVE',100,1,'Seed JA201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90011000001,'CN-36-08',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90011000001 AND region_code='CN-36-08'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90011,'CN-36-08','legacy-equivalent-1',1,90011000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('JA201|index-v1',256),SHA2('JA201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90011,'CN-36-08','legacy-equivalent-1',1,90011000001,1,1,'Seed JA201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90011 AND region_code='CN-36-08'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90011000001,90011,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90011,'legacy-equivalent-1','CN-36-08','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90011|legacy-equivalent-1|CN-36-08|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90011
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-08'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- GS201 固始跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90012,'GS201','固始跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:GS201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90012,'legacy-equivalent-1','CN-41-15-25',1,1,1,SHA2('GS201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90012,'CN-41-15-25','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90012000001,90012,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90012,'gameCode','GS201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:GS201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('GS201|catalog-v1',256),SHA2('GS201|rules-v1',256),SHA2('GS201|ui-v1',256),SHA2('GS201|components-v1',256),SHA2('GS201|bundle-v1',256),'ACTIVE',100,1,'Seed GS201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90012000001,'CN-41-15-25',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90012000001 AND region_code='CN-41-15-25'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90012,'CN-41-15-25','legacy-equivalent-1',1,90012000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('GS201|index-v1',256),SHA2('GS201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90012,'CN-41-15-25','legacy-equivalent-1',1,90012000001,1,1,'Seed GS201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90012 AND region_code='CN-41-15-25'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90012000001,90012,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90012,'legacy-equivalent-1','CN-41-15-25','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90012|legacy-equivalent-1|CN-41-15-25|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90012
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-41-15-25'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- XG201 孝感跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90013,'XG201','孝感跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:XG201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90013,'legacy-equivalent-1','CN-42-09',1,1,1,SHA2('XG201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90013,'CN-42-09','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90013000001,90013,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90013,'gameCode','XG201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:XG201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('XG201|catalog-v1',256),SHA2('XG201|rules-v1',256),SHA2('XG201|ui-v1',256),SHA2('XG201|components-v1',256),SHA2('XG201|bundle-v1',256),'ACTIVE',100,1,'Seed XG201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90013000001,'CN-42-09',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90013000001 AND region_code='CN-42-09'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90013,'CN-42-09','legacy-equivalent-1',1,90013000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('XG201|index-v1',256),SHA2('XG201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90013,'CN-42-09','legacy-equivalent-1',1,90013000001,1,1,'Seed XG201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90013 AND region_code='CN-42-09'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90013000001,90013,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90013,'legacy-equivalent-1','CN-42-09','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90013|legacy-equivalent-1|CN-42-09|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90013
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-42-09'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- ND201 宁都跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90014,'ND201','宁都跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:ND201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90014,'legacy-equivalent-1','CN-36-07-30',1,1,1,SHA2('ND201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90014,'CN-36-07-30','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90014000001,90014,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90014,'gameCode','ND201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:ND201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('ND201|catalog-v1',256),SHA2('ND201|rules-v1',256),SHA2('ND201|ui-v1',256),SHA2('ND201|components-v1',256),SHA2('ND201|bundle-v1',256),'ACTIVE',100,1,'Seed ND201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90014000001,'CN-36-07-30',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90014000001 AND region_code='CN-36-07-30'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90014,'CN-36-07-30','legacy-equivalent-1',1,90014000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('ND201|index-v1',256),SHA2('ND201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90014,'CN-36-07-30','legacy-equivalent-1',1,90014000001,1,1,'Seed ND201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90014 AND region_code='CN-36-07-30'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90014000001,90014,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90014,'legacy-equivalent-1','CN-36-07-30','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90014|legacy-equivalent-1|CN-36-07-30|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90014
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-07-30'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- HH201 怀化跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90015,'HH201','怀化跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:HH201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90015,'legacy-equivalent-1','CN-43-12',1,1,1,SHA2('HH201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90015,'CN-43-12','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90015000001,90015,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90015,'gameCode','HH201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:HH201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('HH201|catalog-v1',256),SHA2('HH201|rules-v1',256),SHA2('HH201|ui-v1',256),SHA2('HH201|components-v1',256),SHA2('HH201|bundle-v1',256),'ACTIVE',100,1,'Seed HH201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90015000001,'CN-43-12',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90015000001 AND region_code='CN-43-12'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90015,'CN-43-12','legacy-equivalent-1',1,90015000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('HH201|index-v1',256),SHA2('HH201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90015,'CN-43-12','legacy-equivalent-1',1,90015000001,1,1,'Seed HH201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90015 AND region_code='CN-43-12'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90015000001,90015,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90015,'legacy-equivalent-1','CN-43-12','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90015|legacy-equivalent-1|CN-43-12|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90015
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-43-12'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- XX201 新县跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90016,'XX201','新县跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:XX201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90016,'legacy-equivalent-1','CN-41-15-23',1,1,1,SHA2('XX201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90016,'CN-41-15-23','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90016000001,90016,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90016,'gameCode','XX201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:XX201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('XX201|catalog-v1',256),SHA2('XX201|rules-v1',256),SHA2('XX201|ui-v1',256),SHA2('XX201|components-v1',256),SHA2('XX201|bundle-v1',256),'ACTIVE',100,1,'Seed XX201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90016000001,'CN-41-15-23',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90016000001 AND region_code='CN-41-15-23'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90016,'CN-41-15-23','legacy-equivalent-1',1,90016000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('XX201|index-v1',256),SHA2('XX201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90016,'CN-41-15-23','legacy-equivalent-1',1,90016000001,1,1,'Seed XX201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90016 AND region_code='CN-41-15-23'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90016000001,90016,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90016,'legacy-equivalent-1','CN-41-15-23','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90016|legacy-equivalent-1|CN-41-15-23|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count UNION ALL SELECT 4 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90016
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-41-15-23'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- JD201 景德镇跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90017,'JD201','景德镇跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:JD201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90017,'legacy-equivalent-1','CN-36-02',1,1,1,SHA2('JD201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90017,'CN-36-02','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90017000001,90017,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90017,'gameCode','JD201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:JD201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('JD201|catalog-v1',256),SHA2('JD201|rules-v1',256),SHA2('JD201|ui-v1',256),SHA2('JD201|components-v1',256),SHA2('JD201|bundle-v1',256),'ACTIVE',100,1,'Seed JD201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90017000001,'CN-36-02',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90017000001 AND region_code='CN-36-02'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90017,'CN-36-02','legacy-equivalent-1',1,90017000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('JD201|index-v1',256),SHA2('JD201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90017,'CN-36-02','legacy-equivalent-1',1,90017000001,1,1,'Seed JD201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90017 AND region_code='CN-36-02'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90017000001,90017,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90017,'legacy-equivalent-1','CN-36-02','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90017|legacy-equivalent-1|CN-36-02|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90017
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-02'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- JS201 江苏跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90018,'JS201','江苏跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:JS201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90018,'legacy-equivalent-1','CN-32',1,1,1,SHA2('JS201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90018,'CN-32','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90018000001,90018,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90018,'gameCode','JS201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:JS201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('JS201|catalog-v1',256),SHA2('JS201|rules-v1',256),SHA2('JS201|ui-v1',256),SHA2('JS201|components-v1',256),SHA2('JS201|bundle-v1',256),'ACTIVE',100,1,'Seed JS201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90018000001,'CN-32',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90018000001 AND region_code='CN-32'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90018,'CN-32','legacy-equivalent-1',1,90018000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('JS201|index-v1',256),SHA2('JS201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90018,'CN-32','legacy-equivalent-1',1,90018000001,1,1,'Seed JS201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90018 AND region_code='CN-32'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90018000001,90018,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90018,'legacy-equivalent-1','CN-32','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90018|legacy-equivalent-1|CN-32|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90018
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-32'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- HN201 河南跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90019,'HN201','河南跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:HN201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90019,'legacy-equivalent-1','CN-41',1,1,1,SHA2('HN201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90019,'CN-41','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90019000001,90019,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90019,'gameCode','HN201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:HN201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('HN201|catalog-v1',256),SHA2('HN201|rules-v1',256),SHA2('HN201|ui-v1',256),SHA2('HN201|components-v1',256),SHA2('HN201|bundle-v1',256),'ACTIVE',100,1,'Seed HN201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90019000001,'CN-41',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90019000001 AND region_code='CN-41'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90019,'CN-41','legacy-equivalent-1',1,90019000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('HN201|index-v1',256),SHA2('HN201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90019,'CN-41','legacy-equivalent-1',1,90019000001,1,1,'Seed HN201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90019 AND region_code='CN-41'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90019000001,90019,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90019,'legacy-equivalent-1','CN-41','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90019|legacy-equivalent-1|CN-41|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90019
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-41'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- TZ201 泰州跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90020,'TZ201','泰州跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:TZ201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90020,'legacy-equivalent-1','CN-32-12',1,1,1,SHA2('TZ201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90020,'CN-32-12','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90020000001,90020,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90020,'gameCode','TZ201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:TZ201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('TZ201|catalog-v1',256),SHA2('TZ201|rules-v1',256),SHA2('TZ201|ui-v1',256),SHA2('TZ201|components-v1',256),SHA2('TZ201|bundle-v1',256),'ACTIVE',100,1,'Seed TZ201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90020000001,'CN-32-12',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90020000001 AND region_code='CN-32-12'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90020,'CN-32-12','legacy-equivalent-1',1,90020000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('TZ201|index-v1',256),SHA2('TZ201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90020,'CN-32-12','legacy-equivalent-1',1,90020000001,1,1,'Seed TZ201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90020 AND region_code='CN-32-12'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90020000001,90020,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90020,'legacy-equivalent-1','CN-32-12','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90020|legacy-equivalent-1|CN-32-12|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90020
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-32-12'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- HB201 淮滨跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90021,'HB201','淮滨跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:HB201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90021,'legacy-equivalent-1','CN-41-15-27',1,1,1,SHA2('HB201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90021,'CN-41-15-27','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90021000001,90021,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90021,'gameCode','HB201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:HB201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('HB201|catalog-v1',256),SHA2('HB201|rules-v1',256),SHA2('HB201|ui-v1',256),SHA2('HB201|components-v1',256),SHA2('HB201|bundle-v1',256),'ACTIVE',100,1,'Seed HB201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90021000001,'CN-41-15-27',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90021000001 AND region_code='CN-41-15-27'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90021,'CN-41-15-27','legacy-equivalent-1',1,90021000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('HB201|index-v1',256),SHA2('HB201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90021,'CN-41-15-27','legacy-equivalent-1',1,90021000001,1,1,'Seed HB201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90021 AND region_code='CN-41-15-27'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90021000001,90021,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90021,'legacy-equivalent-1','CN-41-15-27','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90021|legacy-equivalent-1|CN-41-15-27|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90021
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-41-15-27'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- HC201 潢川跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90022,'HC201','潢川跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:HC201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90022,'legacy-equivalent-1','CN-41-15-26',1,1,1,SHA2('HC201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90022,'CN-41-15-26','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90022000001,90022,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90022,'gameCode','HC201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:HC201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('HC201|catalog-v1',256),SHA2('HC201|rules-v1',256),SHA2('HC201|ui-v1',256),SHA2('HC201|components-v1',256),SHA2('HC201|bundle-v1',256),'ACTIVE',100,1,'Seed HC201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90022000001,'CN-41-15-26',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90022000001 AND region_code='CN-41-15-26'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90022,'CN-41-15-26','legacy-equivalent-1',1,90022000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('HC201|index-v1',256),SHA2('HC201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90022,'CN-41-15-26','legacy-equivalent-1',1,90022000001,1,1,'Seed HC201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90022 AND region_code='CN-41-15-26'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90022000001,90022,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90022,'legacy-equivalent-1','CN-41-15-26','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90022|legacy-equivalent-1|CN-41-15-26|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90022
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-41-15-26'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- PY201 鄱阳跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90023,'PY201','鄱阳跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:PY201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90023,'legacy-equivalent-1','CN-36-11-28',1,1,1,SHA2('PY201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90023,'CN-36-11-28','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90023000001,90023,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90023,'gameCode','PY201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:PY201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('PY201|catalog-v1',256),SHA2('PY201|rules-v1',256),SHA2('PY201|ui-v1',256),SHA2('PY201|components-v1',256),SHA2('PY201|bundle-v1',256),'ACTIVE',100,1,'Seed PY201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90023000001,'CN-36-11-28',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90023000001 AND region_code='CN-36-11-28'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90023,'CN-36-11-28','legacy-equivalent-1',1,90023000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('PY201|index-v1',256),SHA2('PY201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90023,'CN-36-11-28','legacy-equivalent-1',1,90023000001,1,1,'Seed PY201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90023 AND region_code='CN-36-11-28'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90023000001,90023,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90023,'legacy-equivalent-1','CN-36-11-28','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90023|legacy-equivalent-1|CN-36-11-28|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90023
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-11-28'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- JT201 金坛跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90024,'JT201','金坛跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:JT201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90024,'legacy-equivalent-1','CN-32-04-13',1,1,1,SHA2('JT201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90024,'CN-32-04-13','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90024000001,90024,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90024,'gameCode','JT201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:JT201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('JT201|catalog-v1',256),SHA2('JT201|rules-v1',256),SHA2('JT201|ui-v1',256),SHA2('JT201|components-v1',256),SHA2('JT201|bundle-v1',256),'ACTIVE',100,1,'Seed JT201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90024000001,'CN-32-04-13',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90024000001 AND region_code='CN-32-04-13'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90024,'CN-32-04-13','legacy-equivalent-1',1,90024000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('JT201|index-v1',256),SHA2('JT201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90024,'CN-32-04-13','legacy-equivalent-1',1,90024000001,1,1,'Seed JT201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90024 AND region_code='CN-32-04-13'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90024000001,90024,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90024,'legacy-equivalent-1','CN-32-04-13','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90024|legacy-equivalent-1|CN-32-04-13|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90024
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-32-04-13'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- GA201 高安跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90025,'GA201','高安跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:GA201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90025,'legacy-equivalent-1','CN-36-09-83',1,1,1,SHA2('GA201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90025,'CN-36-09-83','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90025000001,90025,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90025,'gameCode','GA201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:GA201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('GA201|catalog-v1',256),SHA2('GA201|rules-v1',256),SHA2('GA201|ui-v1',256),SHA2('GA201|components-v1',256),SHA2('GA201|bundle-v1',256),'ACTIVE',100,1,'Seed GA201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90025000001,'CN-36-09-83',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90025000001 AND region_code='CN-36-09-83'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90025,'CN-36-09-83','legacy-equivalent-1',1,90025000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('GA201|index-v1',256),SHA2('GA201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90025,'CN-36-09-83','legacy-equivalent-1',1,90025000001,1,1,'Seed GA201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90025 AND region_code='CN-36-09-83'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90025000001,90025,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90025,'legacy-equivalent-1','CN-36-09-83','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90025|legacy-equivalent-1|CN-36-09-83|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90025
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-09-83'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- YX201 宜兴跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90026,'YX201','宜兴跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:YX201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90026,'legacy-equivalent-1','CN-32-02-82',1,1,1,SHA2('YX201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90026,'CN-32-02-82','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90026000001,90026,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90026,'gameCode','YX201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:YX201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('YX201|catalog-v1',256),SHA2('YX201|rules-v1',256),SHA2('YX201|ui-v1',256),SHA2('YX201|components-v1',256),SHA2('YX201|bundle-v1',256),'ACTIVE',100,1,'Seed YX201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90026000001,'CN-32-02-82',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90026000001 AND region_code='CN-32-02-82'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90026,'CN-32-02-82','legacy-equivalent-1',1,90026000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('YX201|index-v1',256),SHA2('YX201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90026,'CN-32-02-82','legacy-equivalent-1',1,90026000001,1,1,'Seed YX201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90026 AND region_code='CN-32-02-82'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90026000001,90026,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90026,'legacy-equivalent-1','CN-32-02-82','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90026|legacy-equivalent-1|CN-32-02-82|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90026
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-32-02-82'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- XC201 宣城跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90027,'XC201','宣城跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:XC201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90027,'legacy-equivalent-1','CN-34-18',1,1,1,SHA2('XC201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90027,'CN-34-18','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90027000001,90027,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90027,'gameCode','XC201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:XC201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('XC201|catalog-v1',256),SHA2('XC201|rules-v1',256),SHA2('XC201|ui-v1',256),SHA2('XC201|components-v1',256),SHA2('XC201|bundle-v1',256),'ACTIVE',100,1,'Seed XC201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90027000001,'CN-34-18',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90027000001 AND region_code='CN-34-18'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90027,'CN-34-18','legacy-equivalent-1',1,90027000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('XC201|index-v1',256),SHA2('XC201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90027,'CN-34-18','legacy-equivalent-1',1,90027000001,1,1,'Seed XC201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90027 AND region_code='CN-34-18'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90027000001,90027,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90027,'legacy-equivalent-1','CN-34-18','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90027|legacy-equivalent-1|CN-34-18|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 round_count UNION ALL SELECT 16 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90027
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-34-18'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');

-- FZ201 抚州跑得快
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(90028,'FZ201','抚州跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:FZ201',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),category_code=VALUES(category_code),family_code=VALUES(family_code),provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version);

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90028,'legacy-equivalent-1','CN-36-10',1,1,1,SHA2('FZ201|legacy-equivalent-1|regional-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90028,'CN-36-10','AVAILABLE',10)
ON DUPLICATE KEY UPDATE priority=VALUES(priority);

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(90028000001,90028,'legacy-equivalent-1',1,'REGIONAL',JSON_OBJECT('gameId',90028,'gameCode','FZ201','family','poker:pao-de-kuai','provider','com.aoo.bcg.poker.PdkGameProvider:FZ201'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('FZ201|catalog-v1',256),SHA2('FZ201|rules-v1',256),SHA2('FZ201|ui-v1',256),SHA2('FZ201|components-v1',256),SHA2('FZ201|bundle-v1',256),'ACTIVE',100,1,'Seed FZ201 regional PDK catalog',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),reason=VALUES(reason);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 90028000001,'CN-36-10',100,'ACTIVE' FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_game_release_region
  WHERE release_id=90028000001 AND region_code='CN-36-10'
);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(90028,'CN-36-10','legacy-equivalent-1',1,90028000001,JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider@legacy-equivalent-1'),JSON_OBJECT('fields',JSON_ARRAY()),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','playFamily','poker:pao-de-kuai','fields',JSON_ARRAY()),SHA2('FZ201|index-v1',256),SHA2('FZ201|bundle-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT 90028,'CN-36-10','legacy-equivalent-1',1,90028000001,1,1,'Seed FZ201 regional PDK catalog',CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_compiled_index_active
  WHERE game_id=90028 AND region_code='CN-36-10'
);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90028000001,90028,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),endpoint=VALUES(endpoint),priority=VALUES(priority);

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90028,'legacy-equivalent-1','CN-36-10','ROUND_COUNT',round_count,round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('90028|legacy-equivalent-1|CN-36-10|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 16 round_count UNION ALL SELECT 24 round_count) rounds CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 player_count) players
WHERE NOT EXISTS (
  SELECT 1 FROM aoo_room_cost_policy existing
  WHERE existing.game_id = 90028
    AND existing.play_version = 'legacy-equivalent-1'
    AND existing.region_code = 'CN-36-10'
    AND existing.measure_kind = 'ROUND_COUNT'
    AND existing.round_count = rounds.round_count
    AND existing.player_count = players.player_count
    AND existing.payer_mode = 'OWNER');
