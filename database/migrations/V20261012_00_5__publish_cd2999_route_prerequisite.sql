-- Forward-only prerequisite between the signed current identity projection
-- (V20261012_00) and the pricing projection (V20261012_01).
--
-- V20261012_01 must never be applied while the current CD2999 publication has
-- no task-only route.  This patch is deliberately separate from the complete
-- route/schema contract in V20261012_02 so Flyway's deterministic order is:
-- identity -> route prerequisite -> pricing -> full route/schema projection.
-- Historical 630 remains Compatibility-only and is not deleted or rewritten.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_cd2999_route_prerequisite_v20261012_00_5()
BEGIN
  DECLARE release_matches INT DEFAULT 0;
  DECLARE index_matches INT DEFAULT 0;
  DECLARE pointer_matches INT DEFAULT 0;
  DECLARE route_matches INT DEFAULT 0;
  DECLARE signed_source_hash CHAR(64) DEFAULT 'f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5';
  DECLARE route_endpoint VARCHAR(512) DEFAULT 'ws://127.0.0.1:61384/api/v2/gateway/ws';

  SELECT COUNT(*) INTO release_matches
    FROM aoo_game_release
   WHERE release_id=2999001 AND game_id=2999
     AND play_version='cd299-v1.0.0' AND status='ACTIVE'
     AND aoo_game_release.source_hash=signed_source_hash;
  SELECT COUNT(*) INTO index_matches
    FROM aoo_compiled_room_create_index
   WHERE game_id=2999 AND region_code='CN-51-01'
     AND play_version='cd299-v1.0.0' AND index_generation=3999001
     AND release_id=2999001 AND lifecycle_state='ACTIVE';
  SELECT COUNT(*) INTO pointer_matches
    FROM aoo_compiled_index_active
   WHERE game_id=2999 AND region_code='CN-51-01'
     AND play_version='cd299-v1.0.0' AND index_generation=3999001
     AND release_id=2999001;
  IF release_matches<>1 OR index_matches<>1 OR pointer_matches<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_ROUTE_PREREQUISITE_CURRENT_IDENTITY_MISSING';
  END IF;

  UPDATE aoo_game_service_route SET status='OFFLINE'
   WHERE game_id=630 AND status='ACTIVE';

  INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
  VALUES(2999001,2999,'cd299-v1.0.0',route_endpoint,100,'ACTIVE')
  ON DUPLICATE KEY UPDATE
      game_id=VALUES(game_id),play_version=VALUES(play_version),
      endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

  SELECT COUNT(*) INTO route_matches
    FROM aoo_game_service_route
   WHERE game_id=2999 AND play_version='cd299-v1.0.0'
     AND endpoint=route_endpoint AND priority=100 AND status='ACTIVE';
  IF route_matches<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_ROUTE_PREREQUISITE_PUBLICATION_FAILED';
  END IF;
END$$

CALL publish_cd2999_route_prerequisite_v20261012_00_5()$$
DROP PROCEDURE publish_cd2999_route_prerequisite_v20261012_00_5$$

DELIMITER ;
