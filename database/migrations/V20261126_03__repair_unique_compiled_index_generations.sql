-- Correct the generation collision introduced by V20261126_02 before rooms use
-- those projections. This repair is transactional and preserves release data.
DELIMITER $$
CREATE PROCEDURE repair_unique_compiled_index_generations_v20261126_03()
BEGIN
  DECLARE expected_count INT DEFAULT 0;
  DECLARE repaired_count INT DEFAULT 0;

  START TRANSACTION;
  DROP TABLE IF EXISTS unique_index_generation_candidate_v20261126_03;
  CREATE TABLE unique_index_generation_candidate_v20261126_03 AS
  SELECT a.game_id,a.region_code,a.play_version,a.release_id,a.index_generation AS old_generation,
         limits.max_generation + ROW_NUMBER() OVER (ORDER BY a.game_id,a.region_code,a.play_version) AS new_generation
    FROM aoo_compiled_index_active a
    CROSS JOIN (SELECT MAX(index_generation) AS max_generation FROM aoo_compiled_room_create_index) limits;

  SELECT COUNT(*) INTO expected_count FROM unique_index_generation_candidate_v20261126_03;
  IF expected_count<>32
     OR EXISTS (SELECT 1 FROM unique_index_generation_candidate_v20261126_03
                 GROUP BY new_generation HAVING COUNT(*)<>1) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unique compiled index generation repair precondition failed';
  END IF;

  SET FOREIGN_KEY_CHECKS=0;
  UPDATE aoo_compiled_room_create_index i
  JOIN unique_index_generation_candidate_v20261126_03 c
    ON c.game_id=i.game_id AND c.region_code=i.region_code AND c.play_version=i.play_version
   AND c.old_generation=i.index_generation AND c.release_id=i.release_id
     SET i.index_generation=c.new_generation;

  UPDATE aoo_compiled_index_active a
  JOIN unique_index_generation_candidate_v20261126_03 c
    ON c.game_id=a.game_id AND c.region_code=a.region_code AND c.play_version=a.play_version
   AND c.old_generation=a.index_generation AND c.release_id=a.release_id
     SET a.index_generation=c.new_generation,a.cache_epoch=a.cache_epoch+1,
         a.activation_reason='Repair globally unique compiled index generation',
         a.activated_at=CURRENT_TIMESTAMP(3);
  SET FOREIGN_KEY_CHECKS=1;

  SELECT COUNT(*) INTO repaired_count
    FROM unique_index_generation_candidate_v20261126_03 c
    JOIN aoo_compiled_index_active a
      ON a.game_id=c.game_id AND a.region_code=c.region_code AND a.play_version=c.play_version
     AND a.index_generation=c.new_generation AND a.release_id=c.release_id
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
     AND i.index_generation=a.index_generation AND i.release_id=a.release_id;
  IF repaired_count<>expected_count
     OR EXISTS (SELECT 1 FROM aoo_compiled_index_active GROUP BY index_generation HAVING COUNT(*)<>1) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unique compiled index generation repair verification failed';
  END IF;

  DROP TABLE unique_index_generation_candidate_v20261126_03;
  COMMIT;
END$$
DELIMITER ;
CALL repair_unique_compiled_index_generations_v20261126_03();
DROP PROCEDURE repair_unique_compiled_index_generations_v20261126_03;
