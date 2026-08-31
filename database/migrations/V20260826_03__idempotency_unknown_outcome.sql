-- A command whose handler may have mutated memory before durable commit must
-- remain non-replayable until recovery determines its final outcome.
ALTER TABLE aoo_business_idempotency
  DROP CHECK chk_idempotency_status,
  ADD CONSTRAINT chk_idempotency_status CHECK(status IN('PROCESSING','COMPLETED','UNKNOWN'));
