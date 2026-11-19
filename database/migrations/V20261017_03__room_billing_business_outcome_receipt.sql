-- Forward exact create receipts only. All production snapshot writers must share the binding fence.
CREATE TABLE aoo_room_billing_business_receipt (
 source_namespace VARCHAR(32) NOT NULL,
 source_request_id VARCHAR(128) NOT NULL,
 event_type VARCHAR(32) NOT NULL,
 operation_key CHAR(64) NOT NULL,
 room_id BIGINT UNSIGNED NOT NULL,
 creator_id BIGINT UNSIGNED NOT NULL,
 room_instance_id VARCHAR(128) NULL,
 lifecycle_epoch BIGINT UNSIGNED NULL,
 fencing_token BIGINT UNSIGNED NULL,
 state_version BIGINT UNSIGNED NULL,
 game_id BIGINT UNSIGNED NOT NULL,
 play_version VARCHAR(64) NOT NULL,
 outcome_hash CHAR(64) NOT NULL,
 created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(source_namespace,source_request_id,event_type),
 FOREIGN KEY(source_namespace,source_request_id) REFERENCES aoo_room_billing_source_binding(source_namespace,source_request_id) ON DELETE RESTRICT,
 FOREIGN KEY(operation_key) REFERENCES aoo_room_billing_intent_journal(operation_key) ON DELETE RESTRICT,
 CHECK(event_type IN('CREATE_COMMITTED','CREATE_ABORTED_BEFORE_COMMIT','CREATE_DESTROYED')),
 CHECK(room_id>0 AND creator_id>0 AND game_id>0),
 CHECK((event_type IN('CREATE_COMMITTED','CREATE_DESTROYED') AND room_instance_id IS NOT NULL AND CHAR_LENGTH(TRIM(room_instance_id))>0 AND lifecycle_epoch IS NOT NULL AND lifecycle_epoch>0 AND fencing_token IS NOT NULL AND fencing_token>0 AND state_version IS NOT NULL AND state_version>=0) OR (event_type='CREATE_ABORTED_BEFORE_COMMIT' AND room_instance_id IS NULL AND lifecycle_epoch IS NULL AND fencing_token IS NULL AND state_version IS NULL)),
 CHECK(outcome_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB;
