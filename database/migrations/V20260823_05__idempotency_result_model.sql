ALTER TABLE aoo_business_idempotency
    ADD COLUMN response_code INT NULL AFTER status,
    ADD COLUMN response_version VARCHAR(32) NULL AFTER response_code,
    ADD COLUMN response_schema_version INT NULL AFTER response_version;

UPDATE aoo_business_idempotency
SET response_code=0,response_version='v1',response_schema_version=1
WHERE status='COMPLETED' AND response_code IS NULL;
