CREATE TABLE media_asset (
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

CREATE TABLE media_asset_access (
    asset_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    granted_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (asset_id, owner_id),
    KEY ix_media_asset_access_owner (owner_id, asset_id),
    CONSTRAINT fk_media_asset_access_asset FOREIGN KEY (asset_id) REFERENCES media_asset(id)
) ENGINE=InnoDB;

CREATE TABLE media_upload_ticket (
    id CHAR(36) NOT NULL,
    owner_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    storage_upload_id VARCHAR(1024) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    sha256 CHAR(64) NOT NULL,
    chunk_bytes INT NOT NULL,
    expected_parts INT NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    state VARCHAR(16) NOT NULL,
    asset_id BIGINT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_media_ticket_expiry (state, expires_at),
    KEY ix_media_ticket_owner (owner_id, created_at),
    CONSTRAINT fk_media_ticket_asset FOREIGN KEY (asset_id) REFERENCES media_asset(id),
    CONSTRAINT ck_media_ticket_size CHECK (byte_size > 0 AND chunk_bytes > 0 AND expected_parts > 0),
    CONSTRAINT ck_media_ticket_state CHECK (state IN ('OPEN','COMPLETING','READY','FAILED','EXPIRED')),
    CONSTRAINT ck_media_ticket_kind CHECK (kind IN ('AVATAR','VOICE','EVIDENCE'))
) ENGINE=InnoDB;

CREATE TABLE media_upload_part (
    ticket_id CHAR(36) NOT NULL,
    part_number INT NOT NULL,
    byte_size BIGINT NOT NULL,
    etag VARCHAR(255) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (ticket_id, part_number),
    CONSTRAINT fk_media_part_ticket FOREIGN KEY (ticket_id) REFERENCES media_upload_ticket(id) ON DELETE CASCADE,
    CONSTRAINT ck_media_part_number CHECK (part_number > 0),
    CONSTRAINT ck_media_part_size CHECK (byte_size > 0)
) ENGINE=InnoDB;
