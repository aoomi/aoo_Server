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
  (8,'CD201','成都跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:CD201',1,'ACTIVE'),
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

-- NJ201 local classification. Immutable release/index publication belongs to Flyway;
-- local seed must never reactivate an obsolete release after migrations complete.
INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth,status) VALUES
('CN-51-10','CN-51','CITY','CN','5110','内江','/CN/CN-51/CN-51-10',2,'ACTIVE')
ON DUPLICATE KEY UPDATE parent_region_code=VALUES(parent_region_code),region_type=VALUES(region_type),display_name=VALUES(display_name),path=VALUES(path),depth=VALUES(depth),status='ACTIVE';
INSERT INTO aoo_game_family(family_code,category_code,display_name,status) VALUES('poker:pao-de-kuai','POKER','跑得快','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE';
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status) VALUES(629,'NJ201','内江跑得快','POKER','poker:pao-de-kuai','com.aoo.bcg.poker.PdkGameProvider:NJ201',1,'ACTIVE') ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),provider_key=VALUES(provider_key),display_name=VALUES(display_name),family_code=VALUES(family_code),status='ACTIVE';
INSERT INTO aoo_game_region(game_id,region_code,availability,priority) VALUES(629,'CN-51-10','AVAILABLE',1) ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;
UPDATE aoo_game_region SET availability='RETIRED' WHERE game_id=629 AND region_code='CN-51';
INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by) VALUES(629,'legacy-equivalent-1','CN-51-10',1,1,1,SHA2('com.aoo.bcg.poker.PdkGameProvider:NJ201@legacy-equivalent-1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1) ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),status='ACTIVE',content_hash=VALUES(content_hash);
INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status) VALUES(629000001,629,'legacy-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',1,'ACTIVE') ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),status='ACTIVE',priority=1;

-- Explicit free local NJ201 policies. Region is a play classification only; all variants share one price.
INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 629,'legacy-equivalent-1','CN-51',round_count,player_count,'OWNER','ROOM_CARD',0,1,SHA2(CONCAT('629|legacy-equivalent-1|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 UNION ALL SELECT 12 UNION ALL SELECT 16) rounds
CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 UNION ALL SELECT 4) players
WHERE 1=1
ON DUPLICATE KEY UPDATE currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';

-- NJ201 is published at the city scope. Billing does not include region in its
-- lookup key, so the historical province rows must remain retired.
UPDATE aoo_room_cost_policy
SET status='RETIRED',policy_version=policy_version+1,
    content_hash=SHA2(CONCAT(content_hash,'|retired-for-CN-51-10'),256)
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND region_code='CN-51'
  AND status='ACTIVE';
