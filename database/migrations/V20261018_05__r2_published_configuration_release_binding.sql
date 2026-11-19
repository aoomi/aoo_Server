-- r2-only empty-table structural subset of V20260824_77; no data backfill.
-- MySQL DDL implicitly commits; each DDL is guarded and post-checked, never claimed atomic.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE aoo_r2_published_configuration_preflight()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.routines WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='aoo_r2_published_configuration_release_binding_v20261018_05') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_PUBLISHED_CONFIGURATION_PROCEDURE_ALREADY_EXISTS'; END IF;
END$$
CALL aoo_r2_published_configuration_preflight()$$
DROP PROCEDURE aoo_r2_published_configuration_preflight$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE aoo_r2_published_configuration_release_binding_v20261018_05()
BEGIN
  DECLARE n INT DEFAULT 0;
  SELECT COUNT(*) INTO n FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release';
  IF n<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_GAME_RELEASE_TABLE_MISSING'; END IF;
  SELECT COUNT(*) INTO n FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration';
  IF n<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_PUBLISHED_CONFIGURATION_TABLE_MISSING'; END IF;
  SELECT COUNT(*) INTO n FROM aoo_published_game_configuration;
  IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_PUBLISHED_CONFIGURATION_NONEMPTY'; END IF;
  SELECT COUNT(*) INTO n FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=1 AND COLUMN_NAME='release_id';
  IF n>0 AND (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=2 AND COLUMN_NAME='game_id')=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_GAME_RELEASE_IDENTITY_KEY_PARTIAL'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND COLUMN_NAME='release_id')>0
     OR EXISTS(SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release')
     OR EXISTS(SELECT 1 FROM information_schema.referential_constraints WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity')
     OR EXISTS(SELECT 1 FROM information_schema.triggers WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_published_configuration_release_guard')
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_PUBLISHED_CONFIGURATION_PARTIAL_STOP'; END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=1 AND COLUMN_NAME='release_id') THEN
    ALTER TABLE aoo_game_release ADD UNIQUE KEY uk_game_release_identity(release_id,game_id,play_version);
  END IF;
  SELECT COUNT(*) INTO n FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX IN (1,2,3);
  IF n<>3 OR (SELECT MAX(SEQ_IN_INDEX) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity')<>3 OR (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=2 AND COLUMN_NAME='game_id')<>1 OR (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_game_release' AND INDEX_NAME='uk_game_release_identity' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=3 AND COLUMN_NAME='play_version')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_GAME_RELEASE_IDENTITY_KEY_POSTCHECK'; END IF;
  ALTER TABLE aoo_published_game_configuration ADD COLUMN release_id BIGINT UNSIGNED NULL AFTER play_version;
  SELECT COUNT(*) INTO n FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND COLUMN_NAME='release_id';
  IF n<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_RELEASE_COLUMN_POSTCHECK'; END IF;
  ALTER TABLE aoo_published_game_configuration ADD KEY idx_published_configuration_release(release_id,game_id,play_version);
  SELECT COUNT(*) INTO n FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release' AND SEQ_IN_INDEX IN (1,2,3);
  IF n<>3 OR (SELECT MAX(SEQ_IN_INDEX) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release')<>3 OR (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release' AND SEQ_IN_INDEX=1 AND COLUMN_NAME='release_id')<>1 OR (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release' AND SEQ_IN_INDEX=2 AND COLUMN_NAME='game_id')<>1 OR (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aoo_published_game_configuration' AND INDEX_NAME='idx_published_configuration_release' AND SEQ_IN_INDEX=3 AND COLUMN_NAME='play_version')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_RELEASE_INDEX_POSTCHECK'; END IF;
  ALTER TABLE aoo_published_game_configuration ADD CONSTRAINT fk_published_configuration_release_identity FOREIGN KEY (release_id,game_id,play_version) REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT;
  SELECT COUNT(*) INTO n FROM information_schema.referential_constraints WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity';
  IF n<>1 OR (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity' AND TABLE_NAME='aoo_published_game_configuration' AND ORDINAL_POSITION=1 AND COLUMN_NAME='release_id' AND REFERENCED_TABLE_NAME='aoo_game_release' AND REFERENCED_COLUMN_NAME='release_id')<>1 OR (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity' AND TABLE_NAME='aoo_published_game_configuration' AND ORDINAL_POSITION=2 AND COLUMN_NAME='game_id' AND REFERENCED_TABLE_NAME='aoo_game_release' AND REFERENCED_COLUMN_NAME='game_id')<>1 OR (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity' AND TABLE_NAME='aoo_published_game_configuration' AND ORDINAL_POSITION=3 AND COLUMN_NAME='play_version' AND REFERENCED_TABLE_NAME='aoo_game_release' AND REFERENCED_COLUMN_NAME='play_version')<>1 OR (SELECT DELETE_RULE FROM information_schema.referential_constraints WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_published_configuration_release_identity')<>'RESTRICT' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_RELEASE_FK_POSTCHECK'; END IF;
END$$
CALL aoo_r2_published_configuration_release_binding_v20261018_05()$$
DROP PROCEDURE aoo_r2_published_configuration_release_binding_v20261018_05$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_published_configuration_release_guard
BEFORE INSERT ON aoo_published_game_configuration
FOR EACH ROW
BEGIN
  IF NEW.release_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published configuration release_id is required'; END IF;
  IF NOT EXISTS (SELECT 1 FROM aoo_game_release r WHERE r.release_id=NEW.release_id AND r.game_id=NEW.game_id AND r.play_version=NEW.play_version AND r.status IN ('VALIDATED','ACTIVE')) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published configuration release identity is invalid or inactive'; END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE aoo_r2_published_configuration_trigger_postcheck()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.triggers WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_published_configuration_release_guard' AND EVENT_OBJECT_TABLE='aoo_published_game_configuration' AND ACTION_TIMING='BEFORE' AND EVENT_MANIPULATION='INSERT') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='R2_PUBLISHED_CONFIGURATION_TRIGGER_POSTCHECK';
  END IF;
END$$
CALL aoo_r2_published_configuration_trigger_postcheck()$$
DROP PROCEDURE aoo_r2_published_configuration_trigger_postcheck$$
DELIMITER ;
