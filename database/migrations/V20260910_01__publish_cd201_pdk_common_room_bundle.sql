-- CD201 must use the one published PDK common-room bundle. Keep the prior
-- release/index immutable so rooms created under it remain recoverable.
START TRANSACTION;

SET @cd201_source_generation = (
  SELECT index_generation FROM aoo_compiled_index_active
  WHERE game_id=8 AND region_code='CN-51-01' AND play_version='1.0.0'
);
SET @cd201_source_release = (
  SELECT release_id FROM aoo_compiled_index_active
  WHERE game_id=8 AND region_code='CN-51-01' AND play_version='1.0.0'
);

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
  ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 2034091001,r.game_id,r.play_version,2026091001,r.release_scope,
  r.catalog_snapshot,r.rule_snapshot,
  JSON_SET(r.ui_snapshot,'$.bundle','pdk-common-room','$.scene','GameRoom2D'),
  r.component_snapshot,r.catalog_hash,r.rule_hash,
  SHA2(CONCAT('CD201|2026091001|pdk-common-room|',r.ui_hash),256),r.component_hash,
  SHA2(CONCAT('CD201|2026091001|pdk-common-room|',r.bundle_hash),256),
  'ACTIVE',100,r.created_by,'Publish CD201 PDK common-room bundle',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release r
WHERE r.release_id=@cd201_source_release;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 2034091001,region_code,rollout_percent,'ACTIVE'
FROM aoo_game_release_region
WHERE release_id=@cd201_source_release;

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,
  component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
  validated_at,activated_at)
SELECT i.game_id,i.region_code,i.play_version,2026091001,2034091001,
  i.component_chain,i.rule_validator,
  JSON_SET(i.ui_schema,'$.bundle','pdk-common-room','$.scene','GameRoom2D'),
  SHA2(CONCAT('CD201|2026091001|pdk-common-room|',i.lookup_hash),256),
  SHA2(CONCAT('CD201|2026091001|pdk-common-room|',i.bundle_hash),256),
  'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index i
WHERE i.game_id=8 AND i.region_code='CN-51-01' AND i.play_version='1.0.0'
  AND i.index_generation=@cd201_source_generation AND i.release_id=@cd201_source_release;

UPDATE aoo_compiled_index_active
SET index_generation=2026091001,release_id=2034091001,cache_epoch=cache_epoch+1,
  activated_by=1,activation_reason='Publish CD201 PDK common-room bundle',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=8 AND region_code='CN-51-01' AND play_version='1.0.0'
  AND index_generation=@cd201_source_generation AND release_id=@cd201_source_release;

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=8 AND region_code='CN-51-01' AND play_version='1.0.0'
  AND index_generation=@cd201_source_generation AND release_id=@cd201_source_release
  AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE release_id=@cd201_source_release AND status='ACTIVE';

UPDATE aoo_game_release_region
SET status='RETIRED'
WHERE release_id=@cd201_source_release AND status='ACTIVE';

UPDATE aoo_published_game_configuration
SET release_id=2034091001,
  configuration_payload=JSON_SET(configuration_payload,'$.bundle','pdk-common-room','$.scene','GameRoom2D'),
  reason='Publish CD201 PDK common-room bundle',created_at=CURRENT_TIMESTAMP(3)
WHERE game_id=8;

COMMIT;
