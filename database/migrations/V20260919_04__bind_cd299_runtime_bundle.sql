-- Bind CD299 Catalog to its real Creator bundle and bundle-relative prefabs.
SET @cd299_release_id = 6302026091904;
SET @cd299_generation = 2026091904;

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT @cd299_release_id,game_id,play_version,2,'REGIONAL',catalog_snapshot,rule_snapshot,
  JSON_SET(ui_snapshot,'$.bundle','poker-cx','$.scene','GameRoom2D','$.landscapePrefab','CD299/Prefab/Landscape/CD299RoomLandscape','$.portraitPrefab','CD299/Prefab/Portrait/CD299RoomPortrait'),
  component_snapshot,catalog_hash,rule_hash,SHA2('CD299-ui-poker-cx-v2',256),component_hash,SHA2('CD299-runtime-poker-cx-v2',256),'ACTIVE',100,1,'Bind CD299 to poker-cx Creator bundle',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release WHERE release_id=(SELECT release_id FROM aoo_compiled_index_active WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0');

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(@cd299_release_id,'CN-51-01',100,'ACTIVE');
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
SELECT game_id,region_code,play_version,@cd299_generation,@cd299_release_id,component_chain,rule_validator,
  JSON_SET(ui_schema,'$.bundle','poker-cx','$.scene','GameRoom2D','$.landscapePrefab','CD299/Prefab/Landscape/CD299RoomLandscape','$.portraitPrefab','CD299/Prefab/Portrait/CD299RoomPortrait'),
  SHA2(CONCAT('CD299|',@cd299_generation,'|poker-cx'),256),SHA2('CD299-runtime-poker-cx-v2',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0' AND index_generation=(SELECT index_generation FROM aoo_compiled_index_active WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0');
UPDATE aoo_compiled_index_active SET index_generation=@cd299_generation,release_id=@cd299_release_id,cache_epoch=cache_epoch+1,activated_by=1,activation_reason='Bind CD299 to poker-cx Creator bundle',activated_at=CURRENT_TIMESTAMP(3) WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0';
UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=630 AND region_code='CN-51-01' AND play_version='cd299-v1.0.0' AND index_generation<>@cd299_generation AND lifecycle_state='ACTIVE';
UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=630 AND play_version='cd299-v1.0.0' AND release_id<>@cd299_release_id AND status='ACTIVE';
