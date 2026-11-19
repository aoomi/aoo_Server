-- r2-only compatibility for the authoritative 1001 family code mahjong:xue-zhan.
-- Original migrations and r1 are untouched. Execute only in the isolated r2 schema.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE aoo_r2_allow_formal_family_code_v20261018_02()
BEGIN
  DECLARE v_clause TEXT DEFAULT NULL;
  SELECT CHECK_CLAUSE INTO v_clause
    FROM information_schema.check_constraints
   WHERE CONSTRAINT_SCHEMA=DATABASE()
     AND CONSTRAINT_NAME='chk_game_family_code';
  IF v_clause IS NULL OR v_clause NOT LIKE '%A-Z%0-9_%' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_FAMILY_CODE_CHECK_DRIFT';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.check_constraints
             WHERE CONSTRAINT_SCHEMA=DATABASE()
               AND CONSTRAINT_NAME='chk_game_family_code'
               AND CHECK_CLAUSE LIKE '%A-Za-z%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_FAMILY_CODE_COMPATIBILITY_ALREADY_APPLIED';
  END IF;
  ALTER TABLE aoo_game_family DROP CHECK chk_game_family_code;
  ALTER TABLE aoo_game_family ADD CONSTRAINT chk_game_family_code
    CHECK (REGEXP_LIKE(family_code,'^[A-Za-z][A-Za-z0-9_.:-]{0,63}$'));
END$$
CALL aoo_r2_allow_formal_family_code_v20261018_02()$$
DROP PROCEDURE aoo_r2_allow_formal_family_code_v20261018_02$$
DELIMITER ;
