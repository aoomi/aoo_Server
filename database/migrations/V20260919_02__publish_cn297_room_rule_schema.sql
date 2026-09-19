-- Publish the CN297 room-create schema compiled from the read-only workbook
-- Client/docs/开房规则表/金花/全国金花CN297.xlsx.
-- Source SHA-256: ab5f450cd946f477f0c9bd0e91a0b9c5ac84726f3f938106a40791815e09edfa

SET @cn297_release_id = 902026091902;
SET @cn297_generation = 2026091902;

SET @cn297_validator = JSON_OBJECT(
  'gameCode','CN297','playVersion','cn297-v1.0.0',
  'source','全国金花CN297.xlsx',
  'sourceHash','ab5f450cd946f477f0c9bd0e91a0b9c5ac84726f3f938106a40791815e09edfa',
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','totalRounds','control','radio','defaultValue',10,'trusteeCount',3,
      'options',JSON_ARRAY(10,20,30)),
    JSON_OBJECT('key','seatLimit','control','radio','defaultValue',8,'trusteeCount',3,
      'options',JSON_ARRAY(8,10)),
    JSON_OBJECT('key','minimumPlayers','control','radio','defaultValue',2,'trusteeCount',3,
      'options',JSON_ARRAY(2,4,6)),
    JSON_OBJECT('key','operationSeconds','control','radio','defaultValue',10,'trusteeCount',3,
      'options',JSON_ARRAY(10,15,20)),
    JSON_OBJECT('key','compareStartRound','control','radio','defaultValue',5,'trusteeCount',3,
      'options',JSON_ARRAY(1,3,5)),
    JSON_OBJECT('key','maximumBet','control','radio','defaultValue',50,'trusteeCount',3,
      'options',JSON_ARRAY(10,20,50)),
    JSON_OBJECT('key','mustBlindRounds','control','radio','defaultValue',1,'trusteeCount',3,
      'options',JSON_ARRAY(0,1,2)),
    JSON_OBJECT('key','baseBet','control','radio','defaultValue',1,'trusteeCount',3,
      'options',JSON_ARRAY(1,2,5,10)),
    JSON_OBJECT('key','aaaBonus','control','number','defaultValue',20,'trusteeCount',3,
      'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','leopardBonus','control','number','defaultValue',10,'trusteeCount',3,
      'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','straightFlushBonus','control','number','defaultValue',5,'trusteeCount',3,
      'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY())
  ));

