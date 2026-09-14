CREATE TABLE aoo_client_feature_flag (
 flag_key VARCHAR(128) NOT NULL, platform VARCHAR(32) NOT NULL DEFAULT '*', channel VARCHAR(64) NOT NULL DEFAULT '*',
 enabled BOOLEAN NOT NULL, flag_value VARCHAR(1000) NULL, rollout_percent SMALLINT NOT NULL DEFAULT 100,
 enabled_from TIMESTAMP(3) NOT NULL, enabled_until TIMESTAMP(3) NULL, updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(flag_key,platform,channel), CONSTRAINT ck_feature_rollout CHECK(rollout_percent BETWEEN 0 AND 100),
 CONSTRAINT ck_feature_window CHECK(enabled_until IS NULL OR enabled_until>enabled_from),
 INDEX ix_feature_resolution(platform,channel,enabled_from,enabled_until)
);

CREATE TABLE aoo_server_directory (
 id BIGINT NOT NULL PRIMARY KEY, server_code VARCHAR(64) NOT NULL, display_name VARCHAR(128) NOT NULL,
 region VARCHAR(64) NOT NULL, public_endpoint VARCHAR(1024) NOT NULL, platform VARCHAR(32) NOT NULL DEFAULT '*',
 channel VARCHAR(64) NOT NULL DEFAULT '*', state VARCHAR(24) NOT NULL, weight INT NOT NULL DEFAULT 100,
 rollout_percent SMALLINT NOT NULL DEFAULT 100, migrate_to_server_id BIGINT NULL, message VARCHAR(500) NULL,
 directory_revision BIGINT NOT NULL, visible_from TIMESTAMP(3) NOT NULL, visible_until TIMESTAMP(3) NULL,
 CONSTRAINT uq_server_code UNIQUE(server_code), CONSTRAINT fk_server_migration FOREIGN KEY(migrate_to_server_id) REFERENCES aoo_server_directory(id),
 CONSTRAINT ck_server_state CHECK(state IN ('ACTIVE','MAINTENANCE','MIGRATING','OFFLINE')),
 CONSTRAINT ck_server_rollout CHECK(rollout_percent BETWEEN 0 AND 100),
 CONSTRAINT ck_server_window CHECK(visible_until IS NULL OR visible_until>visible_from),
 INDEX ix_directory_resolution(platform,channel,state,visible_from,visible_until,directory_revision)
);
