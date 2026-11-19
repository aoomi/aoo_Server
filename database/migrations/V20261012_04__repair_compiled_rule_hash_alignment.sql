-- Forward-only repair for active release metadata hash drift.
--
-- V20261012_03 can update an existing release snapshot while its compiled
-- index row is already present.  In that case release.rule_hash may describe
-- the newly wrapped snapshot while Hall validates the bytes of the active
-- compiled rule_validator.  The compiled validator is the signed executable
-- rule contract, so this migration derives only the hash column from those
-- exact persisted bytes.  It does not rewrite historical snapshots, compiled
-- validators, workbooks, protocols, or generated bundles.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

DROP PROCEDURE IF EXISTS repair_compiled_rule_hash_alignment_v20261012_04$$

CREATE PROCEDURE repair_compiled_rule_hash_alignment_v20261012_04()
BEGIN
  DECLARE expected_identity_count INT DEFAULT 32;
  DECLARE active_identity_count INT DEFAULT 0;
  DECLARE distinct_identity_count INT DEFAULT 0;

  START TRANSACTION;

  CREATE TEMPORARY TABLE v20261012_04_rule_hash_alignment (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    release_rule_hash CHAR(64) NOT NULL,
    compiled_rule_hash CHAR(64) NOT NULL,
    PRIMARY KEY (game_id,region_code,play_version)
  ) ENGINE=MEMORY;

  INSERT INTO v20261012_04_rule_hash_alignment(
      game_id,region_code,play_version,release_id,release_rule_hash,compiled_rule_hash)
  SELECT a.game_id,a.region_code,a.play_version,a.release_id,r.rule_hash,
         SHA2(CAST(i.rule_validator AS CHAR CHARACTER SET utf8mb4),256)
    FROM aoo_compiled_index_active a
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id AND i.region_code=a.region_code
     AND i.play_version=a.play_version AND i.index_generation=a.index_generation
     AND i.release_id=a.release_id AND i.lifecycle_state='ACTIVE'
    JOIN aoo_game_release r
      ON r.release_id=a.release_id AND r.game_id=a.game_id
     AND r.play_version=a.play_version AND r.status='ACTIVE'
    JOIN aoo_game_release_region rr
      ON rr.release_id=r.release_id AND rr.region_code=a.region_code
     AND rr.status='ACTIVE'
    JOIN aoo_game_catalog g
      ON g.game_id=a.game_id AND g.status='ACTIVE';

  SELECT COUNT(*),COUNT(DISTINCT CONCAT(game_id,'|',region_code,'|',play_version))
    INTO active_identity_count,distinct_identity_count
    FROM v20261012_04_rule_hash_alignment;
  IF active_identity_count<>expected_identity_count
     OR distinct_identity_count<>expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='COMPILED_RULE_HASH_ALIGNMENT_CURRENT_IDENTITY_COUNT_MISMATCH';
  END IF;

  IF EXISTS (
    SELECT 1 FROM v20261012_04_rule_hash_alignment
     WHERE release_rule_hash IS NULL OR release_rule_hash NOT REGEXP '^[0-9a-f]{64}$'
        OR compiled_rule_hash IS NULL OR compiled_rule_hash NOT REGEXP '^[0-9a-f]{64}$'
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='COMPILED_RULE_HASH_ALIGNMENT_INVALID_HASH';
  END IF;

  UPDATE aoo_game_release r
    JOIN v20261012_04_rule_hash_alignment a ON a.release_id=r.release_id
   SET r.rule_hash=a.compiled_rule_hash
   WHERE r.rule_hash<>a.compiled_rule_hash;

  IF EXISTS (
    SELECT 1
      FROM v20261012_04_rule_hash_alignment a
      JOIN aoo_game_release r ON r.release_id=a.release_id
     WHERE r.rule_hash<>a.compiled_rule_hash
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='COMPILED_RULE_HASH_ALIGNMENT_POSTCHECK_FAILED';
  END IF;

  COMMIT;
END$$

CALL repair_compiled_rule_hash_alignment_v20261012_04()$$
DROP PROCEDURE repair_compiled_rule_hash_alignment_v20261012_04$$

DELIMITER ;
