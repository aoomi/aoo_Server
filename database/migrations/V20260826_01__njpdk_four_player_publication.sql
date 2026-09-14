-- Publish the already-supported four-seat NJPDK layout through a new compiled
-- release manifest. The physical Creator bundle is reused; the manifest digest
-- changes because its authoritative room-create schema changes.

INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT 629000004,game_id,play_version,4,release_scope,catalog_snapshot,rule_snapshot,
       JSON_SET(ui_snapshot,'$.playerCounts',JSON_ARRAY(2,3,4)),component_snapshot,
       catalog_hash,rule_hash,SHA2('njpdk-ui-v4-four-player',256),component_hash,
       SHA2('poker01-prefab-v3|room-index-v4',256),'ACTIVE',100,created_by,
       'Publish authoritative 2/3/4-player NJPDK layouts',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release
WHERE release_id=629000003;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT 629000004,region_code,100,'ACTIVE'
FROM aoo_game_release_region
WHERE release_id=629000003;

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at)
SELECT game_id,region_code,play_version,4,629000004,component_chain,rule_validator,
       JSON_SET(ui_schema,'$.fields[1].options',JSON_ARRAY(
         JSON_OBJECT('value',2,'label','2人'),
         JSON_OBJECT('value',3,'label','3人'),
         JSON_OBJECT('value',4,'label','4人'))),
       SHA2('njpdk-index-v4-four-player',256),bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index
WHERE game_id=629 AND region_code='CN-51' AND play_version='legacy-equivalent-1' AND index_generation=3;

UPDATE aoo_compiled_index_active
SET index_generation=4,release_id=629000004,cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason='Publish authoritative 2/3/4-player NJPDK layouts',activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id=629 AND region_code='CN-51' AND play_version='legacy-equivalent-1';

UPDATE aoo_compiled_room_create_index
SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND region_code='CN-51' AND play_version='legacy-equivalent-1'
  AND index_generation<4 AND lifecycle_state='ACTIVE';

UPDATE aoo_game_release
SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND release_version<4 AND status='ACTIVE';

INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
SELECT 629,'legacy-equivalent-1','CN-51',round_count,4,'OWNER','ROOM_CARD',0,1,
       SHA2(CONCAT('629|legacy-equivalent-1|',round_count,'|4|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 round_count UNION ALL SELECT 10 UNION ALL SELECT 16) rounds
JOIN aoo_play_version play
  ON play.game_id=629 AND play.play_version='legacy-equivalent-1';
