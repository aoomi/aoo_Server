-- EDGE end-to-end state machines: reject impossible durable states at the database boundary.
ALTER TABLE aoo_business_idempotency
    ADD CONSTRAINT chk_edge_idempotency_status CHECK (status IN ('PROCESSING','COMPLETED','UNKNOWN')),
    ADD CONSTRAINT chk_edge_idempotency_result CHECK (
        (status='COMPLETED' AND response_code IS NOT NULL AND response_payload IS NOT NULL)
        OR (status IN ('PROCESSING','UNKNOWN') AND response_code IS NULL));

ALTER TABLE aoo_outbox
    ADD CONSTRAINT chk_edge_outbox_status CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','DEAD')),
    ADD CONSTRAINT chk_edge_outbox_claim CHECK (
        (status='PROCESSING' AND locked_by IS NOT NULL AND claim_token IS NOT NULL AND locked_until IS NOT NULL)
        OR status<>'PROCESSING');

ALTER TABLE aoo_consumed_event
    ADD CONSTRAINT chk_edge_consumer_status CHECK (status IN ('PROCESSING','RETRY','PROCESSED','DEAD')),
    ADD CONSTRAINT chk_edge_consumer_result CHECK (
        (status='PROCESSED' AND processed_at IS NOT NULL AND result_code IS NOT NULL)
        OR status<>'PROCESSED');

ALTER TABLE aoo_room_lease
    ADD CONSTRAINT chk_edge_room_lease_fence CHECK (fencing_token>0);
