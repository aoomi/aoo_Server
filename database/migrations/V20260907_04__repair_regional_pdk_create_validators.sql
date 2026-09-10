-- Repair the two regional PDK publications created before Hall standardized on
-- the field-list validator shape. Publish a new immutable generation instead of
-- mutating generation 2026090703, which may already be referenced by audit data.

START TRANSACTION;

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT CASE a.game_id WHEN 629 THEN 6292026090704 ELSE 900052026090704 END,
 a.game_id,a.play_version,2026090704,r.release_scope,r.catalog_snapshot,r.rule_snapshot,
 r.ui_snapshot,r.component_snapshot,r.catalog_hash,
 SHA2(CONCAT(a.game_id,'|2026090704|field-list-validator'),256),r.ui_hash,r.component_hash,
 SHA2(CONCAT(a.game_id,'|2026090704|pdk-common-room'),256),'ACTIVE',100,r.created_by,
 'Repair regional PDK Hall rule validator shape',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=a.release_id
WHERE a.game_id IN(629,90005);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT CASE a.game_id WHEN 629 THEN 6292026090704 ELSE 900052026090704 END,
 a.region_code,100,'ACTIVE' FROM aoo_compiled_index_active a WHERE a.game_id IN(629,90005);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT a.game_id,a.region_code,a.play_version,2026090704,
 CASE a.game_id WHEN 629 THEN 6292026090704 ELSE 900052026090704 END,
 i.component_chain,i.ui_schema,i.ui_schema,
 SHA2(CONCAT(a.game_id,'|2026090704|index'),256),
 SHA2(CONCAT(a.game_id,'|2026090704|pdk-common-room'),256),'ACTIVE',
 CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
 ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
 AND i.index_generation=a.index_generation WHERE a.game_id IN(629,90005);

UPDATE aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a
 ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version
 SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.game_id IN(629,90005) AND i.index_generation<>2026090704 AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id IN(629,90005) AND release_id NOT IN(6292026090704,900052026090704)
 AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
 SET rr.status='RETIRED' WHERE r.game_id IN(629,90005)
 AND r.release_id NOT IN(6292026090704,900052026090704) AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active SET index_generation=2026090704,
 release_id=CASE game_id WHEN 629 THEN 6292026090704 ELSE 900052026090704 END,
 cache_epoch=cache_epoch+1,activated_by=1,
 activation_reason='Regional PDK Hall validator repair',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id IN(629,90005);

UPDATE aoo_published_game_configuration SET
 release_id=CASE game_id WHEN 629 THEN 6292026090704 ELSE 900052026090704 END,
 reason='Regional PDK Hall validator repair',created_at=CURRENT_TIMESTAMP(3)
WHERE game_id IN(629,90005);

-- The original Neijiang seed only published 12-round costs in the city scope.
-- Mirror the already-approved Sichuan 8/12/16 policies into the active city scope.
INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,player_count,
 payer_mode,currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version,content_hash,status)
SELECT game_id,play_version,'CN-51-10',round_count,player_count,payer_mode,currency_code,
 cost_minor,club_cost_minor,union_cost_minor,policy_version,
 SHA2(CONCAT(content_hash,'|CN-51-10'),256),status
FROM aoo_room_cost_policy source
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND region_code='CN-51'
  AND round_count IN(8,12,16)
ON DUPLICATE KEY UPDATE currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),
 club_cost_minor=VALUES(club_cost_minor),union_cost_minor=VALUES(union_cost_minor),
 policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status=VALUES(status);

COMMIT;
