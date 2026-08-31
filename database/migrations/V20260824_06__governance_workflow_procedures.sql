-- Fail-closed workflow procedures and evidence scoring. Callers cannot declare a
-- migration/drill/plan passed without satisfying the stored database contract.

DELIMITER $$
CREATE PROCEDURE aoo_finalize_migration_run(IN p_migration_run_id BIGINT UNSIGNED)
BEGIN
    DECLARE v_source_count BIGINT UNSIGNED;
    DECLARE v_target_count BIGINT UNSIGNED;
    DECLARE v_source_hash CHAR(64);
    DECLARE v_target_hash CHAR(64);
    DECLARE v_source_balance DECIMAL(38,9);
    DECLARE v_target_balance DECIMAL(38,9);
    DECLARE v_orphans BIGINT UNSIGNED;
    DECLARE v_mismatches BIGINT UNSIGNED;
    DECLARE v_failed_validations BIGINT UNSIGNED;

    START TRANSACTION;
    SELECT source_count,target_count,source_business_hash,target_business_hash,
           source_balance,target_balance,orphan_count,mismatch_count
      INTO v_source_count,v_target_count,v_source_hash,v_target_hash,
           v_source_balance,v_target_balance,v_orphans,v_mismatches
      FROM aoo_migration_run
     WHERE migration_run_id=p_migration_run_id AND status='VALIDATING'
     FOR UPDATE;

    SELECT COUNT(*) INTO v_failed_validations
      FROM aoo_migration_validation
     WHERE migration_run_id=p_migration_run_id AND passed=0;

    IF v_target_count IS NULL OR v_source_hash IS NULL OR v_target_hash IS NULL
       OR v_orphans IS NULL OR v_mismatches IS NULL THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='migration evidence is incomplete';
    END IF;

    UPDATE aoo_migration_run
       SET status=IF(
               v_source_count=v_target_count
               AND v_source_hash=v_target_hash
               AND (v_source_balance <=> v_target_balance)
               AND v_orphans=0 AND v_mismatches=0 AND v_failed_validations=0,
               'PASSED','FAILED'),
           completed_at=CURRENT_TIMESTAMP(3)
     WHERE migration_run_id=p_migration_run_id;
    COMMIT;
END$$

CREATE PROCEDURE aoo_approve_data_repair(IN p_repair_job_id BIGINT UNSIGNED)
BEGIN
    DECLARE v_requested_by BIGINT UNSIGNED;
    DECLARE v_owner_approver BIGINT UNSIGNED;
    DECLARE v_dba_approver BIGINT UNSIGNED;
    DECLARE v_scope_count INT;

    START TRANSACTION;
    SELECT requested_by INTO v_requested_by
      FROM aoo_data_repair_job
     WHERE repair_job_id=p_repair_job_id
       AND status='AWAITING_APPROVAL' AND dry_run=1
     FOR UPDATE;

    SELECT MAX(IF(approval_role='DATA_OWNER' AND decision='APPROVED',approver_id,NULL)),
           MAX(IF(approval_role='DBA' AND decision='APPROVED',approver_id,NULL)),
           COUNT(DISTINCT IF(decision='APPROVED',scope_hash,NULL))
      INTO v_owner_approver,v_dba_approver,v_scope_count
      FROM aoo_data_repair_approval
     WHERE repair_job_id=p_repair_job_id;

    IF v_owner_approver IS NULL OR v_dba_approver IS NULL OR v_scope_count<>1
       OR v_owner_approver=v_dba_approver
       OR v_owner_approver=v_requested_by OR v_dba_approver=v_requested_by THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='independent DATA_OWNER and DBA approvals with identical scope are required';
    END IF;

    UPDATE aoo_data_repair_job
       SET status='APPROVED',approved_by=v_dba_approver
     WHERE repair_job_id=p_repair_job_id;
    COMMIT;
END$$

CREATE PROCEDURE aoo_complete_privacy_request(IN p_privacy_request_id BIGINT UNSIGNED,IN p_result_manifest JSON)
BEGIN
    DECLARE v_subject_hash CHAR(64);
    DECLARE v_active_holds BIGINT UNSIGNED;

    START TRANSACTION;
    SELECT subject_id_hash INTO v_subject_hash
      FROM aoo_privacy_request
     WHERE privacy_request_id=p_privacy_request_id
       AND status='RUNNING'
     FOR UPDATE;

    SELECT COUNT(*) INTO v_active_holds
      FROM aoo_legal_hold
     WHERE scope_key_hash=v_subject_hash AND status='ACTIVE';

    IF v_active_holds>0 THEN
        UPDATE aoo_privacy_request SET status='ON_HOLD'
         WHERE privacy_request_id=p_privacy_request_id;
    ELSE
        UPDATE aoo_privacy_request
           SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP(3),result_manifest=p_result_manifest
         WHERE privacy_request_id=p_privacy_request_id;
    END IF;
    COMMIT;
