-- Correct LS201 against XQP area 950. The published identity remains LS201;
-- the deck is 7-A (32 cards) or 5-A (40 cards), with no 2 or jokers.

START TRANSACTION;

SET @ls201_rule_source_hash = '948467e4e7d526f23ecdc4db6f53058d1bd56722a334d9ef2653a778dbe7db77';
SET @ls201_view_source_hash = '11d1cc8d47ed609f3c4f664e1865442bbcb3ca99c4e2fcf060d72b52dd443a3a';
SET @ls201_schema = JSON_OBJECT(
  'bundle','pdk-common-room','scene','GameRoom2D','regionalProfile','LS201',
  'roomRuleWorkbook','开房规则表/跑得快/凉山跑得快.xlsx',
  'roomRuleSourceHash',@ls201_rule_source_hash,
  'roomRuleViewHash',@ls201_view_source_hash,
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','playerCount','label','人数','order',10,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value',2,'label','2人'),
        JSON_OBJECT('value',3,'label','3人'),JSON_OBJECT('value',4,'label','4人')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',2),
    JSON_OBJECT('key','roundCount','label','局数','order',20,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value',8,'label','8局'),
        JSON_OBJECT('value',12,'label','12局'),JSON_OBJECT('value',16,'label','16局')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',8),
    JSON_OBJECT('key','operationTime','label','操作时间','order',30,'control','RADIO',
      'visible',true,'disabled',true,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value',15,'label','15秒')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',15),
    JSON_OBJECT('key','dealCardCount','label','发牌张数','order',40,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value',8,'label','8张(7-A，无2/王)'),
        JSON_OBJECT('value',10,'label','10张(5-A，无2/王)')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',8),
    JSON_OBJECT('key','jinHuaScore','label','比金花','order',50,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value',1,'label','1分'),
        JSON_OBJECT('value',2,'label','2分'),JSON_OBJECT('value',3,'label','3分'),
        JSON_OBJECT('value',4,'label','4分'),JSON_OBJECT('value',5,'label','5分'),
        JSON_OBJECT('value','no_compare','label','不比')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',1),
    JSON_OBJECT('key','robDealerRule','label','抢庄','order',60,'control','RADIO',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(JSON_OBJECT('value','dealer_first','label','庄家先抢'),
        JSON_OBJECT('value','dealer_last','label','庄家后抢'),
        JSON_OBJECT('value','first_round_no_compete','label','首局不抢'),
        JSON_OBJECT('value','no_compete','label','不抢庄')),
      'defaultCandidateIndexes',JSON_ARRAY(4),'defaultValue','no_compete'),
    JSON_OBJECT('key','playRule','label','玩法','order',70,'control','CHECKBOX',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(
        JSON_OBJECT('value','compare_attachments','label','三带比带'),
        JSON_OBJECT('value','triple_with_one','label','三带一(单或对)'),
        JSON_OBJECT('value','four_with_two','label','四带二(两张或两对)'),
        JSON_OBJECT('value','four_aces','label','四张A'),
        JSON_OBJECT('value','four_configured_rank','label','四张7/5'),
        JSON_OBJECT('value','all_single','label','全单'),
        JSON_OBJECT('value','full_straight','label','全顺子'),
        JSON_OBJECT('value','full_consecutive_pairs','label','全连对'),
        JSON_OBJECT('value','all_pair','label','全对'),
        JSON_OBJECT('value','all_black','label','全黑'),
        JSON_OBJECT('value','all_red','label','全红'),
        JSON_OBJECT('value','all_big','label','全大'),
        JSON_OBJECT('value','all_small','label','全小')),
      'defaultCandidateIndexes',JSON_ARRAY(),'defaultValue',JSON_ARRAY()),
    JSON_OBJECT('key','roomRestriction','label','其他','order',80,'control','CHECKBOX',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(
        JSON_OBJECT('value','ip_limit','label','IP限制'),
        JSON_OBJECT('value','gps_limit','label','GPS限制'),
        JSON_OBJECT('value','timeout_auto_play','label','超时托管'),
        JSON_OBJECT('value','distance_warning','label','距离过近警告'),
        JSON_OBJECT('value','interaction_forbidden','label','禁止互动'),
        JSON_OBJECT('value','chat_muted','label','禁言')),
      'defaultCandidateIndexes',JSON_ARRAY(),'defaultValue',JSON_ARRAY()),
    JSON_OBJECT('key','payerMode','label','支付方式','order',90,'control','RADIO',
      'visible',false,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value','OWNER','label','房主支付')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue','OWNER')
  ));

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 900052026090710,a.game_id,a.play_version,2026090710,r.release_scope,r.catalog_snapshot,
 @ls201_schema,@ls201_schema,r.component_snapshot,r.catalog_hash,
 SHA2(CONCAT('90005|2026090710|',@ls201_rule_source_hash),256),
 SHA2(CONCAT('90005|2026090710|',@ls201_view_source_hash),256),r.component_hash,
 SHA2('90005|2026090710|pdk-common-room',256),'ACTIVE',100,r.created_by,
 'Correct LS201 XQP deck, deal and rule defaults',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=a.release_id
WHERE a.game_id=90005;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 900052026090710,region_code,100,'ACTIVE'
FROM aoo_compiled_index_active WHERE game_id=90005;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT a.game_id,a.region_code,a.play_version,2026090710,900052026090710,i.component_chain,
 @ls201_schema,@ls201_schema,
 SHA2(CONCAT('90005|2026090710|',@ls201_rule_source_hash,'|',@ls201_view_source_hash),256),
 SHA2('90005|2026090710|pdk-common-room',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
 ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
 AND i.index_generation=a.index_generation WHERE a.game_id=90005;

UPDATE aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a
 ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.game_id=90005 AND i.index_generation<>2026090710 AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=90005 AND release_id<>900052026090710 AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED' WHERE r.game_id=90005 AND r.release_id<>900052026090710
 AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active SET index_generation=2026090710,
 release_id=900052026090710,cache_epoch=cache_epoch+1,activated_by=1,
 activation_reason='Correct LS201 XQP deck, deal and rule defaults',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005;

UPDATE aoo_published_game_configuration SET release_id=900052026090710,
 reason='Correct LS201 XQP deck, deal and rule defaults',created_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005;

UPDATE aoo_play_version SET rule_schema_version=3,ui_schema_version=3,row_version=row_version+1
WHERE game_id=90005 AND play_version='xqp-equivalent-1';

COMMIT;
