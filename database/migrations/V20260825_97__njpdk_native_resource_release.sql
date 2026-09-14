-- Publish NJPDK against its real Creator 3.8.8 bundle and common 2D room scene.
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 629000003,629,'legacy-equivalent-1',3,'REGIONAL',JSON_OBJECT('provider','business.global.pk.njpdk.NJPDKGameProvider','gameId',629),JSON_OBJECT('family','poker-pao-de-kuai','schema','canonical-room-v2'),JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D'),JSON_OBJECT('providerVersion','legacy-equivalent-1'),SHA2('njpdk-catalog-v3',256),SHA2('njpdk-rules-v3',256),SHA2('njpdk-ui-v3',256),SHA2('njpdk-provider-v3',256),SHA2('poker01-prefab-v3',256),'ACTIVE',100,1,'Creator 3.8.8 native resource route publication',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_catalog g JOIN aoo_play_version p ON p.game_id=g.game_id AND p.play_version='legacy-equivalent-1'
WHERE g.game_id=629;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT r.release_id,'CN-51',100,'ACTIVE' FROM aoo_game_release r JOIN aoo_region region ON region.region_code='CN-51'
WHERE r.release_id=629000003;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at)
SELECT 629,'CN-51','legacy-equivalent-1',3,629000003,
 JSON_ARRAY('business.global.pk.njpdk.NJPDKGameProvider@legacy-equivalent-1'),
 JSON_OBJECT('seatLimit',JSON_OBJECT('min',2,'max',4)),
 JSON_OBJECT('bundle','poker01-prefab','scene','GameRoom2D','fields',JSON_ARRAY()),SHA2('njpdk-index-v3',256),SHA2('poker01-prefab-v3',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release_region rr WHERE rr.release_id=629000003 AND rr.region_code='CN-51';

UPDATE aoo_compiled_index_active
SET index_generation=3,release_id=629000003,cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason='Creator 3.8.8 native resource route publication',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=629 AND region_code='CN-51';

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND region_code='CN-51' AND play_version='legacy-equivalent-1'
  AND index_generation<3 AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND release_version<3 AND status='ACTIVE';
