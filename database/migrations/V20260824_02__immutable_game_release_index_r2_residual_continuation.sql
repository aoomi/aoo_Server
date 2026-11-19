-- r2-only continuation for V20260824_02 after empty residual objects were observed.
-- Never run on r1/production; original migration is intentionally unchanged.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE aoo_v2402_r2_residual_guard()
BEGIN
 DECLARE n BIGINT DEFAULT 0;
 SELECT COUNT(*) INTO n FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('aoo_game_release','aoo_game_release_region','aoo_game_release_component','aoo_game_release_audit','aoo_compiled_room_create_index','aoo_compiled_index_active','aoo_configuration_cache_epoch','aoo_room_rule_lock','aoo_room_template','aoo_room_snapshot','perspective_replay_event','perspective_replay_event_archive');
 IF n<>12 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_REQUIRED_TABLE_DRIFT'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_game_release; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_RELEASE_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_game_release_region; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_RELEASE_REGION_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_game_release_component; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_RELEASE_COMPONENT_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_game_release_audit; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_RELEASE_AUDIT_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_compiled_room_create_index; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_COMPILED_INDEX_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_compiled_index_active; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_ACTIVE_INDEX_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_configuration_cache_epoch; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_CACHE_EPOCH_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_room_rule_lock; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_RULE_LOCK_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_room_template; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_TEMPLATE_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM aoo_room_snapshot; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_SNAPSHOT_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM perspective_replay_event; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_REPLAY_RESIDUAL'; END IF;
 SELECT COUNT(*) INTO n FROM perspective_replay_event_archive; IF n<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_NONEMPTY_REPLAY_ARCHIVE_RESIDUAL'; END IF;
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='aoo_room_template_release_lock')
 OR EXISTS(SELECT 1 FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND trigger_name IN ('trg_room_rule_lock_no_update','trg_room_rule_lock_no_delete'))
 OR EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='aoo_activate_compiled_game_index')
 OR EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('aoo_room_snapshot','perspective_replay_event','perspective_replay_event_archive') AND column_name='release_id')
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='V2402_R2_PARTIAL_OR_ALREADY_APPLIED'; END IF;
END$$
CALL aoo_v2402_r2_residual_guard()$$
DROP PROCEDURE aoo_v2402_r2_residual_guard$$
DELIMITER ;

