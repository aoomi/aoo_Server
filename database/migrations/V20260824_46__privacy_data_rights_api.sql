CREATE TABLE aoo_data_rights_request (
 request_id CHAR(36) NOT NULL, account_id BIGINT NOT NULL, request_type VARCHAR(16) NOT NULL,
 status VARCHAR(24) NOT NULL, requested_at TIMESTAMP(3) NOT NULL, cooling_off_until TIMESTAMP(3) NULL,
 cancelled_at TIMESTAMP(3) NULL, completed_at TIMESTAMP(3) NULL, result_manifest JSON NULL,
 failure_reason VARCHAR(255) NULL, version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(request_id), KEY idx_data_rights_owner(account_id,requested_at), KEY idx_data_rights_due(status,cooling_off_until),
 CONSTRAINT fk_data_rights_account FOREIGN KEY(account_id) REFERENCES aoo_account(account_id),
 CONSTRAINT chk_data_rights_type CHECK(request_type IN ('EXPORT','DELETE')),
 CONSTRAINT chk_data_rights_status CHECK(status IN ('PENDING','COOLING_OFF','ON_HOLD','RUNNING','COMPLETED','CANCELLED','FAILED'))
);
CREATE TABLE aoo_account_legal_hold (
 hold_id CHAR(36) NOT NULL, account_id BIGINT NOT NULL, authority_reference VARCHAR(128) NOT NULL,
 reason VARCHAR(500) NOT NULL, placed_by VARCHAR(128) NOT NULL, placed_at TIMESTAMP(3) NOT NULL,
 released_by VARCHAR(128) NULL, released_at TIMESTAMP(3) NULL,
 PRIMARY KEY(hold_id), KEY idx_account_hold(account_id,released_at),
 CONSTRAINT fk_account_hold_account FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
);
CREATE TABLE aoo_data_rights_audit (
 audit_id BIGINT NOT NULL AUTO_INCREMENT, request_id CHAR(36) NULL, account_id BIGINT NOT NULL,
 action_name VARCHAR(64) NOT NULL, actor_type VARCHAR(16) NOT NULL, actor_ref VARCHAR(128) NOT NULL,
 detail_json JSON NOT NULL, occurred_at TIMESTAMP(3) NOT NULL, PRIMARY KEY(audit_id),
 KEY idx_rights_audit_account(account_id,occurred_at),
 CONSTRAINT fk_rights_audit_request FOREIGN KEY(request_id) REFERENCES aoo_data_rights_request(request_id),
 CONSTRAINT fk_rights_audit_account FOREIGN KEY(account_id) REFERENCES aoo_account(account_id)
);
