-- PDK live rooms have one authoritative Creator bundle.  A new immutable
-- release/index is required because mutating an activated publication would
-- make room recovery dependent on whichever node refreshed its cache first.
DROP TEMPORARY TABLE IF EXISTS pdk_common_room_publish;
CREATE TEMPORARY TABLE pdk_common_room_publish AS
SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,
       a.release_id old_release_id,
       (SELECT COALESCE(MAX(r.release_version),0)+1 FROM aoo_game_release r
         WHERE r.game_id=a.game_id AND r.play_version=a.play_version) new_release_version,
       (SELECT COALESCE(MAX(i.index_generation),0)+1 FROM aoo_compiled_room_create_index i
         WHERE i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version) new_generation
FROM aoo_compiled_index_active a
WHERE (a.game_id=8 AND a.play_version='1.0.0')
   OR (a.game_id=629 AND a.play_version='legacy-equivalent-1');

INSERT INTO aoo_game_release(
    release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
    catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,
    rollout_percent,created_by,reason,validated_at,activated_at)
SELECT p.game_id*1000000+p.new_release_version,r.game_id,r.play_version,p.new_release_version,r.release_scope,
       r.catalog_snapshot,r.rule_snapshot,
       JSON_SET(r.ui_snapshot,'$.bundle','pdk-common-room','$.scene','GameRoom2D'),
       r.component_snapshot,r.catalog_hash,r.rule_hash,
       SHA2(CONCAT('pdk-common-room|',r.ui_hash),256),r.component_hash,
       SHA2(CONCAT('pdk-common-room|',r.bundle_hash),256),'ACTIVE',100,r.created_by,
       'Publish authoritative PDK_CommonRoom bundle',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_common_room_publish p JOIN aoo_game_release r ON r.release_id=p.old_release_id;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT p.game_id*1000000+p.new_release_version,rr.region_code,rr.rollout_percent,'ACTIVE'
FROM pdk_common_room_publish p JOIN aoo_game_release_region rr ON rr.release_id=p.old_release_id;

INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,
    component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
    lifecycle_state,validated_at,activated_at)
SELECT p.game_id,p.region_code,p.play_version,p.new_generation,
       p.game_id*1000000+p.new_release_version,i.component_chain,i.rule_validator,
       JSON_SET(i.ui_schema,'$.bundle','pdk-common-room','$.scene','GameRoom2D'),
       SHA2(CONCAT(p.game_id,'|',p.region_code,'|',p.play_version,'|pdk-common-room'),256),
       SHA2(CONCAT('pdk-common-room|',i.bundle_hash),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_common_room_publish p JOIN aoo_compiled_room_create_index i
  ON i.game_id=p.game_id AND i.region_code=p.region_code
 AND i.play_version=p.play_version AND i.index_generation=p.old_generation;

UPDATE aoo_compiled_index_active a JOIN pdk_common_room_publish p
  ON p.game_id=a.game_id AND p.region_code=a.region_code AND p.play_version=a.play_version
SET a.index_generation=p.new_generation,a.release_id=p.game_id*1000000+p.new_release_version,
    a.cache_epoch=a.cache_epoch+1,a.activated_by=1,
    a.activation_reason='Publish authoritative PDK_CommonRoom bundle',a.activated_at=CURRENT_TIMESTAMP(3);

UPDATE aoo_compiled_room_create_index i JOIN pdk_common_room_publish p
  ON p.game_id=i.game_id AND p.region_code=i.region_code AND p.play_version=i.play_version
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.index_generation=p.old_generation AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release r JOIN pdk_common_room_publish p ON p.old_release_id=r.release_id
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN pdk_common_room_publish p ON p.old_release_id=rr.release_id
SET rr.status='RETIRED' WHERE rr.status='ACTIVE';

DROP TEMPORARY TABLE pdk_common_room_publish;
