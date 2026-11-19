-- Rebuild only the canonical Liangshan 2003 publication projection whose
-- operationTime field was published disabled.  The workbook projection is
-- embedded from the read-only publisher preview; no rule source is read at
-- migration time and the prior release/index remain available for replay.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

SET @game_id = 2003;
SET @game_code = 'liangshan2003';
SET @play_version = 'liangshan2003-v1.0.0';
SET @region_code = 'CN-51-34';
SET @source_hash = '234af1daa86aa3efd257e130d7cd5eadb8bb76361079ea3a0f418d0c20502026';
SET @generation = 2026100701;
SET @release_id = @game_id * 1000000000 + @generation;
SET @reason = 'Repair canonical Liangshan2003 operationTime publication';

CREATE TEMPORARY TABLE liangshan2003_publication_repair (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  game_code VARCHAR(64) NOT NULL UNIQUE,
  play_version VARCHAR(64) NOT NULL UNIQUE,
  region_code VARCHAR(32) NOT NULL,
  source_hash CHAR(64) NOT NULL,
  rule_fields JSON NOT NULL
);

INSERT INTO liangshan2003_publication_repair
  (game_id,game_code,play_version,region_code,source_hash,rule_fields)
VALUES
  (@game_id,@game_code,@play_version,@region_code,@source_hash,
   CAST('[{"key":"playerCount","label":"人数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false},{"value":4,"label":"4人","order":30,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTime","label":"操作时间","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10000,"options":[{"value":10000,"label":"10000秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":20,"label":"20秒","order":30,"disabled":false}]},{"key":"dealCardCount","label":"发牌张数","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8张(7-A)","order":10,"disabled":false},{"value":10,"label":"10张(5-A)","order":20,"disabled":false}]},{"key":"jinHuaScore","label":"比金花","control":"radio","order":120,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[6],"defaultValue":"no_compare","options":[{"value":1,"label":"1分","order":10,"disabled":false},{"value":2,"label":"2分","order":20,"disabled":false},{"value":3,"label":"3分","order":30,"disabled":false},{"value":4,"label":"4分","order":40,"disabled":false},{"value":5,"label":"5分","order":50,"disabled":false},{"value":"no_compare","label":"不比","order":60,"disabled":false}]},{"key":"robDealerRule","label":"抢庄","control":"radio","order":130,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"dealer_first","options":[{"value":"dealer_first","label":"庄家先抢","order":10,"disabled":false},{"value":"dealer_last","label":"庄家后抢","order":20,"disabled":false},{"value":"first_round_no_compete","label":"首局不抢","order":30,"disabled":false},{"value":"no_compete","label":"不抢庄","order":40,"disabled":false}]},{"key":"rule_liangshan2003_0001","label":"小结算","control":"checkbox","order":140,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"playRule","label":"玩法","control":"checkbox","order":150,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1,2,3,4,5],"defaultValue":["compare_attachments","triple_with_one","four_with_two","four_ace_rank","all_special_patterns"],"options":[{"value":"compare_attachments","label":"三带比带","order":10,"disabled":false},{"value":"triple_with_one","label":"三带一","order":20,"disabled":false},{"value":"four_with_two","label":"四带二","order":30,"disabled":false},{"value":"four_ace_rank","label":"四张A,7","order":40,"disabled":false},{"value":"all_special_patterns","label":"全单,全顺,全连对,全对,全黑,全红,全大,全小","order":50,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":160,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"roomRestriction","label":"其他","control":"checkbox","order":170,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"超时托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近警告","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]}]' AS JSON));

CREATE TEMPORARY TABLE liangshan2003_publication_validator AS
SELECT p.game_id,
       JSON_ARRAY(
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[0]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[1]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[2]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[3]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[4]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[5]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[6]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[7]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[8]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_REMOVE(JSON_EXTRACT(p.rule_fields,'$[9]'),'$.label','$.visible','$.defaultCandidateIndexes'),
         JSON_OBJECT('key','payerMode','control','radio','order',10000,
           'disabled',false,'required',true,'defaultValue','OWNER',
           'options',JSON_ARRAY(JSON_OBJECT('value','OWNER','order',10,'disabled',false)))) AS fields,
       JSON_OBJECT('rule_ls201_0001',JSON_OBJECT(
         'gameId',p.game_id,'gameCode',p.game_code,'playVersion',p.play_version,
         'canonicalKey','rule_liangshan2003_0001')) AS legacy_field_aliases
FROM liangshan2003_publication_repair p;

-- A missing or duplicated active preimage is an invalid publication state.
-- The enforced CHECK rows fail the transaction before any permanent write.
CREATE TEMPORARY TABLE liangshan2003_publication_active_release (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  release_scope VARCHAR(16) NOT NULL,
  catalog_snapshot JSON NOT NULL,
  rule_snapshot JSON NOT NULL,
  ui_snapshot JSON NOT NULL,
  component_snapshot JSON NOT NULL,
  rollout_percent DECIMAL(5,2) NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  region_rollout_percent DECIMAL(5,2) NOT NULL
);
INSERT INTO liangshan2003_publication_active_release
SELECT r.game_id,r.release_scope,r.catalog_snapshot,r.rule_snapshot,r.ui_snapshot,
       r.component_snapshot,r.rollout_percent,r.created_by,rr.rollout_percent
FROM aoo_game_release r
JOIN liangshan2003_publication_repair p
  ON p.game_id=r.game_id AND p.play_version=r.play_version
JOIN aoo_game_release_region rr
  ON rr.release_id=r.release_id AND rr.region_code=p.region_code AND rr.status='ACTIVE'
