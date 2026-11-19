-- B0 scoped empty-database final schema. No migration-history claim.
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+00:00';

CREATE TABLE `aoo_account` (
    account_id BIGINT NOT NULL AUTO_INCREMENT,
    password_hash VARCHAR(255) NULL,
    guest_credential_hash CHAR(64) NULL,
    recovery_credential_hash CHAR(64) NULL,
    guest BOOLEAN NOT NULL DEFAULT TRUE,
    auth_generation BIGINT NOT NULL DEFAULT 0,
    banned_until TIMESTAMP(3) NULL,
    ban_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_aoo_account_guest (guest_credential_hash),
    UNIQUE KEY uk_aoo_account_recovery (recovery_credential_hash)
) ;

CREATE TABLE `aoo_card_category` (
    category_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (category_code),
    CONSTRAINT chk_card_category_code CHECK (category_code REGEXP '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT chk_card_category_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_club_member` (
    club_id BIGINT UNSIGNED NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    member_status VARCHAR(16) NOT NULL,
    member_role VARCHAR(16) NOT NULL,
    online TINYINT NOT NULL DEFAULT 0,
    profile_payload JSON NOT NULL,
    joined_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (club_id, player_id),
    KEY idx_club_member_page (club_id, member_status, player_id),
    KEY idx_club_online_page (club_id, online, player_id),
    KEY idx_club_manager_online (club_id, member_role, online, player_id),
    KEY idx_player_clubs (player_id, member_status, club_id),
    UNIQUE KEY uk_club_member_business (club_id, player_id),
    CONSTRAINT chk_club_member_identity CHECK (club_id > 0 AND player_id > 0),
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    CONSTRAINT chk_club_member_status CHECK (member_status IN ('ACTIVE','SUSPENDED','LEFT') AND online IN (0,1)),
    CONSTRAINT chk_club_member_revision CHECK (row_version > 0)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_club_room_projection` (
    club_id BIGINT UNSIGNED NOT NULL,
    room_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    room_status VARCHAR(16) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (club_id, room_id),
    KEY ix_club_room_display (club_id, room_status, created_at),
    CONSTRAINT chk_club_room_status CHECK (room_status IN ('OPEN','PLAYING','FINISHED','DISSOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `aoo_club_write_idempotency` (
    scope_key VARCHAR(160),
    response_json TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `aoo_connection_generation` (
    user_id VARCHAR(128) NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    seat_id INT NOT NULL,
    generation BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, room_id, seat_id),
    CONSTRAINT chk_connection_generation_positive CHECK (generation > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_currency_catalog` (
    currency_code VARCHAR(32),
    display_name VARCHAR(64) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    scale_digits TINYINT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_currency_catalog_scope CHECK (scope_type IN ('GLOBAL','CITY','CLUB','UNION')),
    CONSTRAINT chk_currency_catalog_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT chk_currency_catalog_scale CHECK (scale_digits <= 6),
    PRIMARY KEY (currency_code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_display_id_segment` (
    digit_length INT NOT NULL,
    range_start DECIMAL(65,0) NOT NULL,
    range_end DECIMAL(65,0) NOT NULL,
    capacity DECIMAL(65,0) NOT NULL,
    occupied_count DECIMAL(65,0) NOT NULL DEFAULT 0,
    allocation_threshold_bps INT NOT NULL DEFAULT 8500,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (digit_length),
    CONSTRAINT ck_display_segment_digits CHECK (digit_length >= 6),
    CONSTRAINT ck_display_segment_threshold CHECK (allocation_threshold_bps BETWEEN 1 AND 10000)
) ;

CREATE TABLE `aoo_hall_idempotency` (
    account_id BIGINT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    operation VARCHAR(16) NOT NULL,
    response_json JSON NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY(account_id,idempotency_key),
    KEY idx_hall_idempotency_retention(created_at),
    CONSTRAINT chk_hall_idempotency_operation CHECK(operation IN('CREATE','JOIN','LEAVE','TICKET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_region` (
    region_code VARCHAR(32) NOT NULL,
    parent_region_code VARCHAR(32) NULL,
    region_type VARCHAR(16) NOT NULL,
    country_code CHAR(2) NULL,
    subdivision_code VARCHAR(16) NULL,
    display_name VARCHAR(64) NOT NULL,
    path VARCHAR(256) NOT NULL,
    depth TINYINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (region_code),
    UNIQUE KEY uk_region_path (path),
    KEY idx_region_parent (parent_region_code,status,region_code),
    KEY idx_region_country_subdivision (country_code,subdivision_code,status),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_region_code CHECK (region_code REGEXP '^[A-Z0-9][A-Z0-9-]{0,31}$'),
    CONSTRAINT chk_region_type CHECK (region_type IN ('GLOBAL','NONE','COUNTRY','PROVINCE','CITY','DISTRICT')),
    CONSTRAINT chk_region_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT chk_region_depth CHECK (depth <= 4),
    CONSTRAINT chk_region_parent_shape CHECK (
        (region_type IN ('GLOBAL','NONE','COUNTRY') AND parent_region_code IS NULL)
        OR (region_type IN ('PROVINCE','CITY','DISTRICT') AND parent_region_code IS NOT NULL)
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_authority_route` (
    room_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    node_id VARCHAR(128) NOT NULL,
    authority_endpoint VARCHAR(512) NOT NULL,
    fencing_token BIGINT UNSIGNED NOT NULL,
    lifecycle_state VARCHAR(24) NOT NULL,
    last_request_id VARCHAR(128) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    lease_expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    first_round_started_at TIMESTAMP(3) NULL,
    last_business_activity_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(room_id),
    UNIQUE KEY uk_authority_create_request(last_request_id),
    KEY idx_authority_node_state(node_id,lifecycle_state,lease_expires_at),
    CONSTRAINT chk_authority_fencing CHECK(fencing_token > 0),
    CONSTRAINT chk_authority_state CHECK(lifecycle_state IN('CREATING','ACTIVE','RECOVERING','REMOVING','REMOVED','FAILED')),
    KEY idx_room_authority_waiting_expiry(lifecycle_state,first_round_started_at,created_at,room_id),
    KEY idx_room_authority_inactivity_expiry(
    lifecycle_state,first_round_started_at,last_business_activity_at,room_id
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_create_saga` (
    request_id VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    request_json JSON NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    state_version BIGINT UNSIGNED NOT NULL,
    step VARCHAR(32) NOT NULL,
    compensation_origin_step VARCHAR(32) NULL,
    response_json JSON NULL,
    failure_code VARCHAR(128) NULL,
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NULL,
    trace_id VARCHAR(64) NOT NULL DEFAULT 'migration',
    processing_token VARCHAR(64) NULL,
    processing_until DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY(request_id),
    UNIQUE KEY uk_room_create_saga_room(room_id),
    KEY idx_room_saga_recovery(step,next_retry_at,updated_at),
    KEY idx_room_saga_processing(processing_until,step,updated_at),
    KEY idx_room_saga_compensation(compensation_origin_step,step,next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_idempotent_allocation` (
    request_id VARCHAR(128) NOT NULL,
    account_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (request_id),
    UNIQUE KEY uk_room_idempotent_allocation_room (room_id),
    KEY idx_room_idempotent_allocation_account (account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_runtime_identity_authority` (
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

CREATE TABLE `aoo_settlement_outbox` (
    business_id VARCHAR(128) NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    round_no INT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    result_payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_settlement_outbox_retry(status,next_attempt_at,created_at),
    CONSTRAINT chk_settlement_outbox_status CHECK(status='PENDING'),
    PRIMARY KEY (business_id)
) ENGINE=InnoDB;

CREATE TABLE `media_asset` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(512) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    sha256 CHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_asset_hash (sha256, byte_size, mime_type),
    UNIQUE KEY uk_media_asset_object_key (object_key),
    CONSTRAINT ck_media_asset_size CHECK (byte_size > 0),
    CONSTRAINT ck_media_asset_duration CHECK (duration_ms >= 0),
    CONSTRAINT ck_media_asset_state CHECK (state IN ('READY','DELETING','DELETED')),
    CONSTRAINT ck_media_asset_kind CHECK (kind IN ('AVATAR','VOICE','EVIDENCE'))
) ENGINE=InnoDB;

CREATE TABLE `aoo_account_audit` (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    detail_value VARCHAR(255) NOT NULL,
    source_ip VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_account_audit_owner_time (account_id, occurred_at),
    CONSTRAINT fk_account_audit_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
) ;

CREATE TABLE `aoo_account_device` (
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    channel_name VARCHAR(64) NOT NULL,
    client_version VARCHAR(64) NOT NULL,
    first_seen_at TIMESTAMP(3) NOT NULL,
    last_seen_at TIMESTAMP(3) NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    PRIMARY KEY (account_id, device_id),
    CONSTRAINT fk_account_device_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
) ;

CREATE TABLE `aoo_account_identity` (
    identity_id BIGINT NOT NULL AUTO_INCREMENT,
    identity_type VARCHAR(24) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    value_hash CHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    active_value_hash CHAR(64) GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN value_hash ELSE NULL END) STORED,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY(identity_id),
    UNIQUE KEY uk_account_identity_active(identity_type,active_value_hash),
    KEY idx_account_identity_owner(account_id,status,identity_type),
    CONSTRAINT fk_account_identity_owner FOREIGN KEY(account_id) REFERENCES aoo_account(account_id),
    CONSTRAINT ck_account_identity_type CHECK(identity_type IN ('DISPLAY_ID','ALIAS','PHONE','WECHAT_UNION','WECHAT_OPEN','TRUSTED_DEVICE')),
    CONSTRAINT ck_account_identity_status CHECK(status IN ('ACTIVE','DISABLED','MIGRATED'))
) ;

CREATE TABLE `aoo_account_session` (
    session_id CHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    token_family CHAR(36) NOT NULL,
    access_hash CHAR(64) NOT NULL,
    refresh_hash CHAR(64) NOT NULL,
    access_expires_at TIMESTAMP(3) NOT NULL,
    refresh_expires_at TIMESTAMP(3) NOT NULL,
    auth_generation BIGINT NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    revoke_reason VARCHAR(64) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (session_id),
    UNIQUE KEY uk_account_access_hash (access_hash),
    UNIQUE KEY uk_account_refresh_hash (refresh_hash),
    KEY idx_account_session_owner (account_id, revoked_at),
    CONSTRAINT fk_account_session_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id)
) ;

CREATE TABLE `aoo_currency_balance` (
    player_id BIGINT UNSIGNED NOT NULL,
    currency VARCHAR(32) NOT NULL,
    currency_scope_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_currency_balance_non_negative CHECK (balance >= 0),
    PRIMARY KEY (player_id,currency,currency_scope_id),
    KEY idx_currency_balance_catalog (currency,player_id,currency_scope_id),
    CONSTRAINT fk_currency_balance_catalog FOREIGN KEY (currency)
        REFERENCES aoo_currency_catalog(currency_code),
    CONSTRAINT chk_currency_balance_scope CHECK (
        (currency IN ('ROOM_CARD','GOLD','CRYSTAL') AND currency_scope_id=0)
        OR (currency IN ('CITY_ROOM_CARD','SPORTS_POINT') AND currency_scope_id>0)
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_display_id_history` (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    old_display_id VARCHAR(65) NULL,
    new_display_id VARCHAR(65) NOT NULL,
    valid_from TIMESTAMP(3) NOT NULL,
    valid_until TIMESTAMP(3) NULL,
    operator_id BIGINT NULL,
    reason VARCHAR(255) NOT NULL,
    source_ip VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    PRIMARY KEY(history_id),
    KEY idx_display_history_value_time(new_display_id,valid_from,valid_until),
    KEY idx_display_history_owner_time(account_id,valid_from),
    CONSTRAINT fk_display_history_owner FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
) ;

CREATE TABLE `aoo_game_family` (
    family_code VARCHAR(64) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (family_code),
    KEY idx_game_family_category (category_code,status,family_code),
    CONSTRAINT fk_game_family_category FOREIGN KEY (category_code)
        REFERENCES aoo_card_category(category_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_family_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT chk_game_family_code
    CHECK (family_code REGEXP '^[A-Za-z][A-Za-z0-9:_-]{0,63}$')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `player_profile` (
    player_id BIGINT NOT NULL,
    nickname VARCHAR(96) NOT NULL,
    avatar_asset_id BIGINT NULL,
    gender_code VARCHAR(16) NOT NULL DEFAULT 'UNSPECIFIED',
    language_code VARCHAR(35) NOT NULL DEFAULT 'en',
    sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    music_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vibration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    show_gender BOOLEAN NOT NULL DEFAULT FALSE,
    nickname_changed_at TIMESTAMP(3) NULL,
    avatar_changed_at TIMESTAMP(3) NULL,
    profile_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (player_id),
    KEY idx_player_profile_nickname (nickname),
    KEY idx_player_profile_avatar (avatar_asset_id),
    CONSTRAINT fk_player_profile_account FOREIGN KEY (player_id) REFERENCES aoo_account(account_id),
    CONSTRAINT fk_player_profile_avatar FOREIGN KEY (avatar_asset_id) REFERENCES media_asset(id),
    CONSTRAINT ck_player_profile_version CHECK (profile_version >= 0),
    CONSTRAINT ck_player_profile_gender CHECK (gender_code IN ('UNSPECIFIED','FEMALE','MALE','NON_BINARY'))
) ;

CREATE TABLE `aoo_game_catalog` (
    game_id BIGINT UNSIGNED NOT NULL,
    game_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    family_code VARCHAR(64) NOT NULL,
    provider_key VARCHAR(192) NOT NULL,
    catalog_schema_version INT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id),
    UNIQUE KEY uk_game_catalog_code (game_code),
    UNIQUE KEY uk_game_catalog_provider (provider_key),
    KEY idx_game_catalog_classification (category_code,family_code,status,game_id),
    CONSTRAINT fk_game_catalog_category FOREIGN KEY (category_code)
        REFERENCES aoo_card_category(category_code) ON DELETE RESTRICT,
    CONSTRAINT fk_game_catalog_family FOREIGN KEY (family_code)
        REFERENCES aoo_game_family(family_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_catalog_id CHECK (game_id > 0),
    CONSTRAINT chk_game_catalog_status CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','RETIRED')),
    CONSTRAINT chk_game_catalog_schema_version CHECK (catalog_schema_version > 0),
    CONSTRAINT chk_game_catalog_code CHECK (
    REGEXP_LIKE(game_code,'^[a-z][a-z0-9_]{0,63}$')
    OR REGEXP_LIKE(game_code,'^[A-Z]{2}[1-5][0-9]{2}$')
)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `gateway_ws_ticket` (
    ticket_hash CHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    session_id CHAR(36) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    allowed_origin VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    issued_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (ticket_hash),
    UNIQUE KEY uk_gateway_ws_ticket_request (account_id, idempotency_key),
    KEY idx_gateway_ws_ticket_expiry (expires_at),
    CONSTRAINT fk_gateway_ws_ticket_account FOREIGN KEY (account_id) REFERENCES aoo_account(account_id),
    CONSTRAINT fk_gateway_ws_ticket_session FOREIGN KEY (session_id) REFERENCES aoo_account_session(session_id)
) ;

CREATE TABLE `aoo_game_region` (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    availability VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    available_from DATETIME(3) NULL,
    available_until DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,region_code),
    KEY idx_game_region_lookup (region_code,availability,priority,game_id),
    CONSTRAINT fk_game_region_game FOREIGN KEY (game_id)
        REFERENCES aoo_game_catalog(game_id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_region_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_region_availability CHECK (availability IN ('AVAILABLE','HIDDEN','BLOCKED','RETIRED')),
    CONSTRAINT chk_game_region_window CHECK (available_until IS NULL OR available_from IS NULL OR available_until > available_from)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_play_version` (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    default_region_code VARCHAR(32) NOT NULL,
    rule_schema_version INT UNSIGNED NOT NULL,
    ui_schema_version INT UNSIGNED NOT NULL,
    component_schema_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    compatibility_floor VARCHAR(64) NULL,
    activated_at DATETIME(3) NULL,
    retired_at DATETIME(3) NULL,
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,play_version),
    UNIQUE KEY uk_play_version_content (game_id,content_hash),
    KEY idx_play_version_status (status,game_id,activated_at),
    KEY idx_play_version_region (default_region_code,status,game_id),
    CONSTRAINT fk_play_version_game FOREIGN KEY (game_id)
        REFERENCES aoo_game_catalog(game_id) ON DELETE RESTRICT,
    CONSTRAINT fk_play_version_region FOREIGN KEY (default_region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_play_version_name CHECK (play_version REGEXP '^[a-z0-9][a-z0-9._-]{0,63}$'),
    CONSTRAINT chk_play_version_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_play_version_status CHECK (status IN ('DRAFT','VALIDATED','ACTIVE','RETIRED')),
    CONSTRAINT chk_play_version_schemas CHECK (rule_schema_version > 0 AND ui_schema_version > 0 AND component_schema_version > 0),
    CONSTRAINT chk_play_version_lifecycle CHECK (retired_at IS NULL OR activated_at IS NOT NULL)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_game_release` (
    release_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    snapshot_schema_version VARCHAR(64) NULL,
    card_codec_version VARCHAR(64) NULL,
    event_interpreter_version VARCHAR(64) NULL,
    protocol_version VARCHAR(64) NULL,
    source_hash CHAR(64) NULL,
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
    KEY ix_game_release_bundle (game_id,bundle_hash),
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
    ),
    UNIQUE KEY uk_game_release_identity (release_id,game_id,play_version),
    CONSTRAINT chk_game_release_snapshot_schema_version CHECK (
        snapshot_schema_version IS NULL
        OR snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    ),
    CONSTRAINT chk_game_release_runtime_metadata CHECK (
        (snapshot_schema_version IS NULL
            OR snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (card_codec_version IS NULL
            OR card_codec_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (event_interpreter_version IS NULL
            OR event_interpreter_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (protocol_version IS NULL
            OR protocol_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (source_hash IS NULL OR source_hash REGEXP '^[0-9a-f]{64}$')
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_game_service_route` (
    route_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(route_id),
    UNIQUE KEY uk_game_route_endpoint(game_id,play_version,endpoint),
    KEY idx_game_route_lookup(game_id,play_version,status,priority,route_id),
    CONSTRAINT fk_game_route_play FOREIGN KEY(game_id,play_version) REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT chk_game_route_status CHECK(status IN('ACTIVE','DRAINING','OFFLINE')),
    CONSTRAINT chk_game_route_endpoint CHECK(endpoint REGEXP '^wss?://')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_game_release_region` (
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

CREATE TABLE `aoo_published_game_configuration` (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT UNSIGNED NULL,
    configuration_payload JSON NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,play_version),
    CONSTRAINT fk_published_configuration_play FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    KEY idx_published_configuration_release (release_id,game_id,play_version),
    CONSTRAINT fk_published_configuration_release_identity
        FOREIGN KEY (release_id,game_id,play_version)
        REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_snapshot` (
    room_id BIGINT UNSIGNED NOT NULL,
    game_id INT NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT UNSIGNED NULL,
    component_version VARCHAR(64) NOT NULL,
    rule_content_hash CHAR(64) NULL,
    component_content_hash CHAR(64) NULL,
    card_codec_version VARCHAR(64) NULL,
    event_interpreter_version VARCHAR(64) NULL,
    protocol_version VARCHAR(64) NULL,
    fencing_token BIGINT NOT NULL,
    state_version BIGINT NOT NULL DEFAULT 0,
    last_event_sequence BIGINT NOT NULL,
    captured_at DATETIME(3) NOT NULL,
    state_payload JSON NOT NULL,
    KEY idx_room_snapshot_capture (captured_at),
    PRIMARY KEY (room_id),
    KEY idx_snapshot_state_event_version (room_id,state_version,last_event_sequence),
    CONSTRAINT chk_snapshot_identity CHECK (room_id > 0 AND fencing_token > 0 AND state_version >= 0 AND last_event_sequence = state_version),
    KEY idx_snapshot_recoverable (captured_at, room_id),
    KEY idx_room_snapshot_release (release_id,room_id),
    CONSTRAINT fk_room_snapshot_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_compiled_room_create_index` (
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
    CONSTRAINT chk_compiled_index_state CHECK (lifecycle_state IN ('STAGED','READY','ACTIVE','RETIRED','FAILED')),
    UNIQUE KEY uk_compiled_index_release_identity
        (game_id,region_code,play_version,index_generation,release_id),
    CONSTRAINT fk_compiled_index_release_identity
        FOREIGN KEY (release_id,game_id,play_version)
        REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_compiled_index_active` (
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
    CONSTRAINT fk_compiled_active_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_compiled_active_epoch CHECK (cache_epoch > 0),
    CONSTRAINT fk_compiled_active_index_release
        FOREIGN KEY (game_id,region_code,play_version,index_generation,release_id)
        REFERENCES aoo_compiled_room_create_index
            (game_id,region_code,play_version,index_generation,release_id) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_hall_room` (
    room_id BIGINT UNSIGNED NOT NULL,
    game_id BIGINT UNSIGNED NOT NULL,
    classification_region_code VARCHAR(32) NOT NULL COMMENT 'Gameplay classification provenance; server-derived; never an account, asset, club, room-number or service-route partition key',
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    owner_account_id BIGINT UNSIGNED NOT NULL,
    scope_type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL',
    club_id BIGINT UNSIGNED NULL,
    club_template_code VARCHAR(64) NULL,
    state VARCHAR(16) NOT NULL,
    route_endpoint VARCHAR(512) NOT NULL,
    rules_json JSON NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY(room_id),
    KEY idx_hall_room_owner(owner_account_id,state,updated_at,room_id),
    KEY idx_hall_room_route(game_id,play_version,state,room_id),
    CONSTRAINT fk_hall_room_index FOREIGN KEY(game_id,classification_region_code,play_version,index_generation) REFERENCES aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation) ON DELETE RESTRICT,
    CONSTRAINT fk_hall_room_release FOREIGN KEY(release_id) REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    KEY idx_hall_room_club (club_id, state, updated_at, room_id),
    CONSTRAINT chk_hall_room_scope CHECK (
        (scope_type = 'PERSONAL' AND club_id IS NULL AND club_template_code IS NULL)
        OR (scope_type = 'CLUB' AND club_id IS NOT NULL AND club_id > 0 AND club_template_code IS NOT NULL)
    ),
    CONSTRAINT chk_hall_room_state
        CHECK (state IN ('CREATING', 'OPEN', 'PLAYING', 'FINISHED', 'DISSOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_room_rule_lock` (
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
    source_hash CHAR(64) NULL,
    snapshot_schema_version VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id),
    KEY idx_room_rule_lock_release (release_id,room_id),
    KEY idx_room_rule_lock_history (game_id,play_version,created_at,room_id),
    CONSTRAINT fk_room_rule_lock_release FOREIGN KEY (release_id)
        REFERENCES aoo_game_release(release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rule_lock_hashes CHECK (
        bundle_hash REGEXP '^[0-9a-f]{64}$'
        AND rule_hash REGEXP '^[0-9a-f]{64}$'
        AND component_hash REGEXP '^[0-9a-f]{64}$'
    ),
    CONSTRAINT fk_room_rule_lock_index_release
        FOREIGN KEY (game_id,region_code,play_version,index_generation,release_id)
        REFERENCES aoo_compiled_room_create_index
            (game_id,region_code,play_version,index_generation,release_id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rule_lock_runtime_metadata CHECK (
        (source_hash IS NULL AND snapshot_schema_version IS NULL)
        OR (
            source_hash REGEXP '^[0-9a-f]{64}$'
            AND snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
        )
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_hall_room_member` (
    room_id BIGINT UNSIGNED NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    seat_no SMALLINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL,
    joined_at DATETIME(3) NOT NULL,
    left_at DATETIME(3) NULL,
    PRIMARY KEY(room_id,account_id),
    UNIQUE KEY uk_hall_room_seat(room_id,seat_no),
    KEY idx_hall_member_account(account_id,status,room_id),
    CONSTRAINT fk_hall_member_room FOREIGN KEY(room_id) REFERENCES aoo_hall_room(room_id) ON DELETE RESTRICT,
    CONSTRAINT chk_hall_member_status CHECK(status IN('JOINING','JOINED','LEFT','KICKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aoo_hall_game_ticket` (
    ticket_hash CHAR(64) NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    device_fingerprint VARCHAR(128) NOT NULL,
    route_endpoint VARCHAR(512) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    consumed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY(ticket_hash),
    KEY idx_hall_ticket_expiry(expires_at,consumed_at),
    KEY idx_hall_ticket_subject(room_id,account_id,expires_at),
    CONSTRAINT fk_hall_ticket_member FOREIGN KEY(room_id,account_id) REFERENCES aoo_hall_room_member(room_id,account_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DELIMITER $$
CREATE TRIGGER trg_aoo_account_default_crystal
AFTER INSERT ON aoo_account
FOR EACH ROW
INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at)
VALUES(NEW.account_id,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3))$$
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
CREATE TRIGGER trg_game_catalog_row_version
BEFORE UPDATE ON aoo_game_catalog
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
CREATE TRIGGER trg_pdk_business_code_insert BEFORE INSERT ON aoo_game_catalog
FOR EACH ROW
BEGIN
    IF (NEW.game_id=8 AND NEW.game_code<>'CD201')
       OR (NEW.game_id=629 AND NEW.game_code<>'NJ201')
       OR (NEW.game_id=90005 AND NEW.game_code<>'LS201') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PDK catalog rows require CD201/NJ201/LS201';
    END IF;
END$$
CREATE TRIGGER trg_pdk_business_code_update BEFORE UPDATE ON aoo_game_catalog
FOR EACH ROW
BEGIN
    IF (NEW.game_id=8 AND NEW.game_code<>'CD201')
       OR (NEW.game_id=629 AND NEW.game_code<>'NJ201')
       OR (NEW.game_id=90005 AND NEW.game_code<>'LS201') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PDK catalog rows require CD201/NJ201/LS201';
    END IF;
END$$
CREATE TRIGGER trg_play_version_row_version
BEFORE UPDATE ON aoo_play_version
FOR EACH ROW
BEGIN
    SET NEW.row_version=OLD.row_version+1;
END$$
CREATE TRIGGER trg_published_configuration_release_required
BEFORE INSERT ON aoo_published_game_configuration
FOR EACH ROW
BEGIN
    DECLARE v_release_status VARCHAR(16) DEFAULT NULL;
    IF NEW.release_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published configuration requires an immutable release';
    END IF;
    SELECT MAX(status) INTO v_release_status
      FROM aoo_game_release
     WHERE release_id=NEW.release_id
       AND game_id=NEW.game_id
       AND play_version=NEW.play_version;
    IF v_release_status IS NULL OR v_release_status NOT IN ('VALIDATED','ACTIVE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='configuration release is not publishable';
    END IF;
END$$
CREATE TRIGGER trg_region_row_version
BEFORE UPDATE ON aoo_region
FOR EACH ROW
BEGIN
    SET NEW.row_version=OLD.row_version+1;
END$$
CREATE TRIGGER trg_room_rule_lock_no_delete BEFORE DELETE ON aoo_room_rule_lock
FOR EACH ROW
BEGIN
    IF COALESCE(@aoo_authorized_room_cleanup,0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room rule lock is retention controlled';
    END IF;
END$$
CREATE TRIGGER trg_room_rule_lock_no_update
BEFORE UPDATE ON aoo_room_rule_lock
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room rule lock is immutable';
END$$
DELIMITER ;
