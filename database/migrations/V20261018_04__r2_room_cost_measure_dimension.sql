-- r2-only structural subset of V20260919_06. CD299 publication tail is excluded.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE aoo_r2_room_cost_measure_dimension_v20261018_04()
BEGIN
  DECLARE n INT DEFAULT 0;
  SELECT COUNT(*) INTO n FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_room_cost_policy' AND COLUMN_NAME IN ('measure_kind','measure_value');
  IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_ROOM_COST_MEASURE_PARTIAL_OR_APPLIED'; END IF;
  SELECT COUNT(*) INTO n FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_room_cost_policy';
  IF n<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_ROOM_COST_POLICY_MISSING'; END IF;
  ALTER TABLE aoo_room_cost_policy
    ADD COLUMN measure_kind VARCHAR(24) NOT NULL DEFAULT 'ROUND_COUNT' AFTER region_code,
    ADD COLUMN measure_value INT UNSIGNED NOT NULL DEFAULT 1 AFTER measure_kind;
  UPDATE aoo_room_cost_policy SET measure_kind='ROUND_COUNT',measure_value=round_count;
  ALTER TABLE aoo_room_cost_policy
    ADD CONSTRAINT chk_room_cost_measure_kind CHECK (measure_kind IN ('ROUND_COUNT','DURATION_MINUTES')),
    ADD KEY idx_room_cost_measure_lookup(region_code,game_id,play_version,measure_kind,measure_value,player_count,status);
END$$
CALL aoo_r2_room_cost_measure_dimension_v20261018_04()$$
DROP PROCEDURE aoo_r2_room_cost_measure_dimension_v20261018_04$$
DELIMITER ;
