-- Forward-only compatibility cutover before the current gameId=2999
-- projection. V20261012_00 rejects a duplicate current code, so the old
-- gameId=630 catalog identity is renamed and retired first. History, releases,
-- rooms and snapshots remain queryable; only current admission is closed.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE retire_cd299_compatibility_alias_v20261011_99()
BEGIN
  IF EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=630 AND game_code='chengdu2999') THEN
    UPDATE aoo_game_service_route SET status='OFFLINE' WHERE game_id=630 AND status='ACTIVE';
    UPDATE aoo_compiled_room_create_index
       SET lifecycle_state='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
     WHERE game_id=630 AND lifecycle_state='ACTIVE';
    UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
       SET rr.status='RETIRED' WHERE r.game_id=630 AND rr.status='ACTIVE';
    UPDATE aoo_game_release
       SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
     WHERE game_id=630 AND status='ACTIVE';
    UPDATE aoo_play_version
       SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
     WHERE game_id=630 AND status<>'RETIRED';
    UPDATE aoo_game_region SET availability='RETIRED'
     WHERE game_id=630 AND availability<>'RETIRED';
    UPDATE aoo_game_catalog
       SET game_code='cd2999_compat_630',provider_key='compatibility.cd2999.630',status='RETIRED'
     WHERE game_id=630 AND game_code='chengdu2999';
  END IF;
  IF EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=630 AND game_code='chengdu2999') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CD2999_COMPATIBILITY_ALIAS_CUTOVER_FAILED';
  END IF;
END$$

CALL retire_cd299_compatibility_alias_v20261011_99()$$
DROP PROCEDURE retire_cd299_compatibility_alias_v20261011_99$$

DELIMITER ;
