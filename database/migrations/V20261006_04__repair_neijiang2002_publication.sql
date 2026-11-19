-- Repair the current Neijiang canonical publication closure without rewriting historical rows.
-- The identity already has an active canonical play version, release, compiled index,
-- pointer and cost policies; the missing route and stale rule/UI publication are
-- repaired as one forward, idempotent generation.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

CREATE TEMPORARY TABLE neijiang2002_publication (
  game_id BIGINT NOT NULL PRIMARY KEY,
  game_code VARCHAR(32) NOT NULL,
  play_version VARCHAR(64) NOT NULL,
  region_code VARCHAR(32) NOT NULL,
  source_hash CHAR(64) NOT NULL,
  rule_fields JSON NOT NULL
);

INSERT INTO neijiang2002_publication(game_id,game_code,play_version,region_code,source_hash,rule_fields)
VALUES (2002,'neijiang2002','neijiang2002-v1.0.0','CN-51-10','b837699d3db2a723adb08a58724be2974c0a11494a7a057b8c599380f0a234c7',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":70,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTime","label":"操作时间","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":1000,"options":[{"value":1000,"label":"1000秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":20,"label":"20秒","order":30,"disabled":false}]},{"key":"rule_neijiang2002_0001","label":"先出牌","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"winner_first","options":[{"value":"winner_first","label":"赢家先出","order":10,"disabled":false},{"value":"spade_three_first","label":"黑桃3优先","order":20,"disabled":false}]},{"key":"rule_neijiang2002_0002","label":"三带二","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"two_cards_no_compare","options":[{"value":"two_cards_no_compare","label":"随便带","order":10,"disabled":false},{"value":"two_cards_compare","label":"带一样","order":20,"disabled":false}]},{"key":"rule_neijiang2002_0003","label":"炸弹记分","control":"radio","order":120,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":5,"options":[{"value":5,"label":"5分","order":10,"disabled":false},{"value":10,"label":"10分","order":20,"disabled":false},{"value":20,"label":"20分","order":30,"disabled":false},{"value":30,"label":"30分","order":40,"disabled":false}]},{"key":"rule_neijiang2002_0004","label":"小结算","control":"checkbox","order":130,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"rule_neijiang2002_0005","label":"玩法","control":"checkbox","order":140,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[2,3,4,5],"defaultValue":["four_with_two","triple_ace_bomb","require_spade_three","four_threes_direct_win"],"options":[{"value":"three_no_attachment","label":"三不带","order":10,"disabled":false},{"value":"four_with_two","label":"四带二","order":20,"disabled":false},{"value":"triple_ace_bomb","label":"3A算炸弹","order":30,"disabled":false},{"value":"require_spade_three","label":"必须出黑桃3","order":40,"disabled":false},{"value":"four_threes_direct_win","label":"四个3关门","order":50,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":150,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"rule_neijiang2002_0006","label":"其他","control":"checkbox","order":160,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"超时托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近警告","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]}]' AS JSON));

CREATE TEMPORARY TABLE neijiang2002_validator AS
SELECT p.game_id,
       JSON_SET(
         JSON_OBJECT('gameId',p.game_id,'gameCode',p.game_code,'playVersion',p.play_version),
         '$.fields',JSON_ARRAY_APPEND(p.rule_fields,'$',JSON_OBJECT(
           'key','payerMode','label','支付方式','control','radio','order',10000,
           'visible',false,'disabled',true,'required',true,'defaultCandidateIndexes',JSON_ARRAY(1),
           'defaultValue','owner_pays','options',JSON_ARRAY(
             JSON_OBJECT('value','owner_pays','label','房主付','order',10,'disabled',false),
             JSON_OBJECT('value','split_pays','label','平分付','order',20,'disabled',false),
             JSON_OBJECT('value','winner_pays','label','胜家付','order',30,'disabled',false)
           )
         ))
       ) AS validator
FROM neijiang2002_publication p;

CREATE TEMPORARY TABLE neijiang2002_route_template AS
SELECT r.endpoint,r.priority
FROM aoo_game_service_route r
JOIN aoo_game_catalog g ON g.game_id=r.game_id
WHERE g.family_code='poker:pao-de-kuai' AND r.status='ACTIVE'
ORDER BY r.game_id,r.priority,r.route_id
LIMIT 1;

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
SELECT p.game_id*1000000+2026100604,p.game_id,p.play_version,t.endpoint,t.priority,'ACTIVE'
FROM neijiang2002_publication p
CROSS JOIN neijiang2002_route_template t
WHERE 1=1
ON DUPLICATE KEY UPDATE
  endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT p.game_id*1000000000+2026100604,p.game_id,p.play_version,2026100604,r.release_scope,
       JSON_SET(COALESCE(r.catalog_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version),
       JSON_SET(COALESCE(r.rule_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSchema',v.validator),
       JSON_SET(COALESCE(r.ui_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       r.component_snapshot,
       SHA2(CONCAT(p.game_code,'|catalog|',p.play_version),256),
       SHA2(CONCAT(p.game_code,'|rules|',p.play_version,'|',p.source_hash),256),
       p.source_hash,
       r.component_hash,
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash),256),
       'ACTIVE',r.rollout_percent,r.created_by,
       'Repair Neijiang canonical publication closure',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM neijiang2002_publication p
JOIN neijiang2002_validator v ON v.game_id=p.game_id
JOIN aoo_game_release r
  ON r.game_id=p.game_id AND r.play_version=p.play_version AND r.status='ACTIVE'
ORDER BY r.release_version DESC,r.release_id DESC
LIMIT 1
ON DUPLICATE KEY UPDATE
  release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),
  rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
  component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),
  rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),
  bundle_hash=VALUES(bundle_hash),status='ACTIVE',rollout_percent=VALUES(rollout_percent),
  reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT p.game_id*1000000000+2026100604,rr.region_code,rr.rollout_percent,'ACTIVE'
FROM neijiang2002_publication p
JOIN aoo_game_release oldr
  ON oldr.game_id=p.game_id AND oldr.play_version=p.play_version
 AND oldr.status='ACTIVE' AND oldr.release_id<>p.game_id*1000000000+2026100604
JOIN aoo_game_release_region rr ON rr.release_id=oldr.release_id AND rr.status='ACTIVE'
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,component_chain,
  rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
  compiled_at,validated_at,activated_at)