WHERE r.status='ACTIVE';

CREATE TEMPORARY TABLE liangshan2003_publication_active_index (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  component_chain JSON NOT NULL,
  rule_validator JSON NOT NULL,
  ui_schema JSON NOT NULL
);
INSERT INTO liangshan2003_publication_active_index
SELECT i.game_id,i.component_chain,i.rule_validator,i.ui_schema
FROM aoo_compiled_room_create_index i
JOIN liangshan2003_publication_repair p
  ON p.game_id=i.game_id AND p.play_version=i.play_version AND p.region_code=i.region_code
JOIN aoo_compiled_index_active a
  ON a.game_id=i.game_id AND a.region_code=i.region_code
 AND a.play_version=i.play_version AND a.index_generation=i.index_generation
WHERE i.lifecycle_state='ACTIVE';

CREATE TEMPORARY TABLE liangshan2003_publication_assertion (
  assertion TINYINT NOT NULL CHECK (assertion=1)
);
INSERT INTO liangshan2003_publication_assertion(assertion)
SELECT IF(COUNT(*)=1,1,0) FROM liangshan2003_publication_active_release;
INSERT INTO liangshan2003_publication_assertion(assertion)
SELECT IF(COUNT(*)=1,1,0) FROM liangshan2003_publication_active_index;

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT @release_id,p.game_id,p.play_version,@generation,r.release_scope,
       r.catalog_snapshot,
       JSON_SET(COALESCE(r.rule_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSchema',JSON_OBJECT(
           'source',CONCAT(p.game_code,'-room-rule-workbook'),
           'sourceHash',p.source_hash,'gameId',p.game_id,'gameCode',p.game_code,
           'playVersion',p.play_version,'legacyFieldAliases',v.legacy_field_aliases,
           'fields',v.fields)),
       JSON_SET(COALESCE(r.ui_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       r.component_snapshot,
       SHA2(CONCAT(p.game_code,'|catalog|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|rules|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|ui|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|components|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash,'|',@generation),256),
       'ACTIVE',r.rollout_percent,r.created_by,@reason,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM liangshan2003_publication_repair p
JOIN liangshan2003_publication_validator v ON v.game_id=p.game_id
JOIN liangshan2003_publication_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),
  rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
  component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),
  rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),
  bundle_hash=VALUES(bundle_hash),status='ACTIVE',rollout_percent=VALUES(rollout_percent),
  reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at),
  retired_at=NULL;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT @release_id,p.region_code,r.region_rollout_percent,'ACTIVE'
FROM liangshan2003_publication_repair p
JOIN liangshan2003_publication_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,
  component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
  lifecycle_state,compiled_at,validated_at,activated_at)
SELECT p.game_id,p.region_code,p.play_version,@generation,@release_id,
       i.component_chain,
       JSON_SET(COALESCE(i.rule_validator,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.source',CONCAT(p.game_code,'-room-rule-workbook'),
         '$.sourceHash',p.source_hash,'$.legacyFieldAliases',v.legacy_field_aliases,
         '$.fields',v.fields),
       JSON_SET(COALESCE(i.ui_schema,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       SHA2(CONCAT(p.game_code,'|',p.region_code,'|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash,'|',@generation),256),
       'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM liangshan2003_publication_repair p
JOIN liangshan2003_publication_validator v ON v.game_id=p.game_id
JOIN liangshan2003_publication_active_index i ON i.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),
  lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),
  lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),
  activated_at=VALUES(activated_at),retired_at=NULL;

INSERT INTO aoo_compiled_index_active(
  game_id,region_code,play_version,index_generation,release_id,cache_epoch,
  activated_by,activation_reason,activated_at)
SELECT p.game_id,p.region_code,p.play_version,@generation,@release_id,
       CASE WHEN a.index_generation=@generation THEN a.cache_epoch ELSE a.cache_epoch+1 END,
       a.activated_by,@reason,CURRENT_TIMESTAMP(3)
FROM liangshan2003_publication_repair p
JOIN aoo_compiled_index_active a
  ON a.game_id=p.game_id AND a.region_code=p.region_code AND a.play_version=p.play_version
ON DUPLICATE KEY UPDATE
  play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),
  cache_epoch=GREATEST(aoo_compiled_index_active.cache_epoch,VALUES(cache_epoch)),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),
  activated_at=VALUES(activated_at);

-- Retire only prior rows for this exact canonical identity.  No history is
-- deleted, and routes, costs, identity sources and runtime tables are untouched.
UPDATE aoo_compiled_room_create_index i
JOIN liangshan2003_publication_repair p
  ON p.game_id=i.game_id AND p.play_version=i.play_version AND p.region_code=i.region_code
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.lifecycle_state='ACTIVE' AND i.index_generation<>@generation;

UPDATE aoo_game_release r
JOIN liangshan2003_publication_repair p
  ON p.game_id=r.game_id AND p.play_version=r.play_version
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE' AND r.release_id<>@release_id;

UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
JOIN liangshan2003_publication_repair p
  ON p.game_id=r.game_id AND p.play_version=r.play_version
SET rr.status='RETIRED'
WHERE rr.status='ACTIVE' AND r.release_id<>@release_id;

DROP TEMPORARY TABLE liangshan2003_publication_active_index;
DROP TEMPORARY TABLE liangshan2003_publication_active_release;
DROP TEMPORARY TABLE liangshan2003_publication_assertion;
DROP TEMPORARY TABLE liangshan2003_publication_validator;
DROP TEMPORARY TABLE liangshan2003_publication_repair;
COMMIT;
