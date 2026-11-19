-- Forward-only publication of the current CD2999 pricing source.
--
-- The values are the six user-authorized room measures carried by the signed
-- canonical pricing source.  This migration binds them to the current
-- gameId=2999 release/index/source tuple.  Historical gameId=630 rows are not
-- rewritten and remain Compatibility-only.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_cd2999_current_pricing_policy_v20261012_01()
BEGIN
  DECLARE active_matches INT DEFAULT 0;
  DECLARE conflicts INT DEFAULT 0;
  DECLARE published INT DEFAULT 0;

  SELECT COUNT(*) INTO active_matches
    FROM aoo_compiled_index_active a
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id
     AND i.region_code=a.region_code
     AND i.play_version=a.play_version
     AND i.index_generation=a.index_generation
     AND i.release_id=a.release_id
     AND i.lifecycle_state='ACTIVE'
    JOIN aoo_game_release r
      ON r.release_id=a.release_id
     AND r.game_id=a.game_id
     AND r.play_version=a.play_version
     AND r.status='ACTIVE'
   WHERE a.game_id=2999
     AND a.region_code='CN-51-01'
     AND a.play_version='cd299-v1.0.0'
     AND a.release_id=2999001
     AND a.index_generation=3999001
     AND r.source_hash='f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5';

  IF active_matches <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CD2999_PRICING_RELEASE_BINDING_MISSING_OR_DRIFTED';
  END IF;

  SELECT COUNT(*) INTO conflicts
    FROM aoo_room_cost_policy p
   WHERE p.game_id=2999
     AND p.play_version='cd299-v1.0.0'
     AND p.region_code='CN-51-01'
     AND (
       p.status<>'ACTIVE'
       OR p.player_count<>8
       OR p.payer_mode<>'OWNER'
       OR p.currency_code<>'ROOM_CARD'
       OR p.policy_version<>2026101101
       OR p.content_hash<>'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858'
       OR NOT (
         (p.measure_kind='ROUND_COUNT' AND (
           (p.measure_value=10 AND p.round_count=10 AND p.cost_minor=10)
           OR (p.measure_value=20 AND p.round_count=20 AND p.cost_minor=20)
           OR (p.measure_value=30 AND p.round_count=30 AND p.cost_minor=30)))
         OR (p.measure_kind='DURATION_MINUTES' AND (
           (p.measure_value=30 AND p.round_count=30 AND p.cost_minor=10)
           OR (p.measure_value=45 AND p.round_count=45 AND p.cost_minor=15)
           OR (p.measure_value=60 AND p.round_count=60 AND p.cost_minor=20)))
       )
     );

  IF conflicts <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CD2999_PRICING_POLICY_DRIFTED';
  END IF;

  INSERT INTO aoo_room_cost_policy(
      game_id,play_version,region_code,measure_kind,measure_value,round_count,
      player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
  VALUES
    (2999,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',10,10,8,'OWNER','ROOM_CARD',10,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE'),
    (2999,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',20,20,8,'OWNER','ROOM_CARD',20,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE'),
    (2999,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',30,30,8,'OWNER','ROOM_CARD',30,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE'),
    (2999,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',30,30,8,'OWNER','ROOM_CARD',10,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE'),
    (2999,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',45,45,8,'OWNER','ROOM_CARD',15,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE'),
    (2999,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',60,60,8,'OWNER','ROOM_CARD',20,2026101101,'13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858','ACTIVE')
  ON DUPLICATE KEY UPDATE
      game_id=VALUES(game_id);

  SELECT COUNT(*) INTO published
    FROM aoo_room_cost_policy p
   WHERE p.game_id=2999
     AND p.play_version='cd299-v1.0.0'
     AND p.region_code='CN-51-01'
     AND p.status='ACTIVE'
     AND p.player_count=8
     AND p.payer_mode='OWNER'
     AND p.currency_code='ROOM_CARD'
     AND p.policy_version=2026101101
     AND p.content_hash='13193a9929c0433f86b68458c26251a9b94008599e87aa763ef3c8a5e23c1858'
     AND ((p.measure_kind='ROUND_COUNT' AND p.measure_value IN (10,20,30))
       OR (p.measure_kind='DURATION_MINUTES' AND p.measure_value IN (30,45,60)));

  IF published <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CD2999_PRICING_POLICY_PUBLICATION_INCOMPLETE';
  END IF;
END$$

CALL publish_cd2999_current_pricing_policy_v20261012_01()$$
DROP PROCEDURE publish_cd2999_current_pricing_policy_v20261012_01$$

DELIMITER ;
