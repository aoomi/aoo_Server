-- Publish the LS201 create-room schema generated from the authoritative PDK
-- rule workbooks. Values remain stable protocol keys; all visible text comes
-- from the published schema instead of a client-side translation table.

START TRANSACTION;

SET @ls201_rule_source_hash = '514b8f16c72ff048cb778d10b5a61ec4874cbbf148a8ab5a6df558d704236f66';
SET @ls201_view_source_hash = '62b00dd3de701ea184289dda931dca8e937b147134ade8f857d000e6e75403d5';
SET @ls201_schema = JSON_OBJECT(
  'bundle','pdk-common-room','scene','GameRoom2D','regionalProfile','lspdk',
  'roomRuleWorkbook','开房规则表/跑得快/跑得快可配置规则总表.xlsx',
  'roomRuleSourceHash',@ls201_rule_source_hash,
  'roomRuleViewHash',@ls201_view_source_hash,
  'fields',JSON_ARRAY(
    JSON_OBJECT('key','playerCount','label','人数','order',10,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(
        JSON_OBJECT('value',2,'label','2人'),JSON_OBJECT('value',3,'label','3人'),
        JSON_OBJECT('value',4,'label','4人')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',2),
    JSON_OBJECT('key','roundCount','label','局数','order',20,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(
        JSON_OBJECT('value',8,'label','8局'),JSON_OBJECT('value',12,'label','12局'),
        JSON_OBJECT('value',16,'label','16局')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',8),
    JSON_OBJECT('key','operationTime','label','操作时间','order',30,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(
        JSON_OBJECT('value',10,'label','10秒'),JSON_OBJECT('value',15,'label','15秒'),
        JSON_OBJECT('value',20,'label','20秒')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',10),
    JSON_OBJECT('key','dealCardCount','label','发牌张数','order',40,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(
        JSON_OBJECT('value',8,'label','8张(7-A)'),JSON_OBJECT('value',10,'label','10张(5-A)')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue',8),
    JSON_OBJECT('key','jinHuaScore','label','比金花','order',50,'control','RADIO',
      'visible',true,'disabled',false,'required',true,
      'options',JSON_ARRAY(
        JSON_OBJECT('value',1,'label','1分'),JSON_OBJECT('value',2,'label','2分'),
        JSON_OBJECT('value',3,'label','3分'),JSON_OBJECT('value',4,'label','4分'),
        JSON_OBJECT('value',5,'label','5分'),JSON_OBJECT('value','no_compare','label','不比')),
      'defaultCandidateIndexes',JSON_ARRAY(6),'defaultValue','no_compare'),
    JSON_OBJECT('key','robDealerRule','label','抢庄','order',60,'control','RADIO',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(
        JSON_OBJECT('value','dealer_first','label','庄家先抢'),
        JSON_OBJECT('value','dealer_last','label','庄家后抢'),
        JSON_OBJECT('value','first_round_no_compete','label','首局不抢'),
        JSON_OBJECT('value','no_compete','label','不抢庄')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue','dealer_first'),
    JSON_OBJECT('key','playRule','label','玩法','order',70,'control','CHECKBOX',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(
        JSON_OBJECT('value','compare_attachments','label','三带比带'),
        JSON_OBJECT('value','triple_with_one','label','三带一'),
        JSON_OBJECT('value','four_with_two','label','四带二'),
        JSON_OBJECT('value','four_ace_rank','label','四张A,7'),
        JSON_OBJECT('value','all_single','label','全单'),
        JSON_OBJECT('value','full_consecutive_pairs','label','全连对'),
        JSON_OBJECT('value','all_special_patterns','label','全大,全小,全红,全黑,全连,全对')),
      'defaultCandidateIndexes',JSON_ARRAY(1,2,4,6),
      'defaultValue',JSON_ARRAY('compare_attachments','triple_with_one','four_ace_rank','full_consecutive_pairs')),
    JSON_OBJECT('key','roomRestriction','label','其他','order',80,'control','CHECKBOX',
      'visible',true,'disabled',false,'required',false,
      'options',JSON_ARRAY(
        JSON_OBJECT('value','ip_limit','label','IP限制'),
        JSON_OBJECT('value','gps_limit','label','GPS限制'),
        JSON_OBJECT('value','timeout_auto_play','label','超时托管'),
        JSON_OBJECT('value','distance_warning','label','距离过近警告'),
        JSON_OBJECT('value','interaction_forbidden','label','禁止互动'),
        JSON_OBJECT('value','chat_muted','label','禁言')),
      'defaultCandidateIndexes',JSON_ARRAY(1,3,4,6),
      'defaultValue',JSON_ARRAY('ip_limit','timeout_auto_play','distance_warning','chat_muted')),
    JSON_OBJECT('key','payerMode','label','支付方式','order',90,'control','RADIO',
      'visible',false,'disabled',false,'required',true,
      'options',JSON_ARRAY(JSON_OBJECT('value','OWNER','label','房主支付')),
      'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue','OWNER')
  ));

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 900052026090709,a.game_id,a.play_version,2026090709,r.release_scope,r.catalog_snapshot,
 @ls201_schema,@ls201_schema,r.component_snapshot,r.catalog_hash,
 SHA2(CONCAT('90005|2026090709|',@ls201_rule_source_hash),256),
 SHA2(CONCAT('90005|2026090709|',@ls201_view_source_hash),256),r.component_hash,
 SHA2('90005|2026090709|pdk-common-room',256),'ACTIVE',100,r.created_by,
 'Publish authoritative Chinese LS201 room-rule UI',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=a.release_id
WHERE a.game_id=90005;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 900052026090709,region_code,100,'ACTIVE'
FROM aoo_compiled_index_active WHERE game_id=90005;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT a.game_id,a.region_code,a.play_version,2026090709,900052026090709,i.component_chain,
 @ls201_schema,@ls201_schema,
 SHA2(CONCAT('90005|2026090709|',@ls201_rule_source_hash,'|',@ls201_view_source_hash),256),
 SHA2('90005|2026090709|pdk-common-room',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
 ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
 AND i.index_generation=a.index_generation WHERE a.game_id=90005;

UPDATE aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a
 ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.game_id=90005 AND i.index_generation<>2026090709 AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=90005 AND release_id<>900052026090709 AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED' WHERE r.game_id=90005 AND r.release_id<>900052026090709
 AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active SET index_generation=2026090709,release_id=900052026090709,
 cache_epoch=cache_epoch+1,activated_by=1,
 activation_reason='LS201 authoritative Chinese room-rule UI',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005;

UPDATE aoo_published_game_configuration SET release_id=900052026090709,
 reason='LS201 authoritative Chinese room-rule UI',created_at=CURRENT_TIMESTAMP(3)
WHERE game_id=90005;

UPDATE aoo_play_version SET rule_schema_version=2,ui_schema_version=2,row_version=row_version+1
WHERE game_id=90005 AND play_version='xqp-equivalent-1';

COMMIT;
