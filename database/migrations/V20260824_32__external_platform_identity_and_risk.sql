CREATE TABLE IF NOT EXISTS aoo_external_identity (
    account_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(191) NOT NULL,
    union_subject VARCHAR(191) NULL,
    credential_version BIGINT NOT NULL DEFAULT 1,
    bound_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id, provider),
    UNIQUE KEY uk_external_identity_subject (provider, provider_subject),
    CONSTRAINT ck_external_identity_version CHECK (credential_version > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_external_callback_receipt (
    provider VARCHAR(32) NOT NULL,
    event_id VARCHAR(191) NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    received_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at TIMESTAMP(3) NULL,
    result_code VARCHAR(64) NULL,
    PRIMARY KEY (provider, event_id),
    CONSTRAINT ck_callback_payload_sha256 CHECK (payload_sha256 REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aoo_player_risk_observation (
    observation_id VARCHAR(64) NOT NULL,
    player_id BIGINT NOT NULL,
    room_id BIGINT NULL,
    signal_type VARCHAR(32) NOT NULL,
    authorization_state VARCHAR(32) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    accuracy_meters DECIMAL(10,2) NULL,
    ip_address VARBINARY(16) NULL,
    device_hash CHAR(64) NULL,
    observed_at TIMESTAMP(3) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (observation_id),
    KEY idx_player_risk_room_time (room_id, observed_at),
    KEY idx_player_risk_player_time (player_id, observed_at),
    KEY idx_player_risk_device_time (device_hash, observed_at),
    CONSTRAINT ck_player_risk_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    ),
    CONSTRAINT ck_player_risk_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0),
    CONSTRAINT ck_player_risk_device_hash CHECK (device_hash IS NULL OR device_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB;
