-- Database-native integrity views and immutable-history guards. Production
-- readiness requires every *_violation or *_gap view to return zero rows.

-- Complete ownership/retention coverage for every base table present at this
-- migration version. Explicit policies seeded earlier win; the deterministic
-- fallback prevents an undocumented table from entering production.
INSERT IGNORE INTO aoo_table_governance(
    table_name,owner_code,data_classification,authoritative_source,
    hot_retention_days,archive_retention_days,purge_after_days,
    legal_hold_supported,anonymization_strategy,backup_purge_sla_days
)
SELECT
    tables.table_name,
    CASE
        WHEN tables.table_name REGEXP 'ledger|currency|balance' THEN 'PLAYER_ASSET'
        WHEN tables.table_name REGEXP 'club|template' THEN 'CLUB'
        WHEN tables.table_name REGEXP 'admin|privacy|legal_hold|session' THEN 'SECURITY_PRIVACY'
        WHEN tables.table_name REGEXP 'room|replay|settlement|event|lease|connection' THEN 'GAME_RUNTIME'
        WHEN tables.table_name REGEXP 'game|play|component|region|create_ui|rule|profile' THEN 'GAME_CATALOG'
        ELSE 'PLATFORM_DATA'
    END,
    CASE
        WHEN tables.table_name REGEXP 'ledger|currency|balance|admin|privacy|legal_hold|session' THEN 'RESTRICTED'
        WHEN tables.table_name REGEXP 'club|template|room|replay|settlement|event' THEN 'CONFIDENTIAL'
        ELSE 'INTERNAL'
    END,
    'AOO_DB',
    CASE WHEN tables.table_name REGEXP 'room|replay|event|outbox|audit' THEN 30 ELSE 365 END,
    CASE WHEN tables.table_name REGEXP 'room|replay|event|outbox|audit' THEN 365 ELSE 730 END,
    CASE WHEN tables.table_name REGEXP 'catalog|profile|component|region|currency_balance' THEN NULL ELSE 2555 END,
    CASE WHEN tables.table_name REGEXP 'ledger|balance|club|template|room|replay|settlement|event|admin|privacy|session' THEN 1 ELSE 0 END,
    CASE WHEN tables.table_name REGEXP 'ledger|balance|club|template|room|replay|settlement|event|session' THEN 'SUBJECT_ID_TOKENIZE' ELSE NULL END,
    CASE WHEN tables.table_name REGEXP 'ledger|balance|club|template|room|replay|settlement|event|admin|privacy|session' THEN 35 ELSE NULL END
FROM information_schema.tables tables
WHERE tables.table_schema=DATABASE()
  AND tables.table_type='BASE TABLE';

CREATE VIEW v_aoo_active_game_coverage AS
SELECT
    game.game_id,
    game.game_code,
    game.category_code,
    game.family_code,
    COUNT(DISTINCT region.region_code) AS available_region_count,
    COUNT(DISTINCT play.play_version) AS active_play_version_count,
    COUNT(DISTINCT active.region_code) AS active_compiled_region_count
FROM aoo_game_catalog game
LEFT JOIN aoo_game_region region
  ON region.game_id=game.game_id AND region.availability='AVAILABLE'
LEFT JOIN aoo_play_version play
  ON play.game_id=game.game_id AND play.status='ACTIVE'
LEFT JOIN aoo_compiled_index_active active
  ON active.game_id=game.game_id
WHERE game.status='ACTIVE'
GROUP BY game.game_id,game.game_code,game.category_code,game.family_code;

CREATE VIEW v_aoo_active_game_coverage_violation AS
SELECT *
  FROM v_aoo_active_game_coverage
 WHERE available_region_count=0
    OR active_play_version_count=0
    OR active_compiled_region_count=0;

CREATE VIEW v_aoo_component_completeness AS
SELECT
    play.game_id,
    play.play_version,
    required_type.component_type,
    COUNT(binding.component_id) AS binding_count
FROM aoo_play_version play
JOIN aoo_component_type required_type
  ON required_type.required_for_active_game=1
LEFT JOIN aoo_play_component_binding binding
  ON binding.game_id=play.game_id
 AND binding.play_version=play.play_version
 AND binding.component_type=required_type.component_type
WHERE play.status IN ('VALIDATED','ACTIVE')
GROUP BY play.game_id,play.play_version,required_type.component_type;

