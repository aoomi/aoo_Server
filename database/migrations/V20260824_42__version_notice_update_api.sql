CREATE TABLE aoo_client_release (
 id BIGINT NOT NULL PRIMARY KEY, platform VARCHAR(32) NOT NULL, channel VARCHAR(64) NOT NULL,
 version VARCHAR(32) NOT NULL, version_code BIGINT NOT NULL, min_supported_version VARCHAR(32) NOT NULL,
 force_update BOOLEAN NOT NULL DEFAULT FALSE, rollout_percent SMALLINT NOT NULL DEFAULT 100,
 rollback_version VARCHAR(32) NULL, download_url VARCHAR(1024) NOT NULL, state VARCHAR(24) NOT NULL,
 published_at TIMESTAMP(3) NOT NULL, created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT uq_client_release UNIQUE(platform,channel,version),
 CONSTRAINT ck_release_rollout CHECK(rollout_percent BETWEEN 0 AND 100),
 CONSTRAINT ck_release_state CHECK(state IN ('DRAFT','ACTIVE','ROLLED_BACK','RETIRED')),
 INDEX ix_release_resolution(platform,channel,state,published_at,version_code)
);
CREATE TABLE aoo_resource_manifest (
 id BIGINT NOT NULL PRIMARY KEY, release_id BIGINT NOT NULL, version VARCHAR(32) NOT NULL,
 content_sha256 CHAR(64) NOT NULL, signature_algorithm VARCHAR(32) NOT NULL, key_id VARCHAR(128) NOT NULL,
 signature TEXT NOT NULL, state VARCHAR(24) NOT NULL, created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_manifest_release FOREIGN KEY(release_id) REFERENCES aoo_client_release(id),
 CONSTRAINT uq_manifest_release UNIQUE(release_id), CONSTRAINT ck_manifest_state CHECK(state IN ('DRAFT','PUBLISHED','REVOKED')),
 CONSTRAINT ck_manifest_hash CHECK(content_sha256 REGEXP '^[0-9a-f]{64}$')
);
CREATE TABLE aoo_resource_manifest_entry (
 -- 760 utf8mb4 characters plus the BIGINT key stays below InnoDB's 3072-byte
 -- composite-key limit; 768 exceeded it on a clean MySQL 8 migration.
 manifest_id BIGINT NOT NULL, asset_path VARCHAR(760) NOT NULL, size_bytes BIGINT NOT NULL, sha256 CHAR(64) NOT NULL,
 PRIMARY KEY(manifest_id,asset_path), CONSTRAINT fk_manifest_entry FOREIGN KEY(manifest_id) REFERENCES aoo_resource_manifest(id),
 CONSTRAINT ck_asset_size CHECK(size_bytes>=0), CONSTRAINT ck_asset_hash CHECK(sha256 REGEXP '^[0-9a-f]{64}$')
);
CREATE TABLE aoo_client_notice (
 id BIGINT NOT NULL PRIMARY KEY, platform VARCHAR(32) NOT NULL DEFAULT '*', channel VARCHAR(64) NOT NULL DEFAULT '*',
 title VARCHAR(200) NOT NULL, body TEXT NOT NULL, severity VARCHAR(16) NOT NULL, starts_at TIMESTAMP(3) NOT NULL,
 ends_at TIMESTAMP(3) NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT ck_notice_severity CHECK(severity IN ('INFO','WARNING','CRITICAL')), INDEX ix_notice_active(enabled,starts_at,ends_at)
);
CREATE TABLE aoo_maintenance_window (
 id BIGINT NOT NULL PRIMARY KEY, platform VARCHAR(32) NOT NULL DEFAULT '*', channel VARCHAR(64) NOT NULL DEFAULT '*',
 starts_at TIMESTAMP(3) NOT NULL, ends_at TIMESTAMP(3) NOT NULL, message VARCHAR(500) NOT NULL,
 login_blocked BOOLEAN NOT NULL DEFAULT TRUE, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT ck_maintenance_time CHECK(ends_at>starts_at), INDEX ix_maintenance_active(enabled,starts_at,ends_at)
);
