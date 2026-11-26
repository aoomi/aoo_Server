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

-- MJ-015 local-only canonical Chengdu Mahjong publication. The room workbook proves only the
-- authoritative Mahjong fields setCount/playerNum; every other XQP candidate
-- remains unpublished and is therefore rejected by Hall as an unknown field.
SET @chengdu1001_release_id = 100120261003;
SET @chengdu1001_generation = 2026100203;
SET @chengdu1001_play_version = 'chengdu1001-v1.0.0';
SET @chengdu1001_region = 'CN-51-01';
-- This is the read-only canonical Chengdu workbook SHA-256. It remains a
-- source identity, not a substitute for any JSON snapshot content hash.
SET @chengdu1001_source_hash = '9ca9df7a061bebadbe31ccd2acfc169bf968076c6dc459baafe7cf262dc0c87c';
SET @chengdu1001_ui_fields = JSON_ARRAY(
  JSON_OBJECT('key','setCount','label','局数','order',10,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',8,'options',JSON_ARRAY(JSON_OBJECT('label','8局','value',8,'order',10,'disabled',false),JSON_OBJECT('label','12局','value',12,'order',20,'disabled',false),JSON_OBJECT('label','16局','value',16,'order',30,'disabled',false))),
  JSON_OBJECT('key','playerNum','label','人数','order',20,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',4,'options',JSON_ARRAY(JSON_OBJECT('label','2人','value',2,'order',10,'disabled',false),JSON_OBJECT('label','3人','value',3,'order',20,'disabled',false),JSON_OBJECT('label','4人','value',4,'order',30,'disabled',false)))
);
SET @chengdu1001_validator = JSON_OBJECT('gameId',1001,'gameCode','chengdu1001','playVersion',@chengdu1001_play_version,'roomMeasureKind','ROUND_COUNT','source','成都血战麻将.xlsx','sourceHash',@chengdu1001_source_hash,'authority','canonical Chengdu Mahjong server chain','fields',JSON_EXTRACT(@chengdu1001_ui_fields,'$'));
SET @chengdu1001_ui = JSON_OBJECT('bundle','games-mahjong','scene','GameRoom2D','playFamily','mahjong:xue-zhan','roomRuleSourceHash',@chengdu1001_source_hash,'roomRuleViewHash',@chengdu1001_source_hash,'fields',JSON_EXTRACT(@chengdu1001_ui_fields,'$'));
SET @chengdu1001_catalog_snapshot = JSON_OBJECT('gameId',1001,'gameCode','chengdu1001','playVersion',@chengdu1001_play_version,'family','mahjong:xue-zhan');
SET @chengdu1001_component_snapshot = JSON_OBJECT('runtime','MahjongCatalogRuntimeRegistry','profile','chengdu1001');
SET @chengdu1001_component_chain = JSON_ARRAY(CONCAT('com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry@',@chengdu1001_play_version));
SET @chengdu1001_bundle_snapshot = JSON_OBJECT(
  'catalogSnapshot',JSON_EXTRACT(@chengdu1001_catalog_snapshot,'$'),
  'ruleValidator',JSON_EXTRACT(@chengdu1001_validator,'$'),
  'uiSchema',JSON_EXTRACT(@chengdu1001_ui,'$'),
  'componentSnapshot',JSON_EXTRACT(@chengdu1001_component_snapshot,'$'),
  'componentChain',JSON_EXTRACT(@chengdu1001_component_chain,'$')
);

INSERT INTO aoo_game_family(family_code,category_code,display_name,status)
VALUES('mahjong:xue-zhan','MAHJONG','血战麻将','ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),status='ACTIVE';
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(1001,'chengdu1001','成都麻将','MAHJONG','mahjong:xue-zhan','catalog.chengdu1001',1,'ACTIVE')
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),family_code=VALUES(family_code),provider_key=VALUES(provider_key),status='ACTIVE';
INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(1001,@chengdu1001_region,'AVAILABLE',1)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;
INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(1001,@chengdu1001_play_version,@chengdu1001_region,1,1,1,SHA2('chengdu1001|chengdu1001-v1.0.0|setCount,playerNum',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),status='ACTIVE',activated_at=COALESCE(activated_at,CURRENT_TIMESTAMP(3));
INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(1001000001,1001,@chengdu1001_play_version,'ws://127.0.0.1:8080/api/v2/gateway/ws',1,'ACTIVE')
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),priority=1,status='ACTIVE';

-- Room-card pricing is seeded only from database/bootstrap/room-card-pricing.
-- This catalog seed must not create a second pricing authority.

INSERT IGNORE INTO aoo_game_release(release_id,game_id,play_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(@chengdu1001_release_id,1001,@chengdu1001_play_version,@chengdu1001_source_hash,2,'REGIONAL',@chengdu1001_catalog_snapshot,@chengdu1001_validator,@chengdu1001_ui,@chengdu1001_component_snapshot,SHA2(CAST(@chengdu1001_catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256),SHA2(CAST(@chengdu1001_validator AS CHAR CHARACTER SET utf8mb4),256),@chengdu1001_source_hash,SHA2(CAST(@chengdu1001_component_chain AS CHAR CHARACTER SET utf8mb4),256),SHA2(CAST(@chengdu1001_bundle_snapshot AS CHAR CHARACTER SET utf8mb4),256),'ACTIVE',100,1,'MJ-015 local-only canonical Chengdu Mahjong room-create schema',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));
INSERT IGNORE INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(@chengdu1001_release_id,@chengdu1001_region,100,'ACTIVE');
INSERT IGNORE INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(1001,@chengdu1001_region,@chengdu1001_play_version,@chengdu1001_generation,@chengdu1001_release_id,@chengdu1001_component_chain,@chengdu1001_validator,@chengdu1001_ui,SHA2(CONCAT('chengdu1001|',@chengdu1001_generation,'|',CAST(@chengdu1001_validator AS CHAR CHARACTER SET utf8mb4)),256),SHA2(CAST(@chengdu1001_bundle_snapshot AS CHAR CHARACTER SET utf8mb4),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));
INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
VALUES(1001,@chengdu1001_region,@chengdu1001_play_version,@chengdu1001_generation,@chengdu1001_release_id,1,1,'MJ-015 activate canonical Chengdu Mahjong authoritative schema',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE cache_epoch=IF(release_id=VALUES(release_id) AND index_generation=VALUES(index_generation),cache_epoch,cache_epoch+1),play_version=VALUES(play_version),index_generation=VALUES(index_generation),release_id=VALUES(release_id),activated_by=1,activation_reason=VALUES(activation_reason),activated_at=CURRENT_TIMESTAMP(3);

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

-- Room-card pricing is seeded only from database/bootstrap/room-card-pricing;
-- region classification above is intentionally not a pricing dimension.
