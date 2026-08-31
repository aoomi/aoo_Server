CREATE TABLE aoo_identity_verification (
 account_id BIGINT NOT NULL PRIMARY KEY, real_name_status VARCHAR(24) NOT NULL DEFAULT 'UNSUBMITTED',
 legal_name_cipher TEXT NULL, id_number_cipher TEXT NULL, id_token VARCHAR(64) NULL,
 masked_name VARCHAR(64) NULL, masked_id_number VARCHAR(32) NULL,
 phone_verified BOOLEAN NOT NULL DEFAULT FALSE, phone_token VARCHAR(64) NULL, masked_phone VARCHAR(32) NULL,
 updated_at TIMESTAMP(6) NOT NULL, UNIQUE KEY uq_identity_document_token(id_token), KEY ix_identity_phone_token(phone_token)
);
CREATE TABLE aoo_phone_challenge (
 challenge_id CHAR(36) NOT NULL PRIMARY KEY, account_id BIGINT NOT NULL, phone_cipher TEXT NOT NULL,
 phone_token VARCHAR(64) NOT NULL, masked_phone VARCHAR(32) NOT NULL, code_hash VARCHAR(64) NOT NULL,
 attempts INT NOT NULL DEFAULT 0, expires_at TIMESTAMP(6) NOT NULL, verified_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL,
 KEY ix_phone_challenge_account_time(account_id,created_at), KEY ix_phone_challenge_token_time(phone_token,created_at)
);
CREATE TABLE aoo_identity_credential (
 credential_token VARCHAR(64) NOT NULL PRIMARY KEY, account_id BIGINT NOT NULL, purpose VARCHAR(32) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL, consumed_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL,
 KEY ix_identity_credential_account(account_id,expires_at)
);
CREATE TABLE aoo_identity_idempotency (
 account_id BIGINT NOT NULL, operation_scope VARCHAR(32) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 request_fingerprint VARCHAR(64) NOT NULL, response_cipher TEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY(account_id,operation_scope,idempotency_key)
);
CREATE TABLE aoo_identity_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL, action VARCHAR(48) NOT NULL,
 ip_token VARCHAR(64) NOT NULL, details_json TEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 KEY ix_identity_audit_account_time(account_id,created_at)
);
