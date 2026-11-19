CREATE TABLE aoo_room_runtime_identity_authority (
    room_id BIGINT UNSIGNED NOT NULL,
    room_instance_id VARCHAR(128) NOT NULL,
    lifecycle_epoch BIGINT UNSIGNED NOT NULL,
    fencing_token BIGINT UNSIGNED NOT NULL,
    state_version BIGINT UNSIGNED NOT NULL,
    lifecycle_state VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id),
    UNIQUE KEY uk_room_runtime_identity_generation (room_instance_id, lifecycle_epoch, fencing_token),
    CONSTRAINT chk_room_runtime_identity_room CHECK (room_id > 0),
    CONSTRAINT chk_room_runtime_identity_instance CHECK (CHAR_LENGTH(TRIM(room_instance_id)) > 0),
    CONSTRAINT chk_room_runtime_identity_epoch CHECK (lifecycle_epoch > 0),
    CONSTRAINT chk_room_runtime_identity_fence CHECK (fencing_token > 0),
    CONSTRAINT chk_room_runtime_identity_state CHECK (lifecycle_state IN ('ACTIVE', 'DESTROYED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Rows are never deleted: DESTROYED is the tombstone which prevents a stale
-- lifecycle generation from being mounted again for the same room id.
