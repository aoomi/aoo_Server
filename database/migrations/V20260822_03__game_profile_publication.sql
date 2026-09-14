CREATE TABLE IF NOT EXISTS game_profile_version (
    game_id BIGINT UNSIGNED NOT NULL,
    version VARCHAR(64) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    profile_json JSON NOT NULL,
    published_at DATETIME(3) NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (game_id, version),
    KEY idx_game_profile_scope (scope, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_profile_active (
    game_id BIGINT UNSIGNED NOT NULL,
    version VARCHAR(64) NOT NULL,
    activated_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    activated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (game_id),
    CONSTRAINT fk_game_profile_active_version FOREIGN KEY (game_id, version)
        REFERENCES game_profile_version(game_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_profile_audit (
    audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    game_id BIGINT UNSIGNED NOT NULL,
    version VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_game_profile_audit (game_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
