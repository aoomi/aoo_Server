-- Publish CN298 rules compiled from the read-only workbook
-- Client/docs/开房规则表/牛牛/全国牛牛.xlsx.
-- Source SHA-256: 3bbcd398568c7d109835ac07e1114b81189aa8412e82a97b4f6c0ee315ac7ce7

SET @cn298_release_id = 502026091903;
SET @cn298_generation = 2026091903;

SET @cn298_validator = JSON_OBJECT(
  'gameCode','CN298','playVersion','cn298-v1.0.0','source','全国牛牛.xlsx',
  'sourceHash','3bbcd398568c7d109835ac07e1114b81189aa8412e82a97b4f6c0ee315ac7ce7',
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','rounds','control','radio','defaultValue',10,'trusteeCount',3,'options',JSON_ARRAY(10,20,30)),
    JSON_OBJECT('key','maxPlayers','control','radio','defaultValue',8,'trusteeCount',3,'options',JSON_ARRAY(8,10)),
    JSON_OBJECT('key','startPlayers','control','radio','defaultValue',2,'trusteeCount',3,'options',JSON_ARRAY(2,4,6)),
    JSON_OBJECT('key','mode','control','radio','defaultValue','classic','trusteeCount',3,'options',JSON_ARRAY('classic','passion','crazy')),
    JSON_OBJECT('key','maxRobMultiplier','control','radio','defaultValue',4,'trusteeCount',3,'options',JSON_ARRAY(3,4,5)),
    JSON_OBJECT('key','maxPushMultiplier','control','radio','defaultValue',10,'trusteeCount',3,'options',JSON_ARRAY(0,5,10,15)),
    JSON_OBJECT('key','standPolicy','control','radio','defaultValue','everyone_may_stand','trusteeCount',3,'options',JSON_ARRAY('loser_may_stand','everyone_may_stand','no_one_may_stand')),
    JSON_OBJECT('key','fastModeEnabled','control','checkbox','defaultValue',true,'trusteeCount',3,'options',JSON_ARRAY(true))
  ));

SET @cn298_ui = JSON_OBJECT(
  'bundle','poker-nn','scene','GameRoom2D','playFamily','poker:betting',
  'landscapePrefab','Prefab/CN298RoomLandscape','portraitPrefab','Prefab/CN298RoomPortrait',
  'roomRuleSourceHash','3bbcd398568c7d109835ac07e1114b81189aa8412e82a97b4f6c0ee315ac7ce7',
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','rounds','label','局数','order',10,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',10,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),'options',JSON_ARRAY(JSON_OBJECT('label','10局','value',10,'order',10,'disabled',false),JSON_OBJECT('label','20局','value',20,'order',20,'disabled',false),JSON_OBJECT('label','30局','value',30,'order',30,'disabled',false))),
    JSON_OBJECT('key','maxPlayers','label','最大人数','order',20,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',8,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),'options',JSON_ARRAY(JSON_OBJECT('label','8人','value',8,'order',10,'disabled',false),JSON_OBJECT('label','10人','value',10,'order',20,'disabled',false))),
    JSON_OBJECT('key','startPlayers','label','开局人数','order',30,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',2,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),'options',JSON_ARRAY(JSON_OBJECT('label','2人','value',2,'order',10,'disabled',false),JSON_OBJECT('label','4人','value',4,'order',20,'disabled',false),JSON_OBJECT('label','6人','value',6,'order',30,'disabled',false))),
    JSON_OBJECT('key','mode','label','玩法模式','order',40,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue','classic','trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),'options',JSON_ARRAY(JSON_OBJECT('label','经典模式','value','classic','order',10,'disabled',false),JSON_OBJECT('label','激情模式','value','passion','order',20,'disabled',false),JSON_OBJECT('label','疯狂模式','value','crazy','order',30,'disabled',false))),
    JSON_OBJECT('key','maxRobMultiplier','label','抢庄倍数','order',50,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',4,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(2),'options',JSON_ARRAY(JSON_OBJECT('label','最高3倍','value',3,'order',10,'disabled',false),JSON_OBJECT('label','最高4倍','value',4,'order',20,'disabled',false),JSON_OBJECT('label','最高5倍','value',5,'order',30,'disabled',false))),
    JSON_OBJECT('key','maxPushMultiplier','label','推注上限','order',60,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',10,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(3),'options',JSON_ARRAY(JSON_OBJECT('label','不推注','value',0,'order',10,'disabled',false),JSON_OBJECT('label','5倍','value',5,'order',20,'disabled',false),JSON_OBJECT('label','10倍','value',10,'order',30,'disabled',false),JSON_OBJECT('label','15倍','value',15,'order',40,'disabled',false))),
    JSON_OBJECT('key','standPolicy','label','站起规则','order',70,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue','everyone_may_stand','trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(2),'options',JSON_ARRAY(JSON_OBJECT('label','输家可站起','value','loser_may_stand','order',10,'disabled',false),JSON_OBJECT('label','所有人可站起','value','everyone_may_stand','order',20,'disabled',false),JSON_OBJECT('label','所有人不可站起','value','no_one_may_stand','order',30,'disabled',false))),
    JSON_OBJECT('key','fastModeEnabled','label','快速模式','order',80,'control','checkbox','visible',true,'disabled',false,'required',false,'defaultValue',true,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),'options',JSON_ARRAY(JSON_OBJECT('label','启用快速模式','value',true,'order',10,'disabled',false)))
  ));

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(@cn298_release_id,5,'cn298-v1.0.0',2,'GLOBAL',JSON_OBJECT('gameId',5,'gameCode','CN298','family','poker:betting'),@cn298_validator,@cn298_ui,JSON_OBJECT('factory','com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory'),SHA2('CN298-catalog-v2',256),SHA2(CAST(@cn298_validator AS CHAR),256),SHA2(CAST(@cn298_ui AS CHAR),256),SHA2('CN298-components-v2',256),SHA2('CN298-runtime-v2',256),'ACTIVE',100,1,'Publish CN298 authoritative room-create schema',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(@cn298_release_id,'GLOBAL',100,'ACTIVE');
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
VALUES(5,'GLOBAL','cn298-v1.0.0',@cn298_generation,@cn298_release_id,JSON_ARRAY('com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory@cn298-v1.0.0'),@cn298_validator,@cn298_ui,SHA2(CONCAT('CN298|',@cn298_generation,'|',CAST(@cn298_validator AS CHAR)),256),SHA2('CN298-runtime-v2',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));
UPDATE aoo_compiled_index_active SET index_generation=@cn298_generation,release_id=@cn298_release_id,cache_epoch=cache_epoch+1,activated_by=1,activation_reason='Activate CN298 authoritative room-create schema',activated_at=CURRENT_TIMESTAMP(3) WHERE game_id=5 AND region_code='GLOBAL' AND play_version='cn298-v1.0.0';
UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=5 AND region_code='GLOBAL' AND play_version='cn298-v1.0.0' AND index_generation<>@cn298_generation AND lifecycle_state='ACTIVE';
UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=5 AND play_version='cn298-v1.0.0' AND release_id<>@cn298_release_id AND status='ACTIVE';
