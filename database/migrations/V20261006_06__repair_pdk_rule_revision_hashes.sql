-- Repair the four current PDK publications whose active UI snapshot was
-- copied without the authoritative workbook source hash.  Keep the existing
-- rule/UI fields and component chain unchanged; publish one immutable
-- generation carrying only the verified source identity needed by Hall's
-- rule-revision precondition.
--
-- Neijiang 2002 is deliberately not included here.  Its current active cost
-- policy set is governed by the identity's published player-count choices;
-- this migration must not invent a billing dimension or default.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

CREATE TEMPORARY TABLE pdk_rule_revision_repair (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  game_code VARCHAR(64) NOT NULL UNIQUE,
  play_version VARCHAR(64) NOT NULL UNIQUE,
  source_hash CHAR(64) NOT NULL,
  region_code VARCHAR(32) NOT NULL
);

INSERT INTO pdk_rule_revision_repair(game_id,game_code,play_version,source_hash,region_code)
VALUES
  (2001,'chengdu2001','chengdu2001-v1.0.0','3c85d2454b22249cec48df543d292fe1b55b77acfe43987007ef91891b71a72e','CN-51-01'),
  (2004,'mianning2004','mianning2004-v1.0.0','648e76a9a41930aad64a1bdb4be7a921df04faf57d60736b9d832426ba2705d7','CN-51-34-33'),
  (2005,'anyue2005','anyue2005-v1.0.0','4a98640193e56317b8b84d5ad6a1b22460b11077305ec6192b1fc82d2537d241','CN-51-20-21'),
  (2006,'panzhihua2006','panzhihua2006-v1.0.0','d0d4dc518b8acbae16df64756c9e0b6f32278568b395eee01b4809f9657219e7','CN-51-04');

-- Capture the current active publication before installing the new generation.
-- The explicit identity/region join prevents a fallback to another regional
-- row from silently supplying a rule source.
CREATE TEMPORARY TABLE pdk_rule_revision_active_release AS
SELECT r.release_id,r.game_id,r.play_version,r.release_scope,r.catalog_snapshot,
       r.rule_snapshot,r.ui_snapshot,r.component_snapshot,r.rollout_percent,
       r.created_by,rr.region_code,rr.rollout_percent AS region_rollout_percent
FROM aoo_game_release r
JOIN pdk_rule_revision_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
JOIN aoo_game_release_region rr ON rr.release_id=r.release_id
  AND rr.region_code=p.region_code AND rr.status='ACTIVE'
WHERE r.status='ACTIVE';

CREATE TEMPORARY TABLE pdk_rule_revision_active_index AS
SELECT i.game_id,i.region_code,i.play_version,i.component_chain,i.rule_validator,
       i.ui_schema
FROM aoo_compiled_room_create_index i
JOIN pdk_rule_revision_repair p ON p.game_id=i.game_id AND p.play_version=i.play_version
  AND p.region_code=i.region_code
JOIN aoo_compiled_index_active a ON a.game_id=i.game_id AND a.region_code=i.region_code
  AND a.play_version=i.play_version AND a.index_generation=i.index_generation
WHERE i.lifecycle_state='ACTIVE';

-- A missing source row is an invalid publication state, not an invitation to
-- synthesize one.  Enforced temporary CHECK rows fail the migration before
-- writes if any allowlisted identity lacks exactly one active preimage.
CREATE TEMPORARY TABLE pdk_rule_revision_assertion(
  assertion TINYINT NOT NULL CHECK (assertion=1)
);
INSERT INTO pdk_rule_revision_assertion(assertion)
SELECT IF(COUNT(*)=4,1,0) FROM pdk_rule_revision_active_release;
INSERT INTO pdk_rule_revision_assertion(assertion)
SELECT IF(COUNT(*)=4,1,0) FROM pdk_rule_revision_active_index;

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT p.game_id*1000000000+2026100606,p.game_id,p.play_version,2026100606,
       r.release_scope,r.catalog_snapshot,r.rule_snapshot,
       JSON_SET(COALESCE(r.ui_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,
         '$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,
         '$.roomRuleViewHash',p.source_hash),
       r.component_snapshot,
       SHA2(CONCAT(p.game_code,'|catalog|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|rules|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|ui|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|components|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash),256),
       'ACTIVE',r.rollout_percent,r.created_by,
       CONCAT('Repair canonical PDK rule revision hash ',p.game_code),
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_rule_revision_repair p
JOIN pdk_rule_revision_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),
  rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
  component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),
  rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),
  component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),
  status='ACTIVE',rollout_percent=VALUES(rollout_percent),
  reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at),
  retired_at=NULL;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT p.game_id*1000000000+2026100606,p.region_code,r.region_rollout_percent,'ACTIVE'
FROM pdk_rule_revision_repair p
JOIN pdk_rule_revision_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,
  component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
  lifecycle_state,compiled_at,validated_at,activated_at)
SELECT p.game_id,p.region_code,p.play_version,2026100606,
       p.game_id*1000000000+2026100606,
       i.component_chain,i.rule_validator,
       JSON_SET(COALESCE(i.ui_schema,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,
         '$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,
         '$.roomRuleViewHash',p.source_hash),
       SHA2(CONCAT(p.game_code,'|',p.region_code,'|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash),256),
       'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_rule_revision_repair p
JOIN pdk_rule_revision_active_index i ON i.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),
  lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),
  lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),
  activated_at=VALUES(activated_at),retired_at=NULL;

INSERT INTO aoo_compiled_index_active(
  game_id,region_code,play_version,index_generation,release_id,cache_epoch,
  activated_by,activation_reason,activated_at)
SELECT p.game_id,p.region_code,p.play_version,2026100606,
       p.game_id*1000000000+2026100606,
       CASE WHEN a.index_generation=2026100606 THEN a.cache_epoch ELSE a.cache_epoch+1 END,
       a.activated_by,
       CONCAT('Repair canonical PDK rule revision hash ',p.game_code),CURRENT_TIMESTAMP(3)
FROM pdk_rule_revision_repair p
JOIN aoo_compiled_index_active a ON a.game_id=p.game_id AND a.region_code=p.region_code
ON DUPLICATE KEY UPDATE
  play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),cache_epoch=GREATEST(aoo_compiled_index_active.cache_epoch,VALUES(cache_epoch)),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),
  activated_at=VALUES(activated_at);

UPDATE aoo_compiled_room_create_index i
JOIN pdk_rule_revision_repair p ON p.game_id=i.game_id AND p.play_version=i.play_version
  AND p.region_code=i.region_code
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.lifecycle_state='ACTIVE' AND i.index_generation<>2026100606;

UPDATE aoo_game_release r
JOIN pdk_rule_revision_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE' AND r.release_id<>p.game_id*1000000000+2026100606;

UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
JOIN pdk_rule_revision_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET rr.status='RETIRED'
WHERE rr.status='ACTIVE' AND r.release_id<>p.game_id*1000000000+2026100606;

DROP TEMPORARY TABLE pdk_rule_revision_active_index;
DROP TEMPORARY TABLE pdk_rule_revision_active_release;
DROP TEMPORARY TABLE pdk_rule_revision_assertion;
DROP TEMPORARY TABLE pdk_rule_revision_repair;
COMMIT;
