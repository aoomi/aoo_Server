-- JSON_QUOTE inside JSON_SET stored embedded quote characters. Republish the
-- two LS201 enum defaults as ordinary JSON strings.

START TRANSACTION;

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 900052026090708,a.game_id,a.play_version,2026090708,r.release_scope,r.catalog_snapshot,
 r.rule_snapshot,r.ui_snapshot,r.component_snapshot,r.catalog_hash,
 SHA2('90005|2026090708|string-defaults',256),SHA2('90005|2026090708|ui',256),
 r.component_hash,SHA2('90005|2026090708|pdk-common-room',256),'ACTIVE',100,r.created_by,
 'Fix LS201 enum defaults',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=a.release_id
WHERE a.game_id=90005;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 900052026090708,region_code,100,'ACTIVE' FROM aoo_compiled_index_active WHERE game_id=90005;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT a.game_id,a.region_code,a.play_version,2026090708,900052026090708,i.component_chain,
 JSON_SET(i.rule_validator,'$.fields[4].defaultValue','no_compare',
   '$.fields[5].defaultValue','dealer_first'),
 JSON_SET(i.ui_schema,'$.fields[4].defaultValue','no_compare',
   '$.fields[5].defaultValue','dealer_first'),
 SHA2('90005|2026090708|index',256),SHA2('90005|2026090708|pdk-common-room',256),
 'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
 ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
 AND i.index_generation=a.index_generation WHERE a.game_id=90005;

UPDATE aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a
 ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.game_id=90005 AND i.index_generation<>2026090708 AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=90005 AND release_id<>900052026090708 AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED' WHERE r.game_id=90005 AND r.release_id<>900052026090708
 AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active SET index_generation=2026090708,release_id=900052026090708,
 cache_epoch=cache_epoch+1,activated_by=1,activation_reason='LS201 enum defaults fixed',
 activated_at=CURRENT_TIMESTAMP(3) WHERE game_id=90005;

UPDATE aoo_published_game_configuration SET release_id=900052026090708,
 reason='LS201 enum defaults fixed',created_at=CURRENT_TIMESTAMP(3) WHERE game_id=90005;

COMMIT;
