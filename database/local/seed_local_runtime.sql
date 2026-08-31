-- Local development only. Production publication remains management-controlled.
INSERT INTO aoo_server_directory
  (id,server_code,display_name,region,public_endpoint,platform,channel,state,weight,rollout_percent,directory_revision,visible_from)
VALUES
  (900000001,'local-development','本地开发服','local','ws://127.0.0.1:8080/api/v2/gateway/ws','*','*','ACTIVE',100,100,1,TIMESTAMP('2026-01-01 00:00:00.000'))
ON DUPLICATE KEY UPDATE
  display_name=VALUES(display_name),public_endpoint=VALUES(public_endpoint),state='ACTIVE',directory_revision=directory_revision+1;

INSERT INTO aoo_client_feature_flag
  (flag_key,platform,channel,enabled,flag_value,rollout_percent,enabled_from)
VALUES
  ('wechat_login','*','*',FALSE,NULL,100,TIMESTAMP('2026-01-01 00:00:00.000'))
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),flag_value=VALUES(flag_value),updated_at=CURRENT_TIMESTAMP(3);

-- Formal local publication generated from enabled NATIONAL catalog rows.
INSERT INTO aoo_game_family (family_code,category_code,display_name,status)
VALUES
  ('MAHJONG_STANDARD','MAHJONG','标准麻将','ACTIVE'),
  ('MAHJONG_LAI_ZI','MAHJONG','癞子麻将','ACTIVE'),
  ('POKER_PAO_DE_KUAI','POKER','跑得快','ACTIVE'),
  ('POKER_TRICK_TAKING','POKER','扑克墩牌','ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),status='ACTIVE';

INSERT INTO aoo_game_catalog
  (game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES
  (1,'hzmj','HZMJ','MAHJONG','MAHJONG_STANDARD','catalog.hzmj',1,'ACTIVE'),
  (2,'sss','SSS','POKER','POKER_PAO_DE_KUAI','catalog.sss',1,'ACTIVE'),
  (8,'pdk','PDK','POKER','POKER_PAO_DE_KUAI','catalog.pdk',1,'ACTIVE'),
  (27,'gdy','GDY','POKER','POKER_TRICK_TAKING','catalog.gdy',1,'ACTIVE'),
  (31,'ddz','DDZ','POKER','POKER_PAO_DE_KUAI','catalog.ddz',1,'ACTIVE'),
  (130,'erddz','ERDDZ','POKER','POKER_PAO_DE_KUAI','catalog.erddz',1,'ACTIVE'),
  (588,'dphmj','DPHMJ','MAHJONG','MAHJONG_LAI_ZI','catalog.dphmj',1,'ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),family_code=VALUES(family_code),status='ACTIVE';

INSERT INTO aoo_game_region (game_id,region_code,availability,priority)
VALUES (1,'GLOBAL','AVAILABLE',100),(2,'GLOBAL','AVAILABLE',100),(8,'GLOBAL','AVAILABLE',100),
       (27,'GLOBAL','AVAILABLE',100),(31,'GLOBAL','AVAILABLE',100),(130,'GLOBAL','AVAILABLE',100),
       (588,'GLOBAL','AVAILABLE',100)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority);

INSERT INTO aoo_play_version
  (game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
   component_schema_version,content_hash,status,activated_at,created_by)
VALUES
  (1,'1.0.0','GLOBAL',1,1,1,SHA2('local:hzmj:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (2,'1.0.0','GLOBAL',1,1,1,SHA2('local:sss:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (8,'1.0.0','GLOBAL',1,1,1,SHA2('local:pdk:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (27,'1.0.0','GLOBAL',1,1,1,SHA2('local:gdy:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (31,'1.0.0','GLOBAL',1,1,1,SHA2('local:ddz:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (130,'1.0.0','GLOBAL',1,1,1,SHA2('local:erddz:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
  (588,'1.0.0','GLOBAL',1,1,1,SHA2('local:dphmj:1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE status='ACTIVE',activated_at=COALESCE(activated_at,CURRENT_TIMESTAMP(3));

-- Canonical gameplay-classification aliases retained for catalog search migration.
INSERT INTO aoo_region_alias (source_system,source_region_code,region_code,mapping_status,verified_at) VALUES
('legacy-city-id','1270000','CN-12','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1070000','CN-13','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1160000','CN-14','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1280000','CN-15','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1190000','CN-21','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1150000','CN-22','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1300000','CN-23','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1300100','CN-23-01','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1220000','CN-31','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1020000','CN-32','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1080000','CN-33','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1110000','CN-34','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1010000','CN-35','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1030000','CN-36','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1060000','CN-37','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1100000','CN-41','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1090000','CN-42','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1140000','CN-43','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1140700','CN-43-11','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1050000','CN-44','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1130000','CN-45','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1240000','CN-46','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1200000','CN-50','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1040000','CN-51','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1040900','CN-51-01','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1230000','CN-52','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1260000','CN-53','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1320000','CN-54','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1210000','CN-61','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1180000','CN-62','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1290000','CN-64','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1170000','CN-65','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-city-id','1120000','CN-71','VERIFIED',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE region_code=VALUES(region_code),mapping_status='VERIFIED',verified_at=VALUES(verified_at);

-- NJPDK 629 local formal publication, derived from NJPDKGameProvider descriptor.
INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth,status) VALUES
('CN-51-10','CN-51','CITY','CN','5110','内江','/CN/CN-51/CN-51-10',2,'ACTIVE')
ON DUPLICATE KEY UPDATE parent_region_code=VALUES(parent_region_code),region_type=VALUES(region_type),display_name=VALUES(display_name),path=VALUES(path),depth=VALUES(depth),status='ACTIVE';
INSERT INTO aoo_game_family(family_code,category_code,display_name,status) VALUES('poker:pao-de-kuai','POKER','跑得快','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE';
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status) VALUES(629,'njpdk','内江跑得快','POKER','poker:pao-de-kuai','business.global.pk.njpdk.NJPDKGameProvider',1,'ACTIVE') ON DUPLICATE KEY UPDATE provider_key=VALUES(provider_key),display_name=VALUES(display_name),family_code=VALUES(family_code),status='ACTIVE';
INSERT INTO aoo_game_region(game_id,region_code,availability,priority) VALUES(629,'CN-51-10','AVAILABLE',1) ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;
UPDATE aoo_game_region SET availability='RETIRED' WHERE game_id=629 AND region_code='CN-51';
INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by) VALUES(629,'legacy-equivalent-1','CN-51',1,1,1,SHA2('business.global.pk.njpdk.NJPDKGameProvider:legacy-equivalent-1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1) ON DUPLICATE KEY UPDATE status='ACTIVE',content_hash=VALUES(content_hash);
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at) VALUES(629000004,629,'legacy-equivalent-1',4,'REGIONAL',JSON_OBJECT('provider','business.global.pk.njpdk.NJPDKGameProvider','gameId',629,'family','poker:pao-de-kuai'),JSON_OBJECT('family','poker:pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','pdk-common-room','scene','GameRoom2D','playerCounts',JSON_ARRAY(2,3,4),'playFamily','poker:pao-de-kuai','bigSettleTemplate','BigSettleTpl_110'),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('njpdk-catalog-v5',256),SHA2('njpdk-rules-v5',256),SHA2('pdk-common-room|njpdk-ui-v5-settlement-family',256),SHA2('njpdk-provider-v3',256),SHA2('pdk-common-room|room-index-v5',256),'ACTIVE',100,1,'Publish authoritative NJPDK family and settlement contract',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE release_id=release_id;
INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(629000004,'CN-51',100,'ACTIVE') ON DUPLICATE KEY UPDATE release_id=release_id;
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at) VALUES(629,'CN-51','legacy-equivalent-1',4,629000004,JSON_ARRAY('business.global.pk.njpdk.NJPDKGameProvider@legacy-equivalent-1'),JSON_OBJECT(),JSON_OBJECT('bundle','pdk-common-room','scene','GameRoom2D','fields',JSON_ARRAY(),'playFamily','poker:pao-de-kuai','bigSettleTemplate','BigSettleTpl_110'),SHA2('njpdk-index-v5-settlement-family',256),SHA2('pdk-common-room|room-index-v5',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE game_id=game_id;
INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason) VALUES(629,'CN-51','legacy-equivalent-1',4,629000004,1,1,'Publish authoritative 2/3/4-player NJPDK layouts') ON DUPLICATE KEY UPDATE game_id=game_id;
UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3)) WHERE game_id=629 AND region_code='CN-51' AND play_version='legacy-equivalent-1' AND index_generation<4 AND lifecycle_state='ACTIVE';
UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3)) WHERE game_id=629 AND play_version='legacy-equivalent-1' AND release_version<4 AND status='ACTIVE';
UPDATE aoo_game_release r
JOIN (SELECT MAX(release_version) AS release_version FROM aoo_game_release WHERE game_id=629 AND play_version='legacy-equivalent-1' AND status='ACTIVE') latest
  ON r.game_id=629 AND r.play_version='legacy-equivalent-1' AND r.release_version<latest.release_version
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE';
UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED'
WHERE r.game_id=629 AND r.status='RETIRED' AND rr.status='ACTIVE';
UPDATE aoo_compiled_room_create_index i
JOIN aoo_game_release r ON r.release_id=i.release_id
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.game_id=629 AND r.status='RETIRED' AND i.lifecycle_state='ACTIVE';
UPDATE aoo_compiled_index_active a
JOIN aoo_game_release r ON r.game_id=a.game_id AND r.play_version=a.play_version AND r.status='ACTIVE'
JOIN aoo_compiled_room_create_index i ON i.release_id=r.release_id AND i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.lifecycle_state='ACTIVE'
SET a.index_generation=i.index_generation,a.release_id=r.release_id,a.cache_epoch=a.cache_epoch+1,
    a.activated_by=1,a.activation_reason='Select latest authoritative local NJPDK publication',a.activated_at=CURRENT_TIMESTAMP(3)
WHERE a.game_id=629;
INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status) VALUES(629000001,629,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',1,'ACTIVE') ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),status='ACTIVE',priority=1;

-- Explicit free local NJPDK policies. Region is a play classification only; all variants share one price.
INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 629,'legacy-equivalent-1','CN-51',round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('629|legacy-equivalent-1|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 UNION ALL SELECT 12 UNION ALL SELECT 16) rounds
CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 UNION ALL SELECT 4) players
WHERE 1=1
ON DUPLICATE KEY UPDATE currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';
