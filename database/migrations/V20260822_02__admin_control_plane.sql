CREATE TABLE IF NOT EXISTS admin_control_resource (
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    updated_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (resource_type, resource_id),
    KEY idx_admin_resource_updated (resource_type, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_command_dedup (
    request_id VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    operator_id BIGINT UNSIGNED NOT NULL,
    response_json JSON NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (request_id),
    KEY idx_admin_command_resource (resource_type, resource_id, created_at),
    KEY idx_admin_command_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
