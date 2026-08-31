-- Immutable publication bundle, precompiled room-create index, atomic active
-- pointer, cache epoch and durable room/history version locks.

CREATE TABLE aoo_game_release (
    release_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_version BIGINT UNSIGNED NOT NULL,
    release_scope VARCHAR(16) NOT NULL,
    catalog_snapshot JSON NOT NULL,
    rule_snapshot JSON NOT NULL,
    ui_snapshot JSON NOT NULL,
    component_snapshot JSON NOT NULL,
    catalog_hash CHAR(64) NOT NULL,
    rule_hash CHAR(64) NOT NULL,
    ui_hash CHAR(64) NOT NULL,
    component_hash CHAR(64) NOT NULL,
    bundle_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'STAGED',
    rollout_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    created_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    validated_at DATETIME(3) NULL,
    activated_at DATETIME(3) NULL,
    retired_at DATETIME(3) NULL,
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_game_release_version (game_id,play_version,release_version),
    UNIQUE KEY uk_game_release_bundle (game_id,bundle_hash),
    KEY idx_game_release_status (status,game_id,created_at),
    CONSTRAINT fk_game_release_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT chk_game_release_id CHECK (release_id > 0),
    CONSTRAINT chk_game_release_scope CHECK (release_scope IN ('GLOBAL','REGIONAL','CANARY')),
    CONSTRAINT chk_game_release_status CHECK (status IN ('STAGED','VALIDATED','ACTIVE','RETIRED','ROLLED_BACK','FAILED')),
    CONSTRAINT chk_game_release_rollout CHECK (rollout_percent >= 0.00 AND rollout_percent <= 100.00),
    CONSTRAINT chk_game_release_hashes CHECK (
        catalog_hash REGEXP '^[0-9a-f]{64}$'
        AND rule_hash REGEXP '^[0-9a-f]{64}$'
        AND ui_hash REGEXP '^[0-9a-f]{64}$'
        AND component_hash REGEXP '^[0-9a-f]{64}$'
        AND bundle_hash REGEXP '^[0-9a-f]{64}$'
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_release_region (
    release_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    rollout_percent DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    status VARCHAR(16) NOT NULL DEFAULT 'STAGED',
    PRIMARY KEY (release_id,region_code),
    KEY idx_game_release_region_lookup (region_code,status,release_id),
    CONSTRAINT fk_game_release_region_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_release_region_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_release_region_rollout CHECK (rollout_percent >= 0.00 AND rollout_percent <= 100.00),
    CONSTRAINT chk_game_release_region_status CHECK (status IN ('STAGED','VALIDATED','ACTIVE','RETIRED','ROLLED_BACK'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_release_component (
    release_id BIGINT UNSIGNED NOT NULL,
    component_type VARCHAR(32) NOT NULL,
    ordinal SMALLINT UNSIGNED NOT NULL,
    component_id BIGINT UNSIGNED NOT NULL,
    component_key VARCHAR(192) NOT NULL,
    component_version VARCHAR(64) NOT NULL,
    spi_type VARCHAR(192) NOT NULL,
    implementation_locator VARCHAR(512) NOT NULL,
    artifact_digest CHAR(64) NOT NULL,
    parameter_snapshot JSON NOT NULL,
    content_hash CHAR(64) NOT NULL,
    PRIMARY KEY (release_id,component_type,ordinal),
    UNIQUE KEY uk_release_component_instance (release_id,component_id),
    KEY idx_release_component_reverse (component_id,release_id),
    CONSTRAINT fk_release_component_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT fk_release_component_type FOREIGN KEY (component_type)
        REFERENCES aoo_component_type(component_type) ON DELETE RESTRICT,
    CONSTRAINT fk_release_component_component FOREIGN KEY (component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT,
    CONSTRAINT chk_release_component_digest CHECK (artifact_digest REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_release_component_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_compiled_room_create_index (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    component_chain JSON NOT NULL,
    rule_validator JSON NOT NULL,
    ui_schema JSON NOT NULL,
    lookup_hash CHAR(64) NOT NULL,
    bundle_hash CHAR(64) NOT NULL,
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'STAGED',
    compiled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    validated_at DATETIME(3) NULL,
    activated_at DATETIME(3) NULL,
    retired_at DATETIME(3) NULL,
    PRIMARY KEY (game_id,region_code,play_version,index_generation),
    UNIQUE KEY uk_compiled_index_release_region (release_id,region_code),
    KEY idx_compiled_index_activation (lifecycle_state,region_code,game_id,validated_at),
    CONSTRAINT fk_compiled_index_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT fk_compiled_index_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT fk_compiled_index_release_region FOREIGN KEY (release_id,region_code)
        REFERENCES aoo_game_release_region(release_id,region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_compiled_index_generation CHECK (index_generation > 0),
    CONSTRAINT chk_compiled_index_lookup_hash CHECK (lookup_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_compiled_index_bundle_hash CHECK (bundle_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_compiled_index_state CHECK (lifecycle_state IN ('STAGED','READY','ACTIVE','RETIRED','FAILED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_compiled_index_active (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    cache_epoch BIGINT UNSIGNED NOT NULL,
    activated_by BIGINT UNSIGNED NOT NULL,
    activation_reason VARCHAR(500) NOT NULL,
    activated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,region_code),
    UNIQUE KEY uk_compiled_active_release_region (release_id,region_code),
    KEY idx_compiled_active_direct (game_id,region_code,play_version,index_generation),
    CONSTRAINT fk_compiled_active_index FOREIGN KEY (game_id,region_code,play_version,index_generation)
        REFERENCES aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation) ON DELETE RESTRICT,
    CONSTRAINT fk_compiled_active_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_compiled_active_epoch CHECK (cache_epoch > 0)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_configuration_cache_epoch (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    cache_epoch BIGINT UNSIGNED NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    invalidation_event_id VARCHAR(64) NOT NULL,
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,region_code),
    UNIQUE KEY uk_cache_epoch_event (invalidation_event_id),
    KEY idx_cache_epoch_release (release_id,game_id,region_code),
    CONSTRAINT fk_cache_epoch_active FOREIGN KEY (game_id,region_code)
        REFERENCES aoo_compiled_index_active(game_id,region_code) ON DELETE RESTRICT,
    CONSTRAINT fk_cache_epoch_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_cache_epoch_value CHECK (cache_epoch > 0)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_release_audit (
    audit_id BIGINT UNSIGNED NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator_id BIGINT UNSIGNED NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    before_status VARCHAR(16) NULL,
    after_status VARCHAR(16) NOT NULL,
    before_active_release_id BIGINT UNSIGNED NULL,
    after_active_release_id BIGINT UNSIGNED NULL,
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (audit_id),
    UNIQUE KEY uk_game_release_audit_request (request_id),
    KEY idx_game_release_audit_release (release_id,occurred_at,audit_id),
    CONSTRAINT fk_game_release_audit_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_game_release_audit_action CHECK (action IN ('VALIDATE','ACTIVATE','CANARY','ROLLBACK','RETIRE','FAIL'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_room_rule_lock (
    room_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    component_chain JSON NOT NULL,
    immutable_rules JSON NOT NULL,
    bundle_hash CHAR(64) NOT NULL,
    rule_hash CHAR(64) NOT NULL,
    component_hash CHAR(64) NOT NULL,
    card_codec_version VARCHAR(64) NOT NULL,
    event_interpreter_version VARCHAR(64) NOT NULL,
    protocol_version VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id),
    KEY idx_room_rule_lock_release (release_id,room_id),
    KEY idx_room_rule_lock_history (game_id,play_version,created_at,room_id),
    CONSTRAINT fk_room_rule_lock_index FOREIGN KEY (game_id,region_code,play_version,index_generation)
        REFERENCES aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation) ON DELETE RESTRICT,
    CONSTRAINT fk_room_rule_lock_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rule_lock_hashes CHECK (
        bundle_hash REGEXP '^[0-9a-f]{64}$'
        AND rule_hash REGEXP '^[0-9a-f]{64}$'
        AND component_hash REGEXP '^[0-9a-f]{64}$'
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
