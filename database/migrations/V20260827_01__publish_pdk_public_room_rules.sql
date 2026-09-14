-- Publish the PDK public room-rule capability catalogue as a new immutable
-- compiled index.  These fields are display-only because card-pattern
-- availability belongs to the authoritative PdkVariantPolicy, not to client
-- selections.  In particular FOUR_WITH_THREE remains unavailable unless a
-- concrete variant publishes and enforces an explicit policy.

INSERT INTO aoo_game_release(
    release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
    catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,
    rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 800000002,game_id,play_version,2,release_scope,
       catalog_snapshot,rule_snapshot,
       JSON_SET(ui_snapshot,'$.publicRoomRuleSchemaVersion',2),component_snapshot,
       catalog_hash,rule_hash,SHA2('pdk-public-room-rules-v2',256),component_hash,
       SHA2('poker01-prefab-v4|pdk-public-room-rules-v2',256),'ACTIVE',100,created_by,
       'Publish PDK public room rules and fail-closed optional capabilities',
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release
WHERE release_id=800000001;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 800000002,region_code,100,'ACTIVE'
FROM aoo_game_release_region
WHERE release_id=800000001;

INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,
    component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
    lifecycle_state,validated_at,activated_at)
SELECT i.game_id,i.region_code,i.play_version,2,800000002,
       i.component_chain,i.rule_validator,
       JSON_ARRAY_APPEND(
         JSON_SET(i.ui_schema,'$.publicRoomRuleSchemaVersion',2),
         '$.fields',JSON_OBJECT(
           'key','basicPatternCapabilities','label','基础牌型（固定支持）',
           'control','MULTI_SELECT','disabled',TRUE,
           'options',JSON_ARRAY(
             JSON_OBJECT('value','SINGLE','label','单张（固定支持）','disabled',TRUE),
             JSON_OBJECT('value','PAIR','label','对子（固定支持）','disabled',TRUE),
             JSON_OBJECT('value','STRAIGHT','label','顺子（固定支持）','disabled',TRUE),
             JSON_OBJECT('value','CONSECUTIVE_PAIRS','label','连对（固定支持）','disabled',TRUE),
             JSON_OBJECT('value','BOMB','label','炸弹（固定支持）','disabled',TRUE),
             JSON_OBJECT('value','AIRPLANE','label','飞机（固定支持）','disabled',TRUE))),
         '$.fields',JSON_OBJECT(
           'key','optionalPatternCapabilities','label','可选牌型能力',
           'control','MULTI_SELECT','disabled',TRUE,
           'options',JSON_ARRAY(
             JSON_OBJECT('value','TRIPLE_WITH_ONE','label','三带一（公共支持）','disabled',TRUE),
             JSON_OBJECT('value','TRIPLE_WITH_PAIR','label','三带二（当前玩法启用）','disabled',TRUE),
             JSON_OBJECT('value','FOUR_WITH_ONE','label','四带一（需具体玩法开启）','disabled',TRUE),
             JSON_OBJECT('value','FOUR_WITH_TWO','label','四带二（当前玩法启用）','disabled',TRUE),
             JSON_OBJECT('value','FOUR_WITH_THREE','label','四带三（默认关闭，需具体玩法开启）','disabled',TRUE))),
         '$.fields',JSON_OBJECT(
           'key','flowCapabilities','label','公共流程能力',
           'control','MULTI_SELECT','disabled',TRUE,
           'options',JSON_ARRAY(
             JSON_OBJECT('value','READY_DEAL','label','准备与发牌','disabled',TRUE),
             JSON_OBJECT('value','TURN_HINT_PLAY_PASS','label','轮转、提示、出牌与过牌','disabled',TRUE),
             JSON_OBJECT('value','TRUSTEE_TIMEOUT','label','托管与超时处理','disabled',TRUE),
             JSON_OBJECT('value','RECONNECT_REPLAY','label','断线重连与回放','disabled',TRUE),
             JSON_OBJECT('value','SETTLEMENT','label','小结算与大结算','disabled',TRUE)))) AS ui_schema,
       SHA2('pdk-index-v2-public-room-rules',256),
       SHA2('poker01-prefab-v4|pdk-public-room-rules-v2',256),
       'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index i
JOIN aoo_compiled_index_active a
  ON a.game_id=i.game_id AND a.region_code=i.region_code
 AND a.play_version=i.play_version AND a.index_generation=i.index_generation
WHERE i.game_id=8 AND i.play_version='1.0.0';

UPDATE aoo_compiled_index_active
SET index_generation=2,release_id=800000002,cache_epoch=cache_epoch+1,
    activated_by=1,
    activation_reason='Publish PDK public room rules and optional capability policy',
    activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=8 AND play_version='1.0.0';

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=8 AND play_version='1.0.0'
  AND index_generation<2 AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=8 AND play_version='1.0.0'
  AND release_version<2 AND status='ACTIVE';
