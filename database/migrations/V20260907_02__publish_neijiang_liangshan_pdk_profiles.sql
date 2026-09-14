-- Publish Neijiang and Liangshan as independent regional identities while both
-- execute through the single common PDK provider/state machine.

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,
    subdivision_code,display_name,path,depth,status)
VALUES('CN-51-34','CN-51','CITY','CN','5134','凉山','/CN/CN-51/CN-51-34',2,'ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),status='ACTIVE';

INSERT INTO aoo_region_alias(source_system,source_region_code,region_code,mapping_status,verified_at)
VALUES('canonical-adcode','513400','CN-51-34','VERIFIED',CURRENT_TIMESTAMP(3)),
      ('xqp-area','9','CN-51-34','VERIFIED',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE region_code=VALUES(region_code),mapping_status='VERIFIED',
    verified_at=VALUES(verified_at);

UPDATE aoo_game_catalog
SET provider_key='com.aoo.bcg.poker.PdkGameProvider:njpdk',
    display_name='内江跑得快',family_code='poker:pao-de-kuai',row_version=row_version+1
WHERE game_id=629 AND game_code='njpdk';

INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,
    provider_key,catalog_schema_version,status)
VALUES(90005,'lspdk','凉山跑得快','POKER','poker:pao-de-kuai',
       'com.aoo.bcg.poker.PdkGameProvider:lspdk',1,'ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),family_code=VALUES(family_code),
    provider_key=VALUES(provider_key),status='ACTIVE',row_version=row_version+1;

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(90005,'CN-51-34','AVAILABLE',1)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;

INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,
    ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(90005,'xqp-equivalent-1','CN-51-34',1,1,1,
       SHA2('lspdk|xqp-area-9|type-5|common-pdk-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),status='ACTIVE',
    activated_at=COALESCE(activated_at,CURRENT_TIMESTAMP(3)),row_version=row_version+1;

INSERT INTO aoo_room_rule_definition(game_id,play_version,rule_key,rule_kind,value_type,
    required_value,default_value,minimum_value,maximum_value,description,ordinal,
    schema_version,content_hash,status)
VALUES
 (90005,'xqp-equivalent-1','playerCount','ROOM','INTEGER',1,CAST('2' AS JSON),2,4,'凉山跑得快人数',10,1,SHA2('lspdk.playerCount.2.3.4',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','roundCount','ROOM','INTEGER',1,CAST('8' AS JSON),8,16,'凉山跑得快局数',20,1,SHA2('lspdk.roundCount.8.12.16',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','operationTime','FLOW','INTEGER',1,CAST('10' AS JSON),10,20,'操作超时秒数',30,1,SHA2('lspdk.operationTime.10.15.20',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','dealCardCount','DECK','INTEGER',1,CAST('8' AS JSON),8,10,'每人8张使用7-A牌堆；10张使用5-A牌堆',40,1,SHA2('lspdk.dealCardCount.8.10',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','jinHuaScore','SCORING','ENUM',1,JSON_QUOTE('no_compare'),NULL,NULL,'金花分值或不比较',50,1,SHA2('lspdk.jinHuaScore.1.2.3.4.5.none',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','robDealerRule','FLOW','ENUM',1,JSON_QUOTE('dealer_first'),NULL,NULL,'抢庄顺序与首局规则',60,1,SHA2('lspdk.robDealerRule.v1',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','playRule','HAND','LIST',0,JSON_ARRAY('compare_attachments','triple_with_one','four_ace_rank','full_consecutive_pairs'),NULL,NULL,'凉山地区牌型开关',70,1,SHA2('lspdk.playRule.v1',256),'ACTIVE'),
 (90005,'xqp-equivalent-1','roomRestriction','ROOM','LIST',0,JSON_ARRAY('ip_limit','timeout_auto_play','distance_warning','chat_muted'),NULL,NULL,'房间安全与互动开关',80,1,SHA2('lspdk.roomRestriction.v1',256),'ACTIVE')
ON DUPLICATE KEY UPDATE status='ACTIVE';

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
    ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,
    validated_at,activated_at)
VALUES(90005000001,90005,'xqp-equivalent-1',1,'REGIONAL',
 JSON_OBJECT('gameId',90005,'gameCode','lspdk','displayName','凉山跑得快',
   'family','poker:pao-de-kuai','regionCode','CN-51-34','xqpArea',9,'xqpGameType',5,
   'provider','com.aoo.bcg.poker.PdkGameProvider:lspdk'),
 JSON_OBJECT('family','poker:pao-de-kuai','profile','liangshan','playerCounts',JSON_ARRAY(2,3,4),
   'roundCounts',JSON_ARRAY(8,12,16),'operationTimes',JSON_ARRAY(10,15,20),
   'dealCardCounts',JSON_ARRAY(8,10),'deckModes',JSON_ARRAY('LS_7_TO_ACE','LS_5_TO_ACE'),
   'jinHuaScores',JSON_ARRAY(1,2,3,4,5,'no_compare'),
   'robDealerRules',JSON_ARRAY('dealer_first','dealer_last','first_round_no_compete','no_compete')),
 JSON_OBJECT('bundle','pdk-common-room','scene','GameRoom2D','playFamily','poker:pao-de-kuai',
   'regionalProfile','PDK/LSPDK/Code/LiangshanPdkRoomProfile','playerCounts',JSON_ARRAY(2,3,4)),
 JSON_OBJECT('providerVersion','xqp-equivalent-1','regionalProvider','native-pdk-liangshan'),
 SHA2('lspdk-catalog-v1',256),SHA2('lspdk-rules-v1',256),SHA2('lspdk-ui-v1',256),
 SHA2('lspdk-components-v1',256),SHA2('pdk-common-room|lspdk-v1',256),
 'ACTIVE',100,1,'Publish XQP-equivalent Liangshan profile on common PDK',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(90005000001,'CN-51-34',100,'ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
    release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
    lifecycle_state,validated_at,activated_at)
VALUES(90005,'CN-51-34','xqp-equivalent-1',1,90005000001,
 JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider:lspdk@xqp-equivalent-1'),
 JSON_OBJECT('playerCount',JSON_OBJECT('enum',JSON_ARRAY(2,3,4)),
   'roundCount',JSON_OBJECT('enum',JSON_ARRAY(8,12,16)),
   'operationTime',JSON_OBJECT('enum',JSON_ARRAY(10,15,20)),
   'dealCardCount',JSON_OBJECT('enum',JSON_ARRAY(8,10)),
   'dependencies',JSON_ARRAY(
     JSON_OBJECT('when',JSON_OBJECT('dealCardCount',8),'deckMode','LS_7_TO_ACE','configuredFourRank',7),
     JSON_OBJECT('when',JSON_OBJECT('dealCardCount',10),'deckMode','LS_5_TO_ACE','configuredFourRank',5),
     JSON_OBJECT('when',JSON_OBJECT('robDealerRule','dealer_last'),'requires','competeDealerEnabled'),
     JSON_OBJECT('when',JSON_OBJECT('robDealerRule','first_round_no_compete'),'requires','competeDealerEnabled'))),
 JSON_OBJECT('bundle','pdk-common-room','scene','GameRoom2D','regionalProfile','lspdk',
   'fields',JSON_ARRAY(
     JSON_OBJECT('key','playerCount','title','人数','control','RADIO','options',JSON_ARRAY(2,3,4),'default',2),
     JSON_OBJECT('key','roundCount','title','局数','control','RADIO','options',JSON_ARRAY(8,12,16),'default',8),
     JSON_OBJECT('key','operationTime','title','操作时间','control','RADIO','options',JSON_ARRAY(10,15,20),'default',10),
     JSON_OBJECT('key','dealCardCount','title','发牌张数','control','RADIO','options',JSON_ARRAY(8,10),'default',8),
     JSON_OBJECT('key','jinHuaScore','title','比金花','control','RADIO','options',JSON_ARRAY(1,2,3,4,5,'no_compare'),'default','no_compare'),
     JSON_OBJECT('key','robDealerRule','title','抢庄','control','RADIO','options',JSON_ARRAY('dealer_first','dealer_last','first_round_no_compete','no_compete'),'default','dealer_first'),
     JSON_OBJECT('key','playRule','title','玩法','control','CHECKBOX'),
     JSON_OBJECT('key','roomRestriction','title','其他','control','CHECKBOX'))),
 SHA2('lspdk-index-v1',256),SHA2('pdk-common-room|lspdk-v1',256),
 'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,
    release_id,cache_epoch,activated_by,activation_reason)
VALUES(90005,'CN-51-34','xqp-equivalent-1',1,90005000001,1,1,
       'Publish Liangshan regional PDK profile')
ON DUPLICATE KEY UPDATE play_version=VALUES(play_version),index_generation=VALUES(index_generation),
    release_id=VALUES(release_id),cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason=VALUES(activation_reason),activated_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES(90005000001,90005,'xqp-equivalent-1','ws://127.0.0.1:8080/api/v2/gateway/ws',1,'ACTIVE')
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),priority=1,status='ACTIVE';

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,player_count,
    payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 90005,'xqp-equivalent-1','CN-51-34',round_count,player_count,'OWNER','ROOM_CARD',0,1,
       SHA2(CONCAT('90005|xqp-equivalent-1|CN-51-34|',round_count,'|',player_count,'|OWNER'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 12 UNION ALL SELECT 16) rounds
CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 UNION ALL SELECT 4) players
WHERE 1=1
ON DUPLICATE KEY UPDATE content_hash=VALUES(content_hash),status='ACTIVE';

-- Neijiang already has an immutable release chain. Publish a new generation
-- that names its regional profile/provider without changing the common room bundle.
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
    ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,
    validated_at,activated_at)
SELECT 629000008,629,'legacy-equivalent-1',8,r.release_scope,
 JSON_SET(r.catalog_snapshot,'$.gameCode','njpdk','$.regionCode','CN-51-10',
   '$.xqpArea',6,'$.xqpGameType',5,'$.provider','com.aoo.bcg.poker.PdkGameProvider:njpdk'),
 JSON_SET(r.rule_snapshot,'$.profile','neijiang','$.playerCounts',JSON_ARRAY(2,3),
   '$.cardsPerPlayer',16,'$.deckModes',JSON_ARRAY('CUT_40','STANDARD_48')),
 JSON_SET(r.ui_snapshot,'$.regionalProfile','PDK/NJPDK/Code/NeijiangPdkRoomProfile'),
 JSON_SET(r.component_snapshot,'$.regionalProvider','native-pdk-neijiang'),
 SHA2('njpdk-catalog-v8',256),SHA2('njpdk-rules-v8',256),SHA2('njpdk-ui-v8',256),
 SHA2('njpdk-components-v8',256),SHA2('pdk-common-room|njpdk-v8',256),
 'ACTIVE',100,r.created_by,'Publish Neijiang regional profile on common PDK',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release r
WHERE r.game_id=629 AND r.play_version='legacy-equivalent-1'
ORDER BY r.release_version DESC LIMIT 1
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(629000008,'CN-51-10',100,'ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
    release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
    lifecycle_state,validated_at,activated_at)
SELECT 629,'CN-51-10','legacy-equivalent-1',8,629000008,
 JSON_ARRAY('com.aoo.bcg.poker.PdkGameProvider:njpdk@legacy-equivalent-1'),
 JSON_OBJECT('playerCount',JSON_OBJECT('enum',JSON_ARRAY(2,3)),
   'roundCount',JSON_OBJECT('enum',JSON_ARRAY(8,12,16)),
   'cardsPerPlayer',JSON_OBJECT('const',16),
   'dependencies',JSON_ARRAY(
     JSON_OBJECT('when',JSON_OBJECT('playerCount',2,'playRule','remove_three_four'),'deckMode','CUT_40'),
     JSON_OBJECT('when',JSON_OBJECT('playerCount',3),'deckMode','STANDARD_48'))),
 JSON_SET(i.ui_schema,'$.regionalProfile','njpdk','$.playerCounts',JSON_ARRAY(2,3)),
 SHA2('njpdk-index-v8',256),SHA2('pdk-common-room|njpdk-v8',256),
 'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index i
WHERE i.game_id=629 AND i.play_version='legacy-equivalent-1'
ORDER BY i.index_generation DESC LIMIT 1
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id);

INSERT INTO aoo_compiled_index_active(game_id,region_code,play_version,index_generation,
    release_id,cache_epoch,activated_by,activation_reason)
SELECT 629,'CN-51-10','legacy-equivalent-1',8,629000008,1,1,
       'Publish Neijiang regional PDK profile'
FROM (SELECT 1) seed
ON DUPLICATE KEY UPDATE index_generation=VALUES(index_generation),release_id=VALUES(release_id),
    cache_epoch=cache_epoch+1,activated_by=1,activation_reason=VALUES(activation_reason),
    activated_at=CURRENT_TIMESTAMP(3);

DELETE FROM aoo_compiled_index_active
WHERE game_id=629 AND region_code<>'CN-51-10' AND play_version='legacy-equivalent-1';

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND play_version='legacy-equivalent-1'
  AND index_generation<8 AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND release_version<8
  AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED'
WHERE r.game_id=629 AND r.play_version='legacy-equivalent-1' AND r.release_version<8
  AND rr.status='ACTIVE';
