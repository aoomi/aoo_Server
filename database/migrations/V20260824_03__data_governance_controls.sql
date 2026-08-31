-- Executable data-governance contracts for dictionary ownership, legacy mapping,
-- migration reconciliation, schema evolution, repair, privacy and query safety.

CREATE TABLE aoo_data_owner (
    owner_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    contact_uri VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (owner_code),
    CONSTRAINT chk_data_owner_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_table_governance (
    table_name VARCHAR(64) NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    data_classification VARCHAR(24) NOT NULL,
    authoritative_source VARCHAR(64) NOT NULL,
    hot_retention_days INT UNSIGNED NOT NULL,
    archive_retention_days INT UNSIGNED NOT NULL,
    purge_after_days INT UNSIGNED NULL,
    legal_hold_supported TINYINT UNSIGNED NOT NULL,
    anonymization_strategy VARCHAR(64) NULL,
    backup_purge_sla_days INT UNSIGNED NULL,
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (table_name),
    KEY idx_table_governance_owner (owner_code,lifecycle_status,table_name),
    CONSTRAINT fk_table_governance_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_table_governance_class CHECK (data_classification IN ('PUBLIC','INTERNAL','CONFIDENTIAL','RESTRICTED')),
    CONSTRAINT chk_table_governance_hold CHECK (legal_hold_supported IN (0,1)),
    CONSTRAINT chk_table_governance_retention CHECK (
        archive_retention_days >= hot_retention_days
        AND (purge_after_days IS NULL OR purge_after_days >= archive_retention_days)
    ),
    CONSTRAINT chk_table_governance_status CHECK (lifecycle_status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_enum_contract (
    enum_code VARCHAR(64) NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    java_type VARCHAR(256) NULL,
    protocol_type VARCHAR(256) NULL,
    frontend_type VARCHAR(256) NULL,
    compatibility_mode VARCHAR(16) NOT NULL DEFAULT 'STRICT',
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (enum_code,schema_version),
    KEY idx_enum_contract_owner (owner_code,status,enum_code),
    CONSTRAINT fk_enum_contract_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_enum_contract_mode CHECK (compatibility_mode IN ('STRICT','EXPAND_ONLY')),
    CONSTRAINT chk_enum_contract_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_enum_contract_status CHECK (status IN ('DRAFT','ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_enum_value_contract (
    enum_code VARCHAR(64) NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    wire_value VARCHAR(64) NOT NULL,
    numeric_value BIGINT NULL,
    display_name VARCHAR(128) NOT NULL,
    deprecated_at DATETIME(3) NULL,
    PRIMARY KEY (enum_code,schema_version,wire_value),
    UNIQUE KEY uk_enum_numeric_value (enum_code,schema_version,numeric_value),
    CONSTRAINT fk_enum_value_contract FOREIGN KEY (enum_code,schema_version)
        REFERENCES aoo_enum_contract(enum_code,schema_version) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_relationship_contract (
    relationship_code VARCHAR(128) NOT NULL,
    child_table VARCHAR(64) NOT NULL,
    child_columns VARCHAR(512) NOT NULL,
    parent_table VARCHAR(64) NOT NULL,
    parent_columns VARCHAR(512) NOT NULL,
    enforcement_type VARCHAR(16) NOT NULL,
    delete_action VARCHAR(16) NOT NULL,
    orphan_detector_sql TEXT NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (relationship_code),
    KEY idx_relationship_child (child_table,status,relationship_code),
    KEY idx_relationship_parent (parent_table,status,relationship_code),
    CONSTRAINT fk_relationship_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_relationship_enforcement CHECK (enforcement_type IN ('FOREIGN_KEY','LOGICAL_GATE')),
    CONSTRAINT chk_relationship_delete CHECK (delete_action IN ('RESTRICT','CASCADE','SET_NULL','ARCHIVE','ANONYMIZE')),
    CONSTRAINT chk_relationship_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_json_schema_registry (
    schema_code VARCHAR(128) NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    json_schema JSON NOT NULL,
    content_hash CHAR(64) NOT NULL,
    compatibility_floor INT UNSIGNED NULL,
    migration_component_key VARCHAR(192) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (schema_code,schema_version),
    UNIQUE KEY uk_json_schema_hash (schema_code,content_hash),
    KEY idx_json_schema_status (status,schema_code,schema_version),
    CONSTRAINT fk_json_schema_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_json_schema_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_json_schema_status CHECK (status IN ('DRAFT','ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_legacy_source_object (
    source_system VARCHAR(64) NOT NULL,
    source_object VARCHAR(128) NOT NULL,
    source_kind VARCHAR(16) NOT NULL,
    source_hash CHAR(64) NOT NULL,
    record_count BIGINT UNSIGNED NOT NULL,
    extraction_status VARCHAR(16) NOT NULL,
    extracted_at DATETIME(3) NOT NULL,
    PRIMARY KEY (source_system,source_object,source_hash),
    CONSTRAINT chk_legacy_source_kind CHECK (source_kind IN ('TABLE','JSON','CSV','EVENT','BLOB')),
    CONSTRAINT chk_legacy_source_hash CHECK (source_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_legacy_source_status CHECK (extraction_status IN ('DISCOVERED','EXTRACTED','QUARANTINED','MIGRATED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_legacy_record_envelope (
    source_system VARCHAR(64) NOT NULL,
    source_object VARCHAR(128) NOT NULL,
    source_primary_key VARCHAR(512) NOT NULL,
    source_snapshot_hash CHAR(64) NOT NULL,
    source_record_hash CHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    target_table VARCHAR(64) NULL,
    target_business_key VARCHAR(512) NULL,
    migration_run_id BIGINT UNSIGNED NULL,
    migration_status VARCHAR(24) NOT NULL DEFAULT 'EXTRACTED',
    extracted_at DATETIME(3) NOT NULL,
    migrated_at DATETIME(3) NULL,
    PRIMARY KEY (source_system,source_object,source_primary_key,source_snapshot_hash),
    UNIQUE KEY uk_legacy_record_hash (source_system,source_object,source_record_hash),
    KEY idx_legacy_record_migration (migration_status,source_system,source_object),
    CONSTRAINT chk_legacy_record_hashes CHECK (source_snapshot_hash REGEXP '^[0-9a-f]{64}$' AND source_record_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_legacy_record_status CHECK (migration_status IN ('EXTRACTED','VALIDATED','MAPPED','MIGRATED','QUARANTINED','REJECTED')),
    CONSTRAINT chk_legacy_record_target CHECK ((migration_status IN ('MAPPED','MIGRATED') AND target_table IS NOT NULL AND target_business_key IS NOT NULL) OR migration_status NOT IN ('MAPPED','MIGRATED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_legacy_field_mapping (
    mapping_id BIGINT UNSIGNED NOT NULL,
    domain_code VARCHAR(64) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    source_object VARCHAR(128) NOT NULL,
    source_field VARCHAR(128) NOT NULL,
    target_table VARCHAR(64) NOT NULL,
    target_column VARCHAR(64) NOT NULL,
    transform_expression TEXT NOT NULL,
    null_policy VARCHAR(16) NOT NULL,
    default_policy VARCHAR(256) NULL,
    validation_rule TEXT NOT NULL,
    reversibility VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    approved_by BIGINT UNSIGNED NULL,
    approved_at DATETIME(3) NULL,
    PRIMARY KEY (mapping_id),
    UNIQUE KEY uk_legacy_field_mapping (source_system,source_object,source_field,target_table,target_column),
    KEY idx_legacy_mapping_target (target_table,target_column,status),
    CONSTRAINT chk_legacy_mapping_null CHECK (null_policy IN ('REJECT','PRESERVE','DEFAULT','QUARANTINE')),
    CONSTRAINT chk_legacy_mapping_reversibility CHECK (reversibility IN ('REVERSIBLE','LOSSY_DOCUMENTED','NOT_REVERSIBLE')),
    CONSTRAINT chk_legacy_mapping_status CHECK (status IN ('DRAFT','APPROVED','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_migration_run (
    migration_run_id BIGINT UNSIGNED NOT NULL,
    migration_code VARCHAR(128) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    authority_mode VARCHAR(24) NOT NULL,
    schema_phase VARCHAR(16) NOT NULL,
    source_snapshot_hash CHAR(64) NOT NULL,
    source_count BIGINT UNSIGNED NOT NULL,
    target_count BIGINT UNSIGNED NULL,
    source_business_hash CHAR(64) NULL,
    target_business_hash CHAR(64) NULL,
    source_balance DECIMAL(38,9) NULL,
    target_balance DECIMAL(38,9) NULL,
    orphan_count BIGINT UNSIGNED NULL,
    mismatch_count BIGINT UNSIGNED NULL,
    checkpoint JSON NULL,
    rollback_manifest JSON NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (migration_run_id),
    UNIQUE KEY uk_migration_run_code_snapshot (migration_code,source_snapshot_hash),
    KEY idx_migration_run_status (status,started_at,migration_run_id),
    CONSTRAINT chk_migration_authority CHECK (authority_mode IN ('LEGACY','DUAL_WRITE','AOO','READ_ONLY_VERIFY')),
    CONSTRAINT chk_migration_schema_phase CHECK (schema_phase IN ('EXPAND','MIGRATE','CONTRACT')),
    CONSTRAINT chk_migration_source_hash CHECK (source_snapshot_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_migration_run_status CHECK (status IN ('PLANNED','DRY_RUN','RUNNING','PAUSED','VALIDATING','PASSED','FAILED','ROLLED_BACK')),
    CONSTRAINT chk_migration_counts CHECK (target_count IS NULL OR target_count <= 18446744073709551615)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_migration_validation (
    migration_run_id BIGINT UNSIGNED NOT NULL,
    validation_code VARCHAR(64) NOT NULL,
    validation_type VARCHAR(24) NOT NULL,
    expected_value VARCHAR(512) NOT NULL,
    actual_value VARCHAR(512) NOT NULL,
    sample_evidence JSON NULL,
    passed TINYINT UNSIGNED NOT NULL,
    validated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (migration_run_id,validation_code),
    KEY idx_migration_validation_failure (passed,validation_type,migration_run_id),
    CONSTRAINT fk_migration_validation_run FOREIGN KEY (migration_run_id)
        REFERENCES aoo_migration_run(migration_run_id) ON DELETE RESTRICT,
    CONSTRAINT chk_migration_validation_type CHECK (validation_type IN ('COUNT','HASH','BALANCE','FOREIGN_KEY','UNIQUENESS','ENUM','SAMPLE')),
    CONSTRAINT chk_migration_validation_passed CHECK (passed IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE aoo_legacy_record_envelope
    ADD CONSTRAINT fk_legacy_record_migration_run FOREIGN KEY (migration_run_id)
        REFERENCES aoo_migration_run(migration_run_id) ON DELETE RESTRICT;

CREATE TABLE aoo_dual_write_control (
    domain_code VARCHAR(64) NOT NULL,
    legacy_target VARCHAR(128) NOT NULL,
    aoo_target VARCHAR(128) NOT NULL,
    authoritative_side VARCHAR(16) NOT NULL,
    comparison_window_minutes INT UNSIGNED NOT NULL,
    max_mismatch_count BIGINT UNSIGNED NOT NULL,
    max_mismatch_ratio DECIMAL(9,8) NOT NULL,
    stop_condition JSON NOT NULL,
    rollback_procedure JSON NOT NULL,
    last_reconciled_at DATETIME(3) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DISABLED',
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (domain_code),
    CONSTRAINT chk_dual_write_authority CHECK (authoritative_side IN ('LEGACY','AOO')),
    CONSTRAINT chk_dual_write_ratio CHECK (max_mismatch_ratio >= 0 AND max_mismatch_ratio <= 1),
    CONSTRAINT chk_dual_write_status CHECK (status IN ('DISABLED','SHADOW','DUAL_WRITE','AOO_PRIMARY','ROLLBACK','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_schema_change_plan (
    change_id BIGINT UNSIGNED NOT NULL,
    migration_version VARCHAR(64) NOT NULL,
    affected_table VARCHAR(64) NOT NULL,
    schema_phase VARCHAR(16) NOT NULL,
    backward_compatible TINYINT UNSIGNED NOT NULL,
    online_ddl_algorithm VARCHAR(16) NOT NULL,
    lock_mode VARCHAR(16) NOT NULL,
    estimated_rows BIGINT UNSIGNED NOT NULL,
    estimated_bytes BIGINT UNSIGNED NOT NULL,
    maximum_lock_ms BIGINT UNSIGNED NOT NULL,
    maximum_replica_lag_ms BIGINT UNSIGNED NOT NULL,
    preflight_sql TEXT NOT NULL,
    verification_sql TEXT NOT NULL,
    rollback_sql TEXT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
    approved_by BIGINT UNSIGNED NULL,
    PRIMARY KEY (change_id),
    UNIQUE KEY uk_schema_change_table_phase (migration_version,affected_table,schema_phase),
    KEY idx_schema_change_status (status,migration_version,affected_table),
    CONSTRAINT chk_schema_change_phase CHECK (schema_phase IN ('EXPAND','MIGRATE','CONTRACT')),
    CONSTRAINT chk_schema_change_compatible CHECK (backward_compatible IN (0,1)),
    CONSTRAINT chk_schema_change_algorithm CHECK (online_ddl_algorithm IN ('INSTANT','INPLACE','COPY','EXTERNAL')),
    CONSTRAINT chk_schema_change_lock CHECK (lock_mode IN ('NONE','SHARED','EXCLUSIVE')),
    CONSTRAINT chk_schema_change_status CHECK (status IN ('PLANNED','PREFLIGHT_PASSED','RUNNING','VERIFIED','ROLLED_BACK','FAILED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_data_repair_job (
    repair_job_id BIGINT UNSIGNED NOT NULL,
    repair_code VARCHAR(128) NOT NULL,
    target_table VARCHAR(64) NOT NULL,
    predicate_hash CHAR(64) NOT NULL,
    operation_hash CHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    dry_run TINYINT UNSIGNED NOT NULL DEFAULT 1,
    maximum_rows BIGINT UNSIGNED NOT NULL,
    rows_per_second INT UNSIGNED NOT NULL,
    chunk_size INT UNSIGNED NOT NULL,
    resume_checkpoint JSON NULL,
    before_image_location VARCHAR(512) NOT NULL,
    rollback_manifest JSON NOT NULL,
    requested_by BIGINT UNSIGNED NOT NULL,
    approved_by BIGINT UNSIGNED NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    affected_rows BIGINT UNSIGNED NOT NULL DEFAULT 0,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (repair_job_id),
    UNIQUE KEY uk_data_repair_idempotency (idempotency_key),
    KEY idx_data_repair_status (status,target_table,repair_job_id),
    CONSTRAINT chk_data_repair_hashes CHECK (predicate_hash REGEXP '^[0-9a-f]{64}$' AND operation_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_data_repair_dry_run CHECK (dry_run IN (0,1)),
    CONSTRAINT chk_data_repair_rate CHECK (maximum_rows > 0 AND rows_per_second > 0 AND chunk_size > 0 AND chunk_size <= maximum_rows),
    CONSTRAINT chk_data_repair_status CHECK (status IN ('DRAFT','DRY_RUN','AWAITING_APPROVAL','APPROVED','RUNNING','PAUSED','SUCCEEDED','FAILED','ROLLED_BACK')),
    CONSTRAINT chk_data_repair_approval CHECK (status NOT IN ('APPROVED','RUNNING','SUCCEEDED') OR approved_by IS NOT NULL)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_data_repair_audit (
    repair_job_id BIGINT UNSIGNED NOT NULL,
    audit_sequence BIGINT UNSIGNED NOT NULL,
    action VARCHAR(24) NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    checkpoint JSON NULL,
    affected_rows BIGINT UNSIGNED NOT NULL,
    result_hash CHAR(64) NOT NULL,
    detail JSON NOT NULL,
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (repair_job_id,audit_sequence),
    CONSTRAINT fk_data_repair_audit_job FOREIGN KEY (repair_job_id)
        REFERENCES aoo_data_repair_job(repair_job_id) ON DELETE RESTRICT,
    CONSTRAINT chk_data_repair_audit_action CHECK (action IN ('CREATE','DRY_RUN','APPROVE','START','CHUNK','PAUSE','RESUME','COMPLETE','FAIL','ROLLBACK')),
    CONSTRAINT chk_data_repair_audit_hash CHECK (result_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_data_quality_issue (
    issue_id BIGINT UNSIGNED NOT NULL,
    detector_code VARCHAR(128) NOT NULL,
    issue_type VARCHAR(32) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_key VARCHAR(256) NOT NULL,
    evidence JSON NOT NULL,
    evidence_hash CHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    detected_at DATETIME(3) NOT NULL,
    resolved_at DATETIME(3) NULL,
    repair_job_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (issue_id),
    UNIQUE KEY uk_data_quality_evidence (detector_code,object_type,object_key,evidence_hash),
    KEY idx_data_quality_queue (status,severity,detected_at,issue_id),
    CONSTRAINT fk_data_quality_repair FOREIGN KEY (repair_job_id)
        REFERENCES aoo_data_repair_job(repair_job_id) ON DELETE RESTRICT,
    CONSTRAINT chk_data_quality_type CHECK (issue_type IN ('INVALID_GAME','MISSING_REGION','DUPLICATE_TEMPLATE','ORPHAN_ROOM','BALANCE_MISMATCH','BROKEN_REFERENCE','INVALID_ENUM','INVALID_JSON')),
    CONSTRAINT chk_data_quality_hash CHECK (evidence_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_data_quality_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_data_quality_status CHECK (status IN ('OPEN','ACKNOWLEDGED','REPAIRING','RESOLVED','FALSE_POSITIVE'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_privacy_request (
    privacy_request_id BIGINT UNSIGNED NOT NULL,
    subject_type VARCHAR(16) NOT NULL,
    subject_id_hash CHAR(64) NOT NULL,
    request_type VARCHAR(24) NOT NULL,
    identity_verification_ref VARCHAR(256) NOT NULL,
    legal_basis VARCHAR(128) NOT NULL,
    requested_at DATETIME(3) NOT NULL,
    due_at DATETIME(3) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    completed_at DATETIME(3) NULL,
    result_manifest JSON NULL,
    PRIMARY KEY (privacy_request_id),
    UNIQUE KEY uk_privacy_request_subject (subject_id_hash,request_type,requested_at),
    KEY idx_privacy_request_due (status,due_at,privacy_request_id),
    CONSTRAINT chk_privacy_subject CHECK (subject_type IN ('PLAYER','OPERATOR')),
    CONSTRAINT chk_privacy_subject_hash CHECK (subject_id_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_privacy_request_type CHECK (request_type IN ('ACCESS','EXPORT','ANONYMIZE','DELETE','RESTRICT')),
    CONSTRAINT chk_privacy_request_status CHECK (status IN ('RECEIVED','VERIFIED','ON_HOLD','RUNNING','COMPLETED','REJECTED')),
    CONSTRAINT chk_privacy_request_due CHECK (due_at > requested_at)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_legal_hold (
    legal_hold_id BIGINT UNSIGNED NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_key_hash CHAR(64) NOT NULL,
    authority_reference VARCHAR(256) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    placed_by BIGINT UNSIGNED NOT NULL,
    placed_at DATETIME(3) NOT NULL,
    released_by BIGINT UNSIGNED NULL,
    released_at DATETIME(3) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (legal_hold_id),
    KEY idx_legal_hold_scope (scope_key_hash,status,placed_at),
    CONSTRAINT chk_legal_hold_scope CHECK (scope_type IN ('PLAYER','ROOM','CLUB','TIME_RANGE','DATASET')),
    CONSTRAINT chk_legal_hold_hash CHECK (scope_key_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_legal_hold_status CHECK (status IN ('ACTIVE','RELEASED')),
    CONSTRAINT chk_legal_hold_release CHECK ((status='ACTIVE' AND released_at IS NULL) OR (status='RELEASED' AND released_at IS NOT NULL AND released_by IS NOT NULL))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_soft_delete_contract (
    table_name VARCHAR(64) NOT NULL,
    deleted_column VARCHAR(64) NOT NULL,
    active_unique_key VARCHAR(256) NOT NULL,
    restore_conflict_policy VARCHAR(24) NOT NULL,
    history_query_policy VARCHAR(24) NOT NULL,
    detector_sql TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (table_name),
    CONSTRAINT chk_soft_delete_restore CHECK (restore_conflict_policy IN ('REJECT','RENAME','MERGE','MANUAL')),
    CONSTRAINT chk_soft_delete_history CHECK (history_query_policy IN ('EXCLUDE_DEFAULT','INCLUDE_EXPLICIT','ARCHIVE_ONLY')),
    CONSTRAINT chk_soft_delete_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_shard_route_contract (
    domain_code VARCHAR(64) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    route_key VARCHAR(64) NOT NULL,
    route_algorithm VARCHAR(64) NOT NULL,
    virtual_shard_count INT UNSIGNED NOT NULL,
    physical_shard_count INT UNSIGNED NOT NULL,
    cross_shard_query_policy VARCHAR(24) NOT NULL,
    maximum_fanout INT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (domain_code,table_name),
    CONSTRAINT chk_shard_counts CHECK (virtual_shard_count > 0 AND physical_shard_count > 0 AND virtual_shard_count >= physical_shard_count),
    CONSTRAINT chk_shard_fanout CHECK (maximum_fanout > 0 AND maximum_fanout <= physical_shard_count),
    CONSTRAINT chk_shard_policy CHECK (cross_shard_query_policy IN ('FORBIDDEN','ASYNC_AGGREGATE','BOUNDED_FANOUT')),
    CONSTRAINT chk_shard_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_pagination_contract (
    query_code VARCHAR(128) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    filter_columns VARCHAR(512) NOT NULL,
    order_columns VARCHAR(512) NOT NULL,
    cursor_columns VARCHAR(512) NOT NULL,
    maximum_page_size INT UNSIGNED NOT NULL,
    offset_allowed TINYINT UNSIGNED NOT NULL DEFAULT 0,
    covering_index_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (query_code),
    KEY idx_pagination_table (table_name,query_code),
    CONSTRAINT chk_pagination_size CHECK (maximum_page_size > 0 AND maximum_page_size <= 1000),
    CONSTRAINT chk_pagination_offset CHECK (offset_allowed IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_replica_read_policy (
    domain_code VARCHAR(64) NOT NULL,
    operation_code VARCHAR(128) NOT NULL,
    consistency_level VARCHAR(24) NOT NULL,
    read_your_write_seconds INT UNSIGNED NOT NULL,
    maximum_replica_lag_ms BIGINT UNSIGNED NOT NULL,
    fallback_to_primary TINYINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (domain_code,operation_code),
    CONSTRAINT chk_replica_consistency CHECK (consistency_level IN ('PRIMARY_ONLY','READ_YOUR_WRITE','BOUNDED_STALENESS','EVENTUAL')),
    CONSTRAINT chk_replica_fallback CHECK (fallback_to_primary IN (0,1)),
    CONSTRAINT chk_replica_policy_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_data_owner(owner_code,display_name,contact_uri) VALUES
('GAME_CATALOG','玩法目录与发布','owner://game-platform'),
('GAME_RUNTIME','房间、事件与回放','owner://game-runtime'),
('PLAYER_ASSET','玩家资产与账务','owner://billing'),
('CLUB','亲友圈与模板','owner://club'),
('PLATFORM_DATA','迁移、治理与运维','owner://data-platform'),
('SECURITY_PRIVACY','隐私、留存与审计','owner://security-privacy');

INSERT INTO aoo_table_governance(
    table_name,owner_code,data_classification,authoritative_source,hot_retention_days,
    archive_retention_days,purge_after_days,legal_hold_supported,
    anonymization_strategy,backup_purge_sla_days
) VALUES
('aoo_game_catalog','GAME_CATALOG','INTERNAL','AOO_DB',3650,3650,NULL,0,NULL,NULL),
('aoo_play_version','GAME_CATALOG','INTERNAL','AOO_DB',3650,3650,NULL,0,NULL,NULL),
('aoo_game_release','GAME_CATALOG','INTERNAL','AOO_DB',3650,3650,NULL,1,NULL,NULL),
('aoo_room_rule_lock','GAME_RUNTIME','CONFIDENTIAL','AOO_DB',180,730,2555,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_snapshot','GAME_RUNTIME','CONFIDENTIAL','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_event','GAME_RUNTIME','CONFIDENTIAL','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('perspective_replay_event','GAME_RUNTIME','RESTRICTED','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('aoo_ledger','PLAYER_ASSET','RESTRICTED','AOO_DB',730,2555,3650,1,'PLAYER_ID_TOKENIZE',35),
('aoo_currency_balance','PLAYER_ASSET','RESTRICTED','AOO_DB',3650,3650,NULL,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_template','CLUB','CONFIDENTIAL','AOO_DB',365,730,2555,1,'PLAYER_ID_TOKENIZE',35),
('aoo_admin_audit','SECURITY_PRIVACY','RESTRICTED','AOO_DB',365,2555,3650,1,'OPERATOR_ID_TOKENIZE',35),
('aoo_privacy_request','SECURITY_PRIVACY','RESTRICTED','AOO_DB',365,2555,3650,1,'SUBJECT_HASH_ONLY',35);

INSERT INTO aoo_replica_read_policy(
    domain_code,operation_code,consistency_level,read_your_write_seconds,
    maximum_replica_lag_ms,fallback_to_primary
) VALUES
('ROOM_TEMPLATE','CREATE_OR_UPDATE_TEMPLATE','PRIMARY_ONLY',60,0,1),
('ROOM_TEMPLATE','READ_AFTER_UPDATE','READ_YOUR_WRITE',60,1000,1),
('PLAYER_ASSET','BALANCE_AND_LEDGER','PRIMARY_ONLY',300,0,1),
('GAME_RUNTIME','CREATE_OR_RECOVER_ROOM','PRIMARY_ONLY',300,0,1),
('HISTORY','LIST_SETTLEMENTS','BOUNDED_STALENESS',0,3000,1);

INSERT INTO aoo_pagination_contract(
    query_code,table_name,filter_columns,order_columns,cursor_columns,
    maximum_page_size,offset_allowed,covering_index_name
) VALUES
('ROOM_EVENT_FORWARD','aoo_room_event','room_id','event_sequence ASC','room_id,event_sequence',500,0,'PRIMARY'),
('LEDGER_PLAYER_HISTORY','aoo_ledger','player_id,currency,currency_scope_id','created_at DESC,ledger_id DESC','created_at,ledger_id',200,0,'idx_ledger_player_scope_time'),
('CLUB_MEMBER_DIRECTORY','aoo_club_member','club_id,member_status','player_id ASC','player_id',200,0,'idx_club_member_page'),
('ROOM_TEMPLATE_ACTIVE','aoo_room_template','club_id,status','updated_at DESC,template_code ASC','updated_at,template_code',100,0,'idx_template_catalog'),
('REPLAY_PLAYER_VIEW','perspective_replay_event','owner_player_id,room_id,set_id','event_sequence ASC','event_sequence',500,0,'idx_replay_player_view');