SELECT p.game_id,i.region_code,p.play_version,2026100604,
       p.game_id*1000000000+2026100604,i.component_chain,v.validator,
       JSON_SET(COALESCE(i.ui_schema,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       SHA2(CONCAT(p.game_code,'|',i.region_code,'|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash),256),
       'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM neijiang2002_publication p
JOIN neijiang2002_validator v ON v.game_id=p.game_id
JOIN aoo_compiled_room_create_index i
  ON i.game_id=p.game_id AND i.play_version=p.play_version AND i.lifecycle_state='ACTIVE'
 AND i.index_generation<>2026100604
ON DUPLICATE KEY UPDATE
  release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),
  bundle_hash=VALUES(bundle_hash),lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),
  activated_at=VALUES(activated_at);

INSERT INTO aoo_compiled_index_active(
  game_id,region_code,play_version,index_generation,release_id,cache_epoch,
  activated_by,activation_reason,activated_at)
SELECT p.game_id,i.region_code,p.play_version,2026100604,
       p.game_id*1000000000+2026100604,i.cache_epoch+1,i.activated_by,
       'Repair Neijiang canonical publication closure',CURRENT_TIMESTAMP(3)
FROM neijiang2002_publication p
JOIN aoo_compiled_index_active i
  ON i.game_id=p.game_id AND i.play_version=p.play_version AND i.index_generation<>2026100604
ON DUPLICATE KEY UPDATE
  play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),cache_epoch=GREATEST(aoo_compiled_index_active.cache_epoch,VALUES(cache_epoch)),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),
  activated_at=VALUES(activated_at);

UPDATE aoo_compiled_room_create_index i
JOIN neijiang2002_publication p ON p.game_id=i.game_id AND p.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.lifecycle_state='ACTIVE' AND i.index_generation<>2026100604;

UPDATE aoo_game_release r
JOIN neijiang2002_publication p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE' AND r.release_id<>p.game_id*1000000000+2026100604;

UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
JOIN neijiang2002_publication p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET rr.status='RETIRED'
WHERE r.release_id<>p.game_id*1000000000+2026100604 AND rr.status='ACTIVE';

UPDATE aoo_game_service_route r
JOIN neijiang2002_publication p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET r.status='OFFLINE'
WHERE r.route_id<>p.game_id*1000000+2026100604 AND r.status='ACTIVE';

DROP TEMPORARY TABLE neijiang2002_route_template;
DROP TEMPORARY TABLE neijiang2002_validator;
DROP TEMPORARY TABLE neijiang2002_publication;
COMMIT;