CREATE VIEW v_aoo_component_completeness_violation AS
SELECT *
  FROM v_aoo_component_completeness
 WHERE binding_count=0;

CREATE VIEW v_aoo_component_graph_violation AS
SELECT
    'MISSING_REQUIRED_DEPENDENCY' AS violation_type,
    binding.game_id,
    binding.play_version,
    dependency.component_id,
    dependency.required_component_id AS related_component_id
FROM aoo_play_component_binding binding
JOIN aoo_component_dependency dependency
  ON dependency.component_id=binding.component_id
 AND dependency.optional_dependency=0
WHERE NOT EXISTS (
    SELECT 1
      FROM aoo_play_component_binding required_binding
     WHERE required_binding.game_id=binding.game_id
       AND required_binding.play_version=binding.play_version
       AND required_binding.component_id=dependency.required_component_id
)
UNION ALL
SELECT
    'CONFLICTING_COMPONENTS' AS violation_type,
    left_binding.game_id,
    left_binding.play_version,
    conflict.component_id,
    conflict.conflicting_component_id AS related_component_id
FROM aoo_play_component_binding left_binding
JOIN aoo_component_conflict conflict
  ON conflict.component_id=left_binding.component_id
JOIN aoo_play_component_binding right_binding
  ON right_binding.game_id=left_binding.game_id
 AND right_binding.play_version=left_binding.play_version
 AND right_binding.component_id=conflict.conflicting_component_id;

CREATE VIEW v_aoo_ui_rule_mapping_violation AS
SELECT
    option_definition.game_id,
    option_definition.play_version,
    option_definition.option_key,
    'OPTION_WITHOUT_RULE_BINDING' AS violation_type
FROM aoo_create_ui_option option_definition
LEFT JOIN aoo_ui_rule_binding binding
  ON binding.game_id=option_definition.game_id
 AND binding.play_version=option_definition.play_version
 AND binding.option_key=option_definition.option_key
WHERE option_definition.status='ACTIVE'
  AND binding.option_key IS NULL
UNION ALL
SELECT
    rule_definition.game_id,
    rule_definition.play_version,
    rule_definition.rule_key AS option_key,
    'REQUIRED_RULE_WITHOUT_UI_BINDING' AS violation_type
FROM aoo_room_rule_definition rule_definition
LEFT JOIN aoo_ui_rule_binding binding
  ON binding.game_id=rule_definition.game_id
 AND binding.play_version=rule_definition.play_version
 AND binding.rule_key=rule_definition.rule_key
WHERE rule_definition.status='ACTIVE'
  AND rule_definition.required_value=1
  AND binding.rule_key IS NULL;

CREATE VIEW v_aoo_release_preflight_violation AS
SELECT
    release_bundle.release_id,
    release_bundle.game_id,
    release_bundle.play_version,
    'MISSING_REQUIRED_COMPONENT' AS violation_type,
    required_type.component_type AS violation_key
FROM aoo_game_release release_bundle
JOIN aoo_component_type required_type
  ON required_type.required_for_active_game=1
LEFT JOIN aoo_game_release_component released
  ON released.release_id=release_bundle.release_id
 AND released.component_type=required_type.component_type
WHERE release_bundle.status IN ('VALIDATED','ACTIVE')
  AND released.component_id IS NULL
UNION ALL
SELECT
    release_bundle.release_id,
    release_bundle.game_id,
    release_bundle.play_version,
    'MISSING_COMPILED_REGION' AS violation_type,
    release_region.region_code AS violation_key
FROM aoo_game_release release_bundle
JOIN aoo_game_release_region release_region
  ON release_region.release_id=release_bundle.release_id
LEFT JOIN aoo_compiled_room_create_index compiled
  ON compiled.release_id=release_bundle.release_id
 AND compiled.region_code=release_region.region_code
WHERE release_bundle.status IN ('VALIDATED','ACTIVE')
  AND compiled.release_id IS NULL
UNION ALL
SELECT
    release_bundle.release_id,
    release_bundle.game_id,
    release_bundle.play_version,
    'BUNDLE_HASH_MISMATCH' AS violation_type,
    compiled.region_code AS violation_key
FROM aoo_game_release release_bundle
JOIN aoo_compiled_room_create_index compiled
  ON compiled.release_id=release_bundle.release_id
WHERE compiled.bundle_hash<>release_bundle.bundle_hash;

