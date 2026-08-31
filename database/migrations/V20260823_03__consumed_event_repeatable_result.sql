ALTER TABLE aoo_consumed_event
    ADD COLUMN result_code INT NULL AFTER processed_at,
    ADD COLUMN result_schema_version INT NULL AFTER result_code,
    ADD COLUMN result_payload JSON NULL AFTER result_schema_version;

UPDATE aoo_consumed_event
SET result_code=0,result_schema_version=1,result_payload=JSON_OBJECT()
WHERE status='PROCESSED' AND result_code IS NULL;