END$$

CREATE TRIGGER trg_query_plan_score_insert
BEFORE INSERT ON aoo_query_plan_observation
FOR EACH ROW
BEGIN
    DECLARE v_max_examined BIGINT UNSIGNED;
    DECLARE v_max_result BIGINT UNSIGNED;
    DECLARE v_max_p95 DECIMAL(12,3);
    DECLARE v_max_p99 DECIMAL(12,3);
    DECLARE v_full_scan TINYINT UNSIGNED;
    DECLARE v_filesort TINYINT UNSIGNED;
    DECLARE v_temporary TINYINT UNSIGNED;
    DECLARE v_expected_index VARCHAR(64);

    SELECT maximum_examined_rows,maximum_result_rows,maximum_p95_ms,maximum_p99_ms,
           full_scan_allowed,filesort_allowed,temporary_table_allowed,expected_index
      INTO v_max_examined,v_max_result,v_max_p95,v_max_p99,
           v_full_scan,v_filesort,v_temporary,v_expected_index
      FROM aoo_query_baseline WHERE query_code=NEW.query_code;

    SET NEW.passed=(
        NEW.examined_rows<=v_max_examined AND NEW.result_rows<=v_max_result
        AND NEW.p95_ms<=v_max_p95 AND NEW.p99_ms<=v_max_p99
        AND (v_full_scan=1 OR NEW.access_type NOT IN ('ALL','INDEX'))
        AND (v_filesort=1 OR NEW.uses_filesort=0)
        AND (v_temporary=1 OR NEW.uses_temporary_table=0)
        AND NEW.selected_index=v_expected_index
    );
END$$

CREATE TRIGGER trg_online_ddl_score_insert
BEFORE INSERT ON aoo_online_ddl_observation
FOR EACH ROW
BEGIN
    DECLARE v_max_lock BIGINT UNSIGNED;
    DECLARE v_max_lag BIGINT UNSIGNED;
    SELECT maximum_lock_ms,maximum_replica_lag_ms
      INTO v_max_lock,v_max_lag
      FROM aoo_schema_change_plan WHERE change_id=NEW.change_id;
    SET NEW.passed=(NEW.completed_at>=NEW.started_at AND NEW.lock_wait_ms<=v_max_lock AND NEW.replica_lag_peak_ms<=v_max_lag);
END$$

CREATE TRIGGER trg_connection_pool_score_insert
BEFORE INSERT ON aoo_connection_pool_observation
FOR EACH ROW
BEGIN
    SET NEW.passed=(NEW.connections_after<=NEW.connections_before AND NEW.abandoned_connections=0 AND NEW.open_transactions=0);
END$$

CREATE TRIGGER trg_restore_drill_score_insert
BEFORE INSERT ON aoo_restore_drill
FOR EACH ROW
BEGIN
    SET NEW.passed=(
        NEW.completed_at>=NEW.started_at
        AND NEW.table_count_expected=NEW.table_count_actual
        AND NEW.asset_hash_expected=NEW.asset_hash_actual
        AND NEW.room_history_hash_expected=NEW.room_history_hash_actual
        AND NEW.relationship_mismatch_count=0
    );
END$$

CREATE TRIGGER trg_failover_drill_score_insert
BEFORE INSERT ON aoo_replica_failover_drill
FOR EACH ROW
BEGIN
    SET NEW.passed=(
        NEW.completed_at>=NEW.started_at
        AND NEW.expected_gtid_set=NEW.promoted_gtid_set
        AND NEW.read_only_guard_verified=1
        AND NEW.connection_reroute_verified=1
        AND NEW.identity_generation_verified=1
        AND NEW.writes_fenced_verified=1
        AND NEW.data_loss_rows=0
    );
END$$

CREATE TRIGGER trg_archive_success_guard
BEFORE UPDATE ON aoo_archive_run
FOR EACH ROW
BEGIN
    IF NEW.status='SUCCEEDED' AND (
        NEW.completed_at IS NULL
        OR NEW.source_count<>NEW.archive_count
        OR NEW.source_hash<>NEW.archive_hash
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='archive run cannot succeed before count/hash reconciliation';
    END IF;
END$$
DELIMITER ;
