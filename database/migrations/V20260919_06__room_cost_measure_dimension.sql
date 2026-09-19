-- Room cost policies must use the same authoritative measure as room rules.
-- The legacy round_count column remains populated during compatibility rollout.
ALTER TABLE aoo_room_cost_policy
  ADD COLUMN measure_kind VARCHAR(24) NOT NULL DEFAULT 'ROUND_COUNT' AFTER region_code,
  ADD COLUMN measure_value INT UNSIGNED NOT NULL DEFAULT 1 AFTER measure_kind;

UPDATE aoo_room_cost_policy SET measure_kind='ROUND_COUNT',measure_value=round_count;
ALTER TABLE aoo_room_cost_policy
  ADD CONSTRAINT chk_room_cost_measure_kind CHECK (measure_kind IN ('ROUND_COUNT','DURATION_MINUTES')),
  ADD KEY idx_room_cost_measure_lookup(region_code,game_id,play_version,measure_kind,measure_value,player_count,status);

-- CD299 is time-boxed. Retire the historical borrowed "8 rounds" dimension and
-- publish each duration exposed by its authoritative create-room schema.
UPDATE aoo_room_cost_policy SET status='RETIRED'
WHERE game_id=630 AND play_version='cd299-v1.0.0';
INSERT INTO aoo_room_cost_policy(
 game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,
 payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 630,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',duration_minutes,duration_minutes,8,
 'OWNER','ROOM_CARD',0,2,SHA2(CONCAT('630|cd299-v1.0.0|DURATION_MINUTES|',duration_minutes,'|8|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 30 duration_minutes UNION ALL SELECT 45 UNION ALL SELECT 60) durations
ON DUPLICATE KEY UPDATE measure_kind=VALUES(measure_kind),measure_value=VALUES(measure_value),
 cost_minor=VALUES(cost_minor),policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';

-- Publish the measure contract immutably with a new compiled generation.
SET @cd299_measure_release = 6302026091907;
SET @cd299_measure_generation = 2026091907;
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT @cd299_measure_release,game_id,play_version,5,release_scope,catalog_snapshot,
 JSON_SET(rule_snapshot,'$.roomMeasureKind','DURATION_MINUTES'),ui_snapshot,component_snapshot,catalog_hash,
 SHA2(CONCAT(rule_hash,'|DURATION_MINUTES'),256),ui_hash,component_hash,SHA2(CONCAT(bundle_hash,'|DURATION_MINUTES'),256),'ACTIVE',100,1,
 'Publish duration-based room billing contract',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release WHERE release_id=(SELECT release_id FROM aoo_compiled_index_active WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0');
INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES(@cd299_measure_release,'CN-51-01',100,'ACTIVE');
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
SELECT game_id,region_code,play_version,@cd299_measure_generation,@cd299_measure_release,component_chain,
 JSON_SET(rule_validator,'$.roomMeasureKind','DURATION_MINUTES'),ui_schema,
 SHA2(CONCAT(lookup_hash,'|DURATION_MINUTES'),256),bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0'
 AND index_generation=(SELECT index_generation FROM aoo_compiled_index_active WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0');
UPDATE aoo_compiled_index_active SET index_generation=@cd299_measure_generation,release_id=@cd299_measure_release,
 cache_epoch=cache_epoch+1,activated_by=1,activation_reason='Publish duration-based room billing contract',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0';
UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0' AND index_generation<>@cd299_measure_generation AND lifecycle_state='ACTIVE';
UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
WHERE game_id=630 AND play_version='cd299-v1.0.0' AND release_id<>@cd299_measure_release AND status='ACTIVE';
