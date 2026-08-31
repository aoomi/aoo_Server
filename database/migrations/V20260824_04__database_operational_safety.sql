-- Durable operational evidence. A plan or policy row is not a pass: observation
-- and drill rows record the actual environment, dataset hash, thresholds and result.

CREATE TABLE aoo_online_ddl_observation (
    observation_id BIGINT UNSIGNED NOT NULL,
    change_id BIGINT UNSIGNED NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    mysql_version VARCHAR(64) NOT NULL,
    table_rows_before BIGINT UNSIGNED NOT NULL,
    table_bytes_before BIGINT UNSIGNED NOT NULL,
    lock_wait_ms BIGINT UNSIGNED NOT NULL,
    replica_lag_peak_ms BIGINT UNSIGNED NOT NULL,
    rows_copied BIGINT UNSIGNED NOT NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NOT NULL,
    passed TINYINT UNSIGNED NOT NULL,
    evidence_uri VARCHAR(512) NOT NULL,
    PRIMARY KEY (observation_id),
    UNIQUE KEY uk_online_ddl_observation (change_id,environment_code,started_at),
    KEY idx_online_ddl_result (passed,completed_at,change_id),
    CONSTRAINT fk_online_ddl_change FOREIGN KEY (change_id)
        REFERENCES aoo_schema_change_plan(change_id) ON DELETE RESTRICT,
    CONSTRAINT chk_online_ddl_time CHECK (completed_at >= started_at),
    CONSTRAINT chk_online_ddl_passed CHECK (passed IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_query_baseline (
    query_code VARCHAR(128) NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    sql_template TEXT NOT NULL,
    sql_hash CHAR(64) NOT NULL,
    expected_index VARCHAR(64) NOT NULL,
    maximum_examined_rows BIGINT UNSIGNED NOT NULL,
    maximum_result_rows BIGINT UNSIGNED NOT NULL,
    maximum_p95_ms DECIMAL(12,3) NOT NULL,
    maximum_p99_ms DECIMAL(12,3) NOT NULL,
    full_scan_allowed TINYINT UNSIGNED NOT NULL DEFAULT 0,
    filesort_allowed TINYINT UNSIGNED NOT NULL DEFAULT 0,
    temporary_table_allowed TINYINT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (query_code),
    CONSTRAINT fk_query_baseline_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_query_baseline_hash CHECK (sql_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_query_baseline_limits CHECK (maximum_result_rows > 0 AND maximum_examined_rows >= maximum_result_rows AND maximum_p99_ms >= maximum_p95_ms),
    CONSTRAINT chk_query_baseline_flags CHECK (full_scan_allowed IN (0,1) AND filesort_allowed IN (0,1) AND temporary_table_allowed IN (0,1)),
    CONSTRAINT chk_query_baseline_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_query_plan_observation (
    observation_id BIGINT UNSIGNED NOT NULL,
    query_code VARCHAR(128) NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    mysql_version VARCHAR(64) NOT NULL,
    dataset_hash CHAR(64) NOT NULL,
    dataset_rows BIGINT UNSIGNED NOT NULL,
    explain_json JSON NOT NULL,
    access_type VARCHAR(24) NOT NULL,
    selected_index VARCHAR(64) NULL,
    examined_rows BIGINT UNSIGNED NOT NULL,
    result_rows BIGINT UNSIGNED NOT NULL,
    uses_filesort TINYINT UNSIGNED NOT NULL,
    uses_temporary_table TINYINT UNSIGNED NOT NULL,
    p50_ms DECIMAL(12,3) NOT NULL,
    p95_ms DECIMAL(12,3) NOT NULL,
    p99_ms DECIMAL(12,3) NOT NULL,
    sample_count INT UNSIGNED NOT NULL,
    passed TINYINT UNSIGNED NOT NULL,
    observed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (observation_id),
    UNIQUE KEY uk_query_plan_environment_dataset (query_code,environment_code,dataset_hash,observed_at),
    KEY idx_query_plan_failures (passed,observed_at,query_code),
    CONSTRAINT fk_query_plan_baseline FOREIGN KEY (query_code)
        REFERENCES aoo_query_baseline(query_code) ON DELETE RESTRICT,
    CONSTRAINT chk_query_plan_dataset_hash CHECK (dataset_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_query_plan_order CHECK (p50_ms <= p95_ms AND p95_ms <= p99_ms),
    CONSTRAINT chk_query_plan_flags CHECK (uses_filesort IN (0,1) AND uses_temporary_table IN (0,1) AND passed IN (0,1)),
    CONSTRAINT chk_query_plan_samples CHECK (sample_count > 0)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_connection_pool_observation (
    observation_id BIGINT UNSIGNED NOT NULL,
    service_code VARCHAR(64) NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    test_scenario VARCHAR(128) NOT NULL,
    concurrency INT UNSIGNED NOT NULL,
    iteration_count BIGINT UNSIGNED NOT NULL,
    connections_before INT UNSIGNED NOT NULL,
    connections_after INT UNSIGNED NOT NULL,
    abandoned_connections INT UNSIGNED NOT NULL,
    open_transactions INT UNSIGNED NOT NULL,
    stream_cancellations BIGINT UNSIGNED NOT NULL,
    exception_paths BIGINT UNSIGNED NOT NULL,
    duration_ms BIGINT UNSIGNED NOT NULL,
    passed TINYINT UNSIGNED NOT NULL,
    evidence_uri VARCHAR(512) NOT NULL,
    observed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (observation_id),
    KEY idx_connection_pool_result (passed,service_code,observed_at),
    CONSTRAINT chk_connection_pool_counts CHECK (concurrency > 0 AND iteration_count > 0 AND duration_ms > 0),
    CONSTRAINT chk_connection_pool_passed CHECK (passed IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_deadlock_retry_policy (
    transaction_code VARCHAR(128) NOT NULL,
    owner_code VARCHAR(64) NOT NULL,
    lock_order VARCHAR(1000) NOT NULL,
    idempotency_key_required TINYINT UNSIGNED NOT NULL,
    maximum_attempts INT UNSIGNED NOT NULL,
    initial_backoff_ms INT UNSIGNED NOT NULL,
    maximum_backoff_ms INT UNSIGNED NOT NULL,
    jitter_percent DECIMAL(5,2) NOT NULL,
    retryable_sqlstates VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (transaction_code),
    CONSTRAINT fk_deadlock_policy_owner FOREIGN KEY (owner_code)
        REFERENCES aoo_data_owner(owner_code) ON DELETE RESTRICT,
    CONSTRAINT chk_deadlock_policy_idempotency CHECK (idempotency_key_required IN (0,1)),
    CONSTRAINT chk_deadlock_policy_attempts CHECK (maximum_attempts BETWEEN 1 AND 20 AND maximum_backoff_ms >= initial_backoff_ms),
    CONSTRAINT chk_deadlock_policy_jitter CHECK (jitter_percent >= 0 AND jitter_percent <= 100),
    CONSTRAINT chk_deadlock_policy_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_deadlock_incident (
    incident_id BIGINT UNSIGNED NOT NULL,
    transaction_code VARCHAR(128) NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    attempt_count INT UNSIGNED NOT NULL,
    victim_statement_hash CHAR(64) NOT NULL,
    blocker_statement_hash CHAR(64) NOT NULL,
    innodb_deadlock_snapshot JSON NOT NULL,
    idempotent_result_preserved TINYINT UNSIGNED NOT NULL,
    recovered TINYINT UNSIGNED NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    PRIMARY KEY (incident_id),
    KEY idx_deadlock_incident_transaction (transaction_code,occurred_at,incident_id),
    CONSTRAINT fk_deadlock_incident_policy FOREIGN KEY (transaction_code)
        REFERENCES aoo_deadlock_retry_policy(transaction_code) ON DELETE RESTRICT,
    CONSTRAINT chk_deadlock_incident_hashes CHECK (victim_statement_hash REGEXP '^[0-9a-f]{64}$' AND blocker_statement_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_deadlock_incident_flags CHECK (idempotent_result_preserved IN (0,1) AND recovered IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_backup_manifest (
    backup_id BIGINT UNSIGNED NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    backup_type VARCHAR(16) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NOT NULL,
    binlog_file VARCHAR(128) NOT NULL,
    binlog_position BIGINT UNSIGNED NOT NULL,
    gtid_set TEXT NOT NULL,
    object_uri VARCHAR(512) NOT NULL,
    encrypted TINYINT UNSIGNED NOT NULL,
    encryption_key_version VARCHAR(64) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    PRIMARY KEY (backup_id),
    UNIQUE KEY uk_backup_content (environment_code,content_hash),
    KEY idx_backup_expiry (status,expires_at,backup_id),
    CONSTRAINT chk_backup_type CHECK (backup_type IN ('FULL','INCREMENTAL','BINLOG')),
    CONSTRAINT chk_backup_time CHECK (completed_at >= started_at AND expires_at > completed_at),
    CONSTRAINT chk_backup_encrypted CHECK (encrypted=1),
    CONSTRAINT chk_backup_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_backup_status CHECK (status IN ('CREATING','VERIFIED','CORRUPT','EXPIRED','PURGED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_restore_drill (
    drill_id BIGINT UNSIGNED NOT NULL,
    backup_id BIGINT UNSIGNED NOT NULL,
    target_environment VARCHAR(64) NOT NULL,
    restore_point DATETIME(3) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NOT NULL,
    rto_seconds BIGINT UNSIGNED NOT NULL,
    rpo_seconds BIGINT UNSIGNED NOT NULL,
    table_count_expected INT UNSIGNED NOT NULL,
    table_count_actual INT UNSIGNED NOT NULL,
    asset_hash_expected CHAR(64) NOT NULL,
    asset_hash_actual CHAR(64) NOT NULL,
    room_history_hash_expected CHAR(64) NOT NULL,
    room_history_hash_actual CHAR(64) NOT NULL,
    relationship_mismatch_count BIGINT UNSIGNED NOT NULL,
    passed TINYINT UNSIGNED NOT NULL,
    evidence_uri VARCHAR(512) NOT NULL,
    PRIMARY KEY (drill_id),
    KEY idx_restore_drill_result (passed,completed_at,drill_id),
    CONSTRAINT fk_restore_drill_backup FOREIGN KEY (backup_id)
        REFERENCES aoo_backup_manifest(backup_id) ON DELETE RESTRICT,
    CONSTRAINT chk_restore_drill_time CHECK (completed_at >= started_at),
    CONSTRAINT chk_restore_drill_hashes CHECK (
        asset_hash_expected REGEXP '^[0-9a-f]{64}$' AND asset_hash_actual REGEXP '^[0-9a-f]{64}$'
        AND room_history_hash_expected REGEXP '^[0-9a-f]{64}$' AND room_history_hash_actual REGEXP '^[0-9a-f]{64}$'
    ),
    CONSTRAINT chk_restore_drill_passed CHECK (passed IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_replica_failover_drill (
    drill_id BIGINT UNSIGNED NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    old_primary_server_uuid CHAR(36) NOT NULL,
    new_primary_server_uuid CHAR(36) NOT NULL,
    expected_gtid_set TEXT NOT NULL,
    promoted_gtid_set TEXT NOT NULL,
    read_only_guard_verified TINYINT UNSIGNED NOT NULL,
    connection_reroute_verified TINYINT UNSIGNED NOT NULL,
    identity_generation_verified TINYINT UNSIGNED NOT NULL,
    writes_fenced_verified TINYINT UNSIGNED NOT NULL,
    data_loss_rows BIGINT UNSIGNED NOT NULL,
    unavailable_ms BIGINT UNSIGNED NOT NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NOT NULL,
    passed TINYINT UNSIGNED NOT NULL,
    evidence_uri VARCHAR(512) NOT NULL,
    PRIMARY KEY (drill_id),
    KEY idx_replica_failover_result (passed,completed_at,drill_id),
    CONSTRAINT chk_failover_distinct CHECK (old_primary_server_uuid <> new_primary_server_uuid),
    CONSTRAINT chk_failover_flags CHECK (read_only_guard_verified IN (0,1) AND connection_reroute_verified IN (0,1) AND identity_generation_verified IN (0,1) AND writes_fenced_verified IN (0,1) AND passed IN (0,1)),
    CONSTRAINT chk_failover_time CHECK (completed_at >= started_at)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_capacity_forecast (
    forecast_id BIGINT UNSIGNED NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    baseline_at DATETIME(3) NOT NULL,
    baseline_rows BIGINT UNSIGNED NOT NULL,
    baseline_bytes BIGINT UNSIGNED NOT NULL,
    daily_row_growth DECIMAL(20,4) NOT NULL,
    daily_byte_growth DECIMAL(20,4) NOT NULL,
    retention_days INT UNSIGNED NULL,
    forecast_30d_bytes BIGINT UNSIGNED NOT NULL,
    forecast_90d_bytes BIGINT UNSIGNED NOT NULL,
    forecast_365d_bytes BIGINT UNSIGNED NOT NULL,
    warning_threshold_bytes BIGINT UNSIGNED NOT NULL,
    critical_threshold_bytes BIGINT UNSIGNED NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    input_hash CHAR(64) NOT NULL,
    PRIMARY KEY (forecast_id),
    UNIQUE KEY uk_capacity_forecast (table_name,environment_code,baseline_at,model_version),
    KEY idx_capacity_threshold (environment_code,forecast_90d_bytes,critical_threshold_bytes),
    CONSTRAINT chk_capacity_growth CHECK (daily_row_growth >= 0 AND daily_byte_growth >= 0),
    CONSTRAINT chk_capacity_horizon CHECK (forecast_30d_bytes <= forecast_90d_bytes AND forecast_90d_bytes <= forecast_365d_bytes),
    CONSTRAINT chk_capacity_threshold CHECK (critical_threshold_bytes > warning_threshold_bytes),
    CONSTRAINT chk_capacity_hash CHECK (input_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_archive_run (
    archive_run_id BIGINT UNSIGNED NOT NULL,
    source_table VARCHAR(64) NOT NULL,
    archive_table VARCHAR(64) NOT NULL,
    partition_name VARCHAR(64) NULL,
    boundary_start DATETIME(3) NOT NULL,
    boundary_end DATETIME(3) NOT NULL,
    source_count BIGINT UNSIGNED NOT NULL,
    archive_count BIGINT UNSIGNED NOT NULL,
    source_hash CHAR(64) NOT NULL,
    archive_hash CHAR(64) NOT NULL,
    legal_hold_excluded_count BIGINT UNSIGNED NOT NULL,
    reclaimed_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (archive_run_id),
    UNIQUE KEY uk_archive_run_idempotency (idempotency_key),
    KEY idx_archive_run_status (status,source_table,boundary_end),
    CONSTRAINT chk_archive_boundary CHECK (boundary_end > boundary_start),
    CONSTRAINT chk_archive_hashes CHECK (source_hash REGEXP '^[0-9a-f]{64}$' AND archive_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_archive_status CHECK (status IN ('DRY_RUN','COPYING','VERIFYING','SWITCHED','PURGING','SUCCEEDED','FAILED','ROLLED_BACK'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_data_repair_approval (
    repair_job_id BIGINT UNSIGNED NOT NULL,
    approval_role VARCHAR(24) NOT NULL,
    approver_id BIGINT UNSIGNED NOT NULL,
    scope_hash CHAR(64) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    decided_at DATETIME(3) NOT NULL,
    PRIMARY KEY (repair_job_id,approval_role),
    UNIQUE KEY uk_repair_approval_actor (repair_job_id,approver_id),
    CONSTRAINT fk_repair_approval_job FOREIGN KEY (repair_job_id)
        REFERENCES aoo_data_repair_job(repair_job_id) ON DELETE RESTRICT,
    CONSTRAINT chk_repair_approval_role CHECK (approval_role IN ('DATA_OWNER','DBA','SECURITY')),
    CONSTRAINT chk_repair_approval_hash CHECK (scope_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_repair_approval_decision CHECK (decision IN ('APPROVED','REJECTED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_statistics_plan_baseline (
    baseline_id BIGINT UNSIGNED NOT NULL,
    query_code VARCHAR(128) NOT NULL,
    environment_code VARCHAR(64) NOT NULL,
    table_statistics_hash CHAR(64) NOT NULL,
    plan_hash CHAR(64) NOT NULL,
    parameter_bucket VARCHAR(128) NOT NULL,
    selected_index VARCHAR(64) NULL,
    examined_rows BIGINT UNSIGNED NOT NULL,
    p95_ms DECIMAL(12,3) NOT NULL,
    captured_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (baseline_id),
    UNIQUE KEY uk_statistics_plan_bucket (query_code,environment_code,parameter_bucket,captured_at),
    KEY idx_statistics_plan_expiry (status,expires_at,baseline_id),
    CONSTRAINT fk_statistics_plan_query FOREIGN KEY (query_code)
        REFERENCES aoo_query_baseline(query_code) ON DELETE RESTRICT,
    CONSTRAINT chk_statistics_plan_hashes CHECK (table_statistics_hash REGEXP '^[0-9a-f]{64}$' AND plan_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_statistics_plan_expiry CHECK (expires_at > captured_at),
    CONSTRAINT chk_statistics_plan_status CHECK (status IN ('ACTIVE','DRIFTED','EXPIRED','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_deadlock_retry_policy(
    transaction_code,owner_code,lock_order,idempotency_key_required,maximum_attempts,
    initial_backoff_ms,maximum_backoff_ms,jitter_percent,retryable_sqlstates
) VALUES
('ASSET_DEBIT_AND_LEDGER','PLAYER_ASSET','currency_balance(player_id,currency,scope)->ledger(business_id)->outbox(event_id)',1,5,20,500,25.00,'40001,41000,1213'),
('ROOM_SETTLEMENT','GAME_RUNTIME','room_snapshot(room_id)->settlement(room_id,round_no)->ledger(player_id asc)->outbox(event_id)',1,5,20,500,25.00,'40001,41000,1213'),
('CLUB_TEMPLATE_UPDATE','CLUB','club_guard(club_id)->room_template(club_id,template_code,version)',1,4,25,400,25.00,'40001,41000,1213'),
('CLUB_MEMBER_UPDATE','CLUB','club_guard(club_id)->club_member(club_id,player_id)',1,4,25,400,25.00,'40001,41000,1213');

INSERT INTO aoo_query_baseline(
    query_code,owner_code,sql_template,sql_hash,expected_index,maximum_examined_rows,
    maximum_result_rows,maximum_p95_ms,maximum_p99_ms
) VALUES
('ROOM_CREATE_INDEX_DIRECT','GAME_CATALOG','SELECT * FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i USING(game_id,region_code,play_version,index_generation) WHERE a.game_id=? AND a.region_code=?','d403d4453fe63f16923afce77272e989bc9d2c3c111fd9c484d16cf26dbc16b2','PRIMARY',2,1,5.000,15.000),
('ROOM_EVENT_FORWARD','GAME_RUNTIME','SELECT * FROM aoo_room_event WHERE room_id=? AND event_sequence>? AND visibility=? AND owner_player_id=? ORDER BY event_sequence LIMIT ?','872030171aa04aa4a38f8135df63981f200d7acc7f6d1dbefdca7ddbfd60d704','PRIMARY',500,500,20.000,50.000),
('LEDGER_PLAYER_HISTORY','PLAYER_ASSET','SELECT * FROM aoo_ledger WHERE player_id=? AND currency=? AND currency_scope_id=? AND (created_at,ledger_id)<(?,?) ORDER BY created_at DESC,ledger_id DESC LIMIT ?','da9157cae29db97de5473ad6358ccdc5b7b55d86d254fea4d66fd2a6eff824ba','idx_ledger_player_scope_time',200,200,30.000,75.000),
('CLUB_MEMBER_KEYSET','CLUB','SELECT * FROM aoo_club_member WHERE club_id=? AND member_status=? AND player_id>? ORDER BY player_id LIMIT ?','20554d44a1b29f4daeb3453c1cfd38cea385a5d117cb7d566242a0bdb014c82a','idx_club_member_page',200,200,30.000,75.000),
('REPLAY_PLAYER_FORWARD','GAME_RUNTIME','SELECT * FROM perspective_replay_event WHERE owner_player_id=? AND room_id=? AND set_id=? AND event_sequence>? ORDER BY event_sequence LIMIT ?','256bfea4fbdca03bb0b7341c91f7c3bdefa7f75003c780a4adafce8beeee82e6','idx_replay_player_view',500,500,50.000,125.000);
