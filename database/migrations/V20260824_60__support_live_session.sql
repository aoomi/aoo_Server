-- GAP-49-09: durable live support queue, ownership, reconnect cursor, audit and idempotency.
CREATE TABLE support_live_session (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, case_id BIGINT UNSIGNED NOT NULL, player_id BIGINT UNSIGNED NOT NULL,
 agent_id BIGINT UNSIGNED NULL, status ENUM('QUEUED','ACTIVE','DISCONNECTED','CLOSED') NOT NULL,
 active_case_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='CLOSED' THEN NULL ELSE case_id END) STORED,
 last_message_id BIGINT UNSIGNED NOT NULL DEFAULT 0, close_reason VARCHAR(500) NULL,
 created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL, closed_at DATETIME(3) NULL,
 row_version BIGINT UNSIGNED NOT NULL, PRIMARY KEY(id),
 UNIQUE KEY uq_support_live_open_case(active_case_id), KEY idx_support_live_queue(status,id),
 KEY idx_support_live_player(player_id,id), KEY idx_support_live_agent(agent_id,status,id),
 CONSTRAINT fk_support_live_case FOREIGN KEY(case_id) REFERENCES support_case(id) ON DELETE RESTRICT,
 CONSTRAINT chk_support_live_version CHECK(row_version>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_live_message (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, session_id BIGINT UNSIGNED NOT NULL,
 sender_type ENUM('PLAYER','SUPPORT','SYSTEM') NOT NULL, sender_id BIGINT UNSIGNED NOT NULL,
 body VARCHAR(2000) NOT NULL, media_reference VARCHAR(160) NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id), KEY idx_support_live_message_cursor(session_id,id),
 CONSTRAINT fk_support_live_message_session FOREIGN KEY(session_id) REFERENCES support_live_session(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_live_audit (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, session_id BIGINT UNSIGNED NOT NULL,
 actor_type ENUM('PLAYER','SUPPORT','SYSTEM') NOT NULL, actor_id BIGINT UNSIGNED NOT NULL,
 event_type VARCHAR(40) NOT NULL, event_detail VARCHAR(500) NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id), KEY idx_support_live_audit(session_id,id),
 CONSTRAINT fk_support_live_audit_session FOREIGN KEY(session_id) REFERENCES support_live_session(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_live_idempotency (
 actor_id BIGINT UNSIGNED NOT NULL, operation_key VARCHAR(80) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 session_id BIGINT UNSIGNED NOT NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(actor_id,operation_key,idempotency_key), KEY idx_support_live_idem_expiry(created_at),
 CONSTRAINT fk_support_live_idem_session FOREIGN KEY(session_id) REFERENCES support_live_session(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
