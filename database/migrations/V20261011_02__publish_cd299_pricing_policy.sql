-- Forward-only publication of the user-authorized CD299 pricing source.
--
-- The six measure rows are configuration, not a runtime formula.  The signed
-- source is Server/server/Bootstrap/src/main/resources/
-- canonical-pricing-policy.json.  This migration binds that source to the
-- already-published release and compiled index; it never derives a release,
-- generation, price, or hash from an aggregate, default, or old row.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_cd299_pricing_policy_v20261011_02()
BEGIN
  DECLARE active_matches INT DEFAULT 0;

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
   WHERE a.game_id=630
     AND a.region_code='CN-51-01'
     AND a.play_version='cd299-v1.0.0'
     AND a.release_id=1214679
     AND a.index_generation=90173
     AND r.source_hash='2da550e6fd78d5f6c3656bf62a288b294503582f4056e60d0fc835fd177c44df';

  IF active_matches <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CD299_PRICING_RELEASE_BINDING_MISSING_OR_DRIFTED';
  END IF;

  INSERT INTO aoo_room_cost_policy(
      game_id,play_version,region_code,measure_kind,measure_value,round_count,
      player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status)
  VALUES
    (630,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',10,10,8,'OWNER','ROOM_CARD',10,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE'),
    (630,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',20,20,8,'OWNER','ROOM_CARD',20,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE'),
    (630,'cd299-v1.0.0','CN-51-01','ROUND_COUNT',30,30,8,'OWNER','ROOM_CARD',30,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE'),
    (630,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',30,30,8,'OWNER','ROOM_CARD',10,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE'),
    (630,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',45,45,8,'OWNER','ROOM_CARD',15,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE'),
    (630,'cd299-v1.0.0','CN-51-01','DURATION_MINUTES',60,60,8,'OWNER','ROOM_CARD',20,2026101101,'845a687bf9804c48fbd6961d0232f63626c47dbd16141d92492d5d45cd083eee','ACTIVE')
  ON DUPLICATE KEY UPDATE
      measure_kind=VALUES(measure_kind),
      measure_value=VALUES(measure_value),
      round_count=VALUES(round_count),
      player_count=VALUES(player_count),
      payer_mode=VALUES(payer_mode),
      currency_code=VALUES(currency_code),
      cost_minor=VALUES(cost_minor),
      policy_version=VALUES(policy_version),
      content_hash=VALUES(content_hash),
      status='ACTIVE';
END$$

CALL publish_cd299_pricing_policy_v20261011_02()$$
DROP PROCEDURE publish_cd299_pricing_policy_v20261011_02$$

DELIMITER ;