CREATE TABLE aoo_room_template_release_lock (
    club_id BIGINT UNSIGNED NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_version BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    rule_hash CHAR(64) NOT NULL,
    locked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (club_id,template_code,template_version),
    KEY idx_template_release_lock_release (release_id,club_id,template_code),
    CONSTRAINT fk_template_release_lock_template FOREIGN KEY (club_id,template_code,template_version)
        REFERENCES aoo_room_template(club_id,template_code,template_version) ON DELETE RESTRICT,
    CONSTRAINT fk_template_release_lock_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT fk_template_release_lock_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT fk_template_release_lock_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_template_release_lock_hash CHECK (rule_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Expand phase: legacy rows remain nullable; new write paths must populate the
-- sidecar lock. Contract-to-NOT-NULL is deliberately deferred until the
-- migration validation ledger proves zero missing references.
ALTER TABLE aoo_room_template
    ADD CONSTRAINT fk_room_template_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT;

ALTER TABLE aoo_room_snapshot
    ADD COLUMN release_id BIGINT UNSIGNED NULL AFTER play_version,
    ADD COLUMN rule_content_hash CHAR(64) NULL AFTER component_version,
    ADD COLUMN component_content_hash CHAR(64) NULL AFTER rule_content_hash,
    ADD COLUMN card_codec_version VARCHAR(64) NULL AFTER component_content_hash,
    ADD COLUMN event_interpreter_version VARCHAR(64) NULL AFTER card_codec_version,
    ADD COLUMN protocol_version VARCHAR(64) NULL AFTER event_interpreter_version,
    ADD KEY idx_room_snapshot_release (release_id,room_id),
    ADD CONSTRAINT fk_room_snapshot_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT;

ALTER TABLE perspective_replay_event
    ADD COLUMN release_id BIGINT UNSIGNED NULL AFTER play_version,
    ADD COLUMN card_codec_version VARCHAR(64) NULL AFTER release_id,
    ADD COLUMN event_interpreter_version VARCHAR(64) NULL AFTER card_codec_version,
    ADD COLUMN protocol_version VARCHAR(64) NULL AFTER event_interpreter_version,
    ADD KEY idx_replay_release (release_id,room_id,set_id,event_sequence),
    ADD CONSTRAINT fk_replay_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT;

ALTER TABLE perspective_replay_event_archive
    ADD COLUMN release_id BIGINT UNSIGNED NULL AFTER play_version,
    ADD COLUMN card_codec_version VARCHAR(64) NULL AFTER release_id,
    ADD COLUMN event_interpreter_version VARCHAR(64) NULL AFTER card_codec_version,
    ADD COLUMN protocol_version VARCHAR(64) NULL AFTER event_interpreter_version,
    ADD KEY idx_replay_archive_release (release_id,room_id,set_id,event_sequence),
    ADD CONSTRAINT fk_replay_archive_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT;

DELIMITER $$
CREATE TRIGGER trg_room_rule_lock_no_update
BEFORE UPDATE ON aoo_room_rule_lock
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room rule lock is immutable';
END$$

CREATE TRIGGER trg_room_rule_lock_no_delete
BEFORE DELETE ON aoo_room_rule_lock
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room rule lock is retention controlled';
END$$
DELIMITER ;

-- The catalog row is the serialization lock. The active pointer, cache epoch,
-- audit record and outbox event commit together; any validation or write error
-- rolls back the complete switch. Passing action=ROLLBACK selects an older READY
-- generation without weakening the same validation gate.
DELIMITER $$
CREATE PROCEDURE aoo_activate_compiled_game_index(
    IN p_audit_id BIGINT UNSIGNED,
    IN p_request_id VARCHAR(64),
    IN p_game_id BIGINT UNSIGNED,
    IN p_region_code VARCHAR(32),
    IN p_play_version VARCHAR(64),
    IN p_index_generation BIGINT UNSIGNED,
    IN p_operator_id BIGINT UNSIGNED,
    IN p_action VARCHAR(16),
    IN p_reason VARCHAR(500)
)
BEGIN
    DECLARE v_guard BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_release_id BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_previous_release_id BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_next_epoch BIGINT UNSIGNED DEFAULT 1;
    DECLARE v_missing_components INT DEFAULT 0;
    DECLARE v_missing_dependencies INT DEFAULT 0;
    DECLARE v_conflicts INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_action NOT IN ('ACTIVATE','CANARY','ROLLBACK') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unsupported release activation action';
    END IF;

    START TRANSACTION;

    SELECT game_id INTO v_guard
      FROM aoo_game_catalog
     WHERE game_id=p_game_id
     FOR UPDATE;

    IF v_guard IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='catalog game guard is missing';
    END IF;

    SELECT MAX(release_id) INTO v_release_id
      FROM aoo_compiled_room_create_index
     WHERE game_id=p_game_id
       AND region_code=p_region_code
       AND play_version=p_play_version
       AND index_generation=p_index_generation
       AND lifecycle_state='READY';

    IF v_release_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='compiled index is absent or not READY';
    END IF;

    SELECT COUNT(*) INTO v_missing_components
      FROM aoo_component_type required_type
     WHERE required_type.required_for_active_game=1
       AND NOT EXISTS (
           SELECT 1
             FROM aoo_game_release_component released
            WHERE released.release_id=v_release_id
              AND released.component_type=required_type.component_type
       );

    SELECT COUNT(*) INTO v_missing_dependencies
      FROM aoo_game_release_component released
      JOIN aoo_component_dependency dependency
        ON dependency.component_id=released.component_id
       AND dependency.optional_dependency=0
     WHERE released.release_id=v_release_id
       AND NOT EXISTS (
           SELECT 1
             FROM aoo_game_release_component required_release
            WHERE required_release.release_id=v_release_id
              AND required_release.component_id=dependency.required_component_id
       );

    SELECT COUNT(*) INTO v_conflicts
      FROM aoo_game_release_component left_component
      JOIN aoo_component_conflict conflict
        ON conflict.component_id=left_component.component_id
      JOIN aoo_game_release_component right_component
        ON right_component.release_id=left_component.release_id
       AND right_component.component_id=conflict.conflicting_component_id
     WHERE left_component.release_id=v_release_id;

    IF v_missing_components > 0 OR v_missing_dependencies > 0 OR v_conflicts > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='release component graph validation failed';
    END IF;

    SELECT MAX(release_id),COALESCE(MAX(cache_epoch),0)+1
      INTO v_previous_release_id,v_next_epoch
      FROM aoo_compiled_index_active
     WHERE game_id=p_game_id AND region_code=p_region_code;

    UPDATE aoo_compiled_room_create_index
       SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
     WHERE game_id=p_game_id AND region_code=p_region_code
       AND lifecycle_state='ACTIVE' AND release_id<>v_release_id;

    UPDATE aoo_compiled_room_create_index
       SET lifecycle_state='ACTIVE',activated_at=CURRENT_TIMESTAMP(3),retired_at=NULL
     WHERE game_id=p_game_id AND region_code=p_region_code
       AND play_version=p_play_version AND index_generation=p_index_generation;

    INSERT INTO aoo_compiled_index_active(
        game_id,region_code,play_version,index_generation,release_id,cache_epoch,
        activated_by,activation_reason,activated_at
    ) VALUES (
        p_game_id,p_region_code,p_play_version,p_index_generation,v_release_id,v_next_epoch,
        p_operator_id,p_reason,CURRENT_TIMESTAMP(3)
    ) ON DUPLICATE KEY UPDATE
        play_version=VALUES(play_version),
        index_generation=VALUES(index_generation),
        release_id=VALUES(release_id),
        cache_epoch=VALUES(cache_epoch),
        activated_by=VALUES(activated_by),
        activation_reason=VALUES(activation_reason),
        activated_at=VALUES(activated_at);

    INSERT INTO aoo_configuration_cache_epoch(
        game_id,region_code,cache_epoch,release_id,invalidation_event_id,changed_at
    ) VALUES (
        p_game_id,p_region_code,v_next_epoch,v_release_id,p_request_id,CURRENT_TIMESTAMP(3)
    ) ON DUPLICATE KEY UPDATE
        cache_epoch=VALUES(cache_epoch),
        release_id=VALUES(release_id),
        invalidation_event_id=VALUES(invalidation_event_id),
        changed_at=VALUES(changed_at);

    UPDATE aoo_game_release
       SET status='ACTIVE',activated_at=CURRENT_TIMESTAMP(3)
     WHERE release_id=v_release_id;

    UPDATE aoo_game_release_region
       SET status='ACTIVE'
     WHERE release_id=v_release_id AND region_code=p_region_code;

    INSERT INTO aoo_game_release_audit(
        audit_id,release_id,action,operator_id,request_id,reason,before_status,
        after_status,before_active_release_id,after_active_release_id,occurred_at
    ) VALUES (
        p_audit_id,v_release_id,p_action,p_operator_id,p_request_id,p_reason,
        IF(v_previous_release_id IS NULL,NULL,'ACTIVE'),'ACTIVE',
        v_previous_release_id,v_release_id,CURRENT_TIMESTAMP(3)
    );

    INSERT INTO aoo_outbox(
        event_id,aggregate_type,aggregate_id,event_type,schema_version,payload,created_at
    ) VALUES (
        p_request_id,'GAME_RELEASE',p_game_id,'GameReleaseActivated',1,
        JSON_OBJECT(
            'gameId',p_game_id,'regionCode',p_region_code,'playVersion',p_play_version,
            'indexGeneration',p_index_generation,'releaseId',v_release_id,
            'cacheEpoch',v_next_epoch,'action',p_action
        ),CURRENT_TIMESTAMP(3)
    );

    COMMIT;
END$$
DELIMITER ;
