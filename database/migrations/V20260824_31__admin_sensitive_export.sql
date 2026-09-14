CREATE TABLE IF NOT EXISTS admin_sensitive_export (
 export_id VARCHAR(128) PRIMARY KEY, definition_code VARCHAR(64) NOT NULL, scope_code VARCHAR(96) NOT NULL,
 row_limit INT UNSIGNED NOT NULL, requester_id BIGINT UNSIGNED NOT NULL, approver_id BIGINT UNSIGNED NULL,
 reason VARCHAR(512) NOT NULL, state VARCHAR(32) NOT NULL, updated_at DATETIME(3) NOT NULL,
 KEY idx_sensitive_export_requester(requester_id,updated_at), KEY idx_sensitive_export_state(state,updated_at),
 CONSTRAINT chk_sensitive_export_rows CHECK(row_limit BETWEEN 1 AND 100000),
 CONSTRAINT chk_sensitive_export_state CHECK(state IN ('PENDING_APPROVAL','APPROVED','EXPORTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_sensitive_export_request (
 request_id VARCHAR(128) PRIMARY KEY, export_id VARCHAR(128) NOT NULL,
 CONSTRAINT fk_sensitive_request_export FOREIGN KEY(export_id) REFERENCES admin_sensitive_export(export_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_sensitive_export_artifact (
 export_id VARCHAR(128) PRIMARY KEY, artifact_json JSON NOT NULL, content_hash CHAR(64) NOT NULL,
 generated_at DATETIME(3) NOT NULL, expires_at DATETIME(3) NOT NULL,
 CONSTRAINT fk_sensitive_artifact_export FOREIGN KEY(export_id) REFERENCES admin_sensitive_export(export_id),
 KEY idx_sensitive_artifact_expiry(expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS admin_sensitive_export_audit (
 audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, export_id VARCHAR(128) NOT NULL,
 operator_id BIGINT UNSIGNED NOT NULL, action VARCHAR(32) NOT NULL, scope_code VARCHAR(96) NOT NULL,
 row_count INT UNSIGNED NOT NULL, content_hash CHAR(64) NOT NULL, occurred_at DATETIME(3) NOT NULL,
 CONSTRAINT fk_sensitive_audit_export FOREIGN KEY(export_id) REFERENCES admin_sensitive_export(export_id),
 KEY idx_sensitive_audit_export(export_id,audit_id), KEY idx_sensitive_audit_operator(operator_id,occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
