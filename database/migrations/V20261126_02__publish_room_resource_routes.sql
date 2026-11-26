-- Publish immutable ACTIVE projections with the client resource route required by room creation.
-- The route is derived from the current game family; historical release/index rows stay immutable.
DELIMITER $$
CREATE PROCEDURE publish_room_resource_routes_v20261126_02()
BEGIN
  DECLARE expected_count INT DEFAULT 0;
  DECLARE published_count INT DEFAULT 0;

  START TRANSACTION;

  DROP TABLE IF EXISTS room_resource_route_candidate_v20261126_02;
  CREATE TABLE room_resource_route_candidate_v20261126_02 AS
  SELECT a.game_id,a.region_code,a.play_version,
         a.game_id * 1000000000 + 2026112602 AS new_release_id,
         (SELECT MAX(r2.release_version) + 1 FROM aoo_game_release r2
           WHERE r2.game_id=a.game_id AND r2.play_version=a.play_version) AS new_release_version,
         (SELECT MAX(i2.index_generation) + 1 FROM aoo_compiled_room_create_index i2
           WHERE i2.game_id=a.game_id AND i2.region_code=a.region_code
             AND i2.play_version=a.play_version) AS new_generation,
         r.snapshot_schema_version,r.card_codec_version,r.event_interpreter_version,r.protocol_version,
         r.source_hash,r.release_scope,r.catalog_snapshot,r.rule_snapshot,r.component_snapshot,
         r.catalog_hash,r.rule_hash,r.ui_hash,r.component_hash,r.created_by,
         i.component_chain,i.rule_validator,
         JSON_SET(i.ui_schema,
           '$.bundle', CASE
             WHEN a.game_id=5 THEN 'poker-nn'
             WHEN a.game_id=9 THEN 'poker-zjh'
             WHEN a.game_id=1001 THEN 'games-mahjong'
             WHEN a.game_id BETWEEN 2001 AND 2028 THEN 'paodekuai-common'
             WHEN a.game_id=2999 THEN 'poker-cx'
           END,
           '$.scene','GameRoom2D') AS new_ui_schema
    FROM aoo_compiled_index_active a
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id AND i.region_code=a.region_code
     AND i.play_version=a.play_version AND i.index_generation=a.index_generation
     AND i.release_id=a.release_id
    JOIN aoo_game_release r ON r.release_id=a.release_id
   WHERE a.game_id IN (5,9,1001,2999)
      OR a.game_id BETWEEN 2001 AND 2028;

  SELECT COUNT(*) INTO expected_count FROM room_resource_route_candidate_v20261126_02;
  IF expected_count<>32
     OR EXISTS (SELECT 1 FROM room_resource_route_candidate_v20261126_02
                 WHERE JSON_UNQUOTE(JSON_EXTRACT(new_ui_schema,'$.bundle')) IS NULL
                    OR JSON_UNQUOTE(JSON_EXTRACT(new_ui_schema,'$.scene'))<>'GameRoom2D')
     OR EXISTS (SELECT 1 FROM room_resource_route_candidate_v20261126_02 c
                 WHERE EXISTS (SELECT 1 FROM aoo_game_release r WHERE r.release_id=c.new_release_id)) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room resource route publication precondition failed';
  END IF;

  ALTER TABLE room_resource_route_candidate_v20261126_02 ADD COLUMN new_bundle_hash CHAR(64);
  UPDATE room_resource_route_candidate_v20261126_02
     SET new_bundle_hash=SHA2(CAST(JSON_OBJECT(
       'catalogSnapshot',JSON_EXTRACT(catalog_snapshot,'$'),
       'ruleValidator',JSON_EXTRACT(rule_validator,'$'),
       'uiSchema',JSON_EXTRACT(new_ui_schema,'$'),
       'componentSnapshot',JSON_EXTRACT(component_snapshot,'$'),
       'componentChain',JSON_EXTRACT(component_chain,'$')
     ) AS CHAR CHARACTER SET utf8mb4),256);

  INSERT INTO aoo_game_release(
    release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,
    protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,
    component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,
    created_by,reason,validated_at,activated_at)
  SELECT new_release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,
         protocol_version,source_hash,new_release_version,release_scope,catalog_snapshot,rule_snapshot,new_ui_schema,
         component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,new_bundle_hash,'ACTIVE',100,
         created_by,'Publish canonical room resource route',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
    FROM room_resource_route_candidate_v20261126_02;

  INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
  SELECT new_release_id,region_code,100,'ACTIVE' FROM room_resource_route_candidate_v20261126_02;

  INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,
    lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
  SELECT game_id,region_code,play_version,new_generation,new_release_id,component_chain,rule_validator,new_ui_schema,
         SHA2(CONCAT(game_id,'|',region_code,'|',play_version,'|',source_hash),256),new_bundle_hash,
         'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
    FROM room_resource_route_candidate_v20261126_02;

  INSERT INTO aoo_compiled_index_active(
    game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
  SELECT game_id,region_code,play_version,new_generation,new_release_id,1,created_by,
         'Activate canonical room resource route',CURRENT_TIMESTAMP(3)
    FROM room_resource_route_candidate_v20261126_02
  ON DUPLICATE KEY UPDATE
    index_generation=VALUES(index_generation),release_id=VALUES(release_id),
    cache_epoch=aoo_compiled_index_active.cache_epoch+1,
    activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

  SELECT COUNT(*) INTO published_count
    FROM room_resource_route_candidate_v20261126_02 c
    JOIN aoo_compiled_index_active a
      ON a.game_id=c.game_id AND a.region_code=c.region_code AND a.play_version=c.play_version
     AND a.index_generation=c.new_generation AND a.release_id=c.new_release_id
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
     AND i.index_generation=a.index_generation AND i.release_id=a.release_id
    JOIN aoo_game_release r ON r.release_id=a.release_id
   WHERE i.ui_schema=r.ui_snapshot
     AND i.bundle_hash=r.bundle_hash
     AND i.bundle_hash=c.new_bundle_hash
     AND JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.bundle')) IS NOT NULL
     AND JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.scene'))='GameRoom2D';

  IF published_count<>expected_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room resource route publication verification failed';
  END IF;

  DROP TABLE room_resource_route_candidate_v20261126_02;
  COMMIT;
END$$
DELIMITER ;
CALL publish_room_resource_routes_v20261126_02();
DROP PROCEDURE publish_room_resource_routes_v20261126_02;
