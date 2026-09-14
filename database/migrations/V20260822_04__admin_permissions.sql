CREATE TABLE IF NOT EXISTS admin_operator_permission (
    operator_id BIGINT UNSIGNED NOT NULL,
    permission_code VARCHAR(96) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    granted_by BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (operator_id, permission_code),
    KEY idx_admin_permission_code (permission_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- No default administrator is inserted. Initial permissions must be granted through
-- an audited deployment procedure so a fresh environment remains deny-by-default.
