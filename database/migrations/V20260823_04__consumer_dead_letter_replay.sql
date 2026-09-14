ALTER TABLE aoo_consumed_event
    ADD COLUMN failure_count INT NOT NULL DEFAULT 0 AFTER claimed_at,
    ADD COLUMN last_error VARCHAR(500) NULL AFTER failure_count,
    ADD COLUMN dead_payload JSON NULL AFTER last_error,
    ADD COLUMN dead_at DATETIME(3) NULL AFTER dead_payload,
    ADD COLUMN replay_count INT NOT NULL DEFAULT 0 AFTER dead_at,
    ADD KEY idx_consumed_dead_letter (status, dead_at);

CREATE TABLE IF NOT EXISTS aoo_dead_letter_replay_audit (
    replay_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    operator_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    replayed_at DATETIME(3) NOT NULL,
    KEY idx_dlq_replay_event (consumer_name,event_id,replayed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
