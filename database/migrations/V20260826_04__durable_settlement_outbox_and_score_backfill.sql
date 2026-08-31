CREATE TABLE IF NOT EXISTS aoo_settlement_outbox (
    business_id VARCHAR(128) NOT NULL PRIMARY KEY,
    room_id BIGINT UNSIGNED NOT NULL,
    round_no INT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    result_payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_settlement_outbox_retry(status,next_attempt_at,created_at),
    CONSTRAINT chk_settlement_outbox_status CHECK(status='PENDING')
) ENGINE=InnoDB;

INSERT IGNORE INTO aoo_settlement_score(
    settlement_id,player_id,score_scale,score_delta,score_after,multiplier,
    score_reason,calculation_hash,created_at)
SELECT s.settlement_id,j.player_id,0,j.score_delta,NULL,1.000000000,
       IF(s.business_id LIKE 'final-room:%','FINAL_ROOM','ROUND'),
       LOWER(SHA2(CONCAT(s.business_id,'|',j.player_id,'|',j.score_delta),256)),s.created_at
FROM aoo_settlement s
JOIN JSON_TABLE(s.result_payload,'$.entries[*]' COLUMNS(
    player_id BIGINT PATH '$.playerId',score_delta DECIMAL(38,9) PATH '$.scoreDelta'
)) j ON TRUE;