CREATE VIEW v_aoo_historical_reference_gap AS
SELECT 'aoo_room_snapshot' AS table_name,CAST(room_id AS CHAR) AS object_key,'release_id' AS missing_column
  FROM aoo_room_snapshot WHERE release_id IS NULL
UNION ALL
SELECT 'aoo_room_snapshot',CAST(room_id AS CHAR),'rule_content_hash'
  FROM aoo_room_snapshot WHERE rule_content_hash IS NULL
UNION ALL
SELECT 'perspective_replay_event',CONCAT(room_id,':',set_id,':',event_sequence,':',visibility,':',owner_player_id),'release_id'
  FROM perspective_replay_event WHERE release_id IS NULL
UNION ALL
SELECT 'perspective_replay_event',CONCAT(room_id,':',set_id,':',event_sequence,':',visibility,':',owner_player_id),'codec_versions'
  FROM perspective_replay_event
 WHERE card_codec_version IS NULL OR event_interpreter_version IS NULL OR protocol_version IS NULL
UNION ALL
SELECT 'perspective_replay_event_archive',CONCAT(room_id,':',set_id,':',event_sequence,':',visibility,':',owner_player_id),'release_id'
  FROM perspective_replay_event_archive WHERE release_id IS NULL;

CREATE VIEW v_aoo_data_governance_gap AS
SELECT tables.table_name
  FROM information_schema.tables tables
LEFT JOIN aoo_table_governance governance
  ON governance.table_name=tables.table_name
 WHERE tables.table_schema=DATABASE()
   AND tables.table_type='BASE TABLE'
   AND governance.table_name IS NULL;

DELIMITER $$
CREATE TRIGGER trg_game_catalog_row_version
BEFORE UPDATE ON aoo_game_catalog
FOR EACH ROW
BEGIN
    SET NEW.row_version=OLD.row_version+1;
END$$

CREATE TRIGGER trg_region_row_version
BEFORE UPDATE ON aoo_region
FOR EACH ROW
BEGIN
    SET NEW.row_version=OLD.row_version+1;
END$$

CREATE TRIGGER trg_play_version_row_version
BEFORE UPDATE ON aoo_play_version
FOR EACH ROW
BEGIN
    SET NEW.row_version=OLD.row_version+1;
END$$

CREATE TRIGGER trg_game_release_immutable_content
BEFORE UPDATE ON aoo_game_release
FOR EACH ROW
BEGIN
    IF OLD.status<>'STAGED' AND (
        NOT (NEW.game_id <=> OLD.game_id)
        OR NOT (NEW.play_version <=> OLD.play_version)
        OR NOT (NEW.release_version <=> OLD.release_version)
        OR NOT (NEW.catalog_snapshot <=> OLD.catalog_snapshot)
        OR NOT (NEW.rule_snapshot <=> OLD.rule_snapshot)
        OR NOT (NEW.ui_snapshot <=> OLD.ui_snapshot)
        OR NOT (NEW.component_snapshot <=> OLD.component_snapshot)
        OR NOT (NEW.bundle_hash <=> OLD.bundle_hash)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='validated game release content is immutable';
    END IF;
END$$

CREATE TRIGGER trg_game_release_no_delete
BEFORE DELETE ON aoo_game_release
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='game release history cannot be deleted';
END$$

CREATE TRIGGER trg_compiled_index_immutable_content
BEFORE UPDATE ON aoo_compiled_room_create_index
FOR EACH ROW
BEGIN
    IF OLD.lifecycle_state<>'STAGED' AND (
        NOT (NEW.release_id <=> OLD.release_id)
        OR NOT (NEW.component_chain <=> OLD.component_chain)
        OR NOT (NEW.rule_validator <=> OLD.rule_validator)
        OR NOT (NEW.ui_schema <=> OLD.ui_schema)
        OR NOT (NEW.lookup_hash <=> OLD.lookup_hash)
        OR NOT (NEW.bundle_hash <=> OLD.bundle_hash)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='validated compiled index content is immutable';
    END IF;
END$$

CREATE TRIGGER trg_template_release_lock_no_update
BEFORE UPDATE ON aoo_room_template_release_lock
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='template release lock is immutable; create a new template version';
END$$

CREATE TRIGGER trg_template_release_lock_no_delete
BEFORE DELETE ON aoo_room_template_release_lock
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='template release lock is history protected';
END$$
DELIMITER ;