SET @cn297_ui = JSON_OBJECT(
  'bundle','poker-zjh','scene','GameRoom2D','playFamily','poker:compare-hand',
  'landscapePrefab','Prefab/CN297RoomLandscape','portraitPrefab','Prefab/CN297RoomPortrait',
  'roomRuleSourceHash','ab5f450cd946f477f0c9bd0e91a0b9c5ac84726f3f938106a40791815e09edfa',
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','totalRounds','label','局数','order',10,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',10,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),
      'options',JSON_ARRAY(JSON_OBJECT('label','10局','value',10,'order',10,'disabled',false),JSON_OBJECT('label','20局','value',20,'order',20,'disabled',false),JSON_OBJECT('label','30局','value',30,'order',30,'disabled',false))),
    JSON_OBJECT('key','seatLimit','label','桌型','order',20,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',8,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),
      'options',JSON_ARRAY(JSON_OBJECT('label','8人桌','value',8,'order',10,'disabled',false),JSON_OBJECT('label','10人桌','value',10,'order',20,'disabled',false))),
    JSON_OBJECT('key','minimumPlayers','label','开局人数','order',30,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',2,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),
      'options',JSON_ARRAY(JSON_OBJECT('label','2人开局','value',2,'order',10,'disabled',false),JSON_OBJECT('label','4人开局','value',4,'order',20,'disabled',false),JSON_OBJECT('label','6人开局','value',6,'order',30,'disabled',false))),
    JSON_OBJECT('key','operationSeconds','label','操作时间','order',40,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',10,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),
      'options',JSON_ARRAY(JSON_OBJECT('label','10秒','value',10,'order',10,'disabled',false),JSON_OBJECT('label','15秒','value',15,'order',20,'disabled',false),JSON_OBJECT('label','20秒','value',20,'order',30,'disabled',false))),
    JSON_OBJECT('key','compareStartRound','label','开始比牌','order',50,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',5,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(3),
      'options',JSON_ARRAY(JSON_OBJECT('label','第1轮','value',1,'order',10,'disabled',false),JSON_OBJECT('label','第3轮','value',3,'order',20,'disabled',false),JSON_OBJECT('label','第5轮','value',5,'order',30,'disabled',false))),
    JSON_OBJECT('key','maximumBet','label','单注封顶','order',60,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',50,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(3),
      'options',JSON_ARRAY(JSON_OBJECT('label','10分','value',10,'order',10,'disabled',false),JSON_OBJECT('label','20分','value',20,'order',20,'disabled',false),JSON_OBJECT('label','50分','value',50,'order',30,'disabled',false))),
    JSON_OBJECT('key','mustBlindRounds','label','闷牌轮数','order',70,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',1,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(2),
      'options',JSON_ARRAY(JSON_OBJECT('label','不强制','value',0,'order',10,'disabled',false),JSON_OBJECT('label','1轮','value',1,'order',20,'disabled',false),JSON_OBJECT('label','2轮','value',2,'order',30,'disabled',false))),
    JSON_OBJECT('key','baseBet','label','底分','order',80,'control','radio','visible',true,'disabled',false,'required',true,'defaultValue',1,'trusteeCount',3,'defaultCandidateIndexes',JSON_ARRAY(1),
      'options',JSON_ARRAY(JSON_OBJECT('label','1分','value',1,'order',10,'disabled',false),JSON_OBJECT('label','2分','value',2,'order',20,'disabled',false),JSON_OBJECT('label','5分','value',5,'order',30,'disabled',false),JSON_OBJECT('label','10分','value',10,'order',40,'disabled',false))),
    JSON_OBJECT('key','aaaBonus','label','AAA奖励','order',90,'control','number','visible',true,'disabled',false,'required',true,'defaultValue',20,'trusteeCount',3,'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','leopardBonus','label','豹子奖励','order',100,'control','number','visible',true,'disabled',false,'required',true,'defaultValue',10,'trusteeCount',3,'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY()),
    JSON_OBJECT('key','straightFlushBonus','label','同花顺奖励','order',110,'control','number','visible',true,'disabled',false,'required',true,'defaultValue',5,'trusteeCount',3,'min',0,'max',2147483647,'step',1,'options',JSON_ARRAY())
  ));

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES(
  @cn297_release_id,9,'cn297-v1.0.0',2,'GLOBAL',
  JSON_OBJECT('gameId',9,'gameCode','CN297','family','poker:compare-hand'),
  @cn297_validator,@cn297_ui,
  JSON_OBJECT('factory','business.global.pk.zjh.ZJHPokerFamilyProviderFactory'),
  SHA2('CN297-catalog-v2',256),SHA2(CAST(@cn297_validator AS CHAR),256),
  SHA2(CAST(@cn297_ui AS CHAR),256),SHA2('CN297-components-v2',256),
  SHA2('CN297-runtime-v2',256),'ACTIVE',100,1,
  'Publish CN297 authoritative room-create schema',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(@cn297_release_id,'GLOBAL',100,'ACTIVE');

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,component_chain,
  rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
  compiled_at,validated_at,activated_at)
VALUES(
  9,'GLOBAL','cn297-v1.0.0',@cn297_generation,@cn297_release_id,
  JSON_ARRAY('business.global.pk.zjh.ZJHPokerFamilyProviderFactory@cn297-v1.0.0'),
  @cn297_validator,@cn297_ui,
  SHA2(CONCAT('CN297|',@cn297_generation,'|',CAST(@cn297_validator AS CHAR)),256),
  SHA2('CN297-runtime-v2',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));

UPDATE aoo_compiled_index_active
SET index_generation=@cn297_generation,release_id=@cn297_release_id,
    cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason='Activate CN297 authoritative room-create schema',
    activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=9 AND region_code='GLOBAL' AND play_version='cn297-v1.0.0';

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
WHERE game_id=9 AND region_code='GLOBAL' AND play_version='cn297-v1.0.0'
  AND index_generation<>@cn297_generation AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
WHERE game_id=9 AND play_version='cn297-v1.0.0'
  AND release_id<>@cn297_release_id AND status='ACTIVE';
