-- v2.64 canonical, recoverable dissolve authority.
--
-- This migration is additive and deliberately does not alter historical
-- migrations or backfill old process-local/PDK dissolve state.  A dissolve
-- generation is a durable aggregate keyed by the room lifecycle generation.
-- The runtime identity row is locked by the JDBC authority before an
-- aggregate/receipt mutation and its state_version is advanced in the same
-- transaction.  DESTROYED runtime tombstones remain authoritative.
--
-- Manual rollback record (not executed here): only an isolated disposable
-- database may drop these two tables in reverse dependency order.  Once any
-- production row exists, rollback is forbidden; terminal rows are retained.

CREATE TABLE aoo_room_dissolve_authority (
    room_id BIGINT UNSIGNED NOT NULL,
    room_instance_id VARCHAR(128) NOT NULL,
    lifecycle_epoch BIGINT UNSIGNED NOT NULL,
    fencing_token BIGINT UNSIGNED NOT NULL,
    state_version BIGINT UNSIGNED NOT NULL,
    expected_state_version BIGINT UNSIGNED NOT NULL,
    vote_id VARCHAR(128) NOT NULL,
    applicant_account_id BIGINT UNSIGNED NOT NULL,
    eligible_member_ids JSON NOT NULL,
    deadline_at DATETIME(3) NOT NULL,
    approve_on_timeout BOOLEAN NOT NULL,
    votes JSON NOT NULL,
    decision VARCHAR(16) NOT NULL,
    terminal_tombstone BOOLEAN NOT NULL,
    terminal_at DATETIME(3) NULL,
    active_slot TINYINT GENERATED ALWAYS AS (
        CASE WHEN terminal_tombstone = 0 AND decision = 'VOTING' THEN 1 ELSE NULL END
    ) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id, lifecycle_epoch, vote_id),
    UNIQUE KEY uk_room_dissolve_active_generation (room_id, lifecycle_epoch, active_slot),
    KEY idx_room_dissolve_runtime (room_id, room_instance_id, lifecycle_epoch, fencing_token),
    CONSTRAINT chk_room_dissolve_ids CHECK (
        room_id > 0 AND lifecycle_epoch > 0 AND fencing_token > 0
        AND state_version >= expected_state_version
        AND CHAR_LENGTH(TRIM(room_instance_id)) > 0
        AND CHAR_LENGTH(TRIM(vote_id)) > 0
        AND applicant_account_id > 0
    ),
    CONSTRAINT chk_room_dissolve_json CHECK (
        JSON_TYPE(eligible_member_ids) = 'ARRAY'
        AND JSON_TYPE(votes) = 'OBJECT'
    ),
    CONSTRAINT chk_room_dissolve_decision CHECK (
        decision IN ('VOTING', 'APPROVED', 'REJECTED')
        AND (
            (terminal_tombstone = 0 AND decision = 'VOTING' AND terminal_at IS NULL)
            OR (terminal_tombstone = 1 AND decision IN ('APPROVED', 'REJECTED')
                AND terminal_at IS NOT NULL)
        )
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_room_dissolve_command_receipt (
    room_id BIGINT UNSIGNED NOT NULL,
    lifecycle_epoch BIGINT UNSIGNED NOT NULL,
    command_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    operation VARCHAR(16) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    result_status VARCHAR(16) NOT NULL,
    vote_id VARCHAR(128) NOT NULL,
    applicant_account_id BIGINT UNSIGNED NOT NULL,
    eligible_member_ids JSON NOT NULL,
    deadline_at DATETIME(3) NOT NULL,
    approve_on_timeout BOOLEAN NOT NULL,
    votes JSON NOT NULL,
    decision VARCHAR(16) NOT NULL,
    result_state_version BIGINT UNSIGNED NOT NULL,
    terminal_tombstone BOOLEAN NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id, lifecycle_epoch, command_id),
    UNIQUE KEY uk_room_dissolve_receipt_idempotency (
        room_id, lifecycle_epoch, idempotency_key
    ),
    KEY idx_room_dissolve_receipt_vote (
        room_id, lifecycle_epoch, vote_id, created_at
    ),
    CONSTRAINT chk_room_dissolve_receipt_hash CHECK (
        request_hash REGEXP '^[0-9a-f]{64}$'
        AND CHAR_LENGTH(TRIM(command_id)) > 0
        AND CHAR_LENGTH(TRIM(idempotency_key)) > 0
        AND CHAR_LENGTH(TRIM(vote_id)) > 0
    ),
    CONSTRAINT chk_room_dissolve_receipt_operation CHECK (
        operation IN ('APPLY', 'VOTE')
    ),
    CONSTRAINT chk_room_dissolve_receipt_status CHECK (
        result_status IN ('APPLIED', 'VOTED')
    ),
    CONSTRAINT chk_room_dissolve_receipt_json CHECK (
        JSON_TYPE(eligible_member_ids) = 'ARRAY'
        AND JSON_TYPE(votes) = 'OBJECT'
    ),
    CONSTRAINT chk_room_dissolve_receipt_decision CHECK (
        decision IN ('VOTING', 'APPROVED', 'REJECTED')
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
