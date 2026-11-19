-- Forward metadata only. External application requires its own exact-schema deployment gate.
CREATE TABLE aoo_room_aa_session (
 session_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 session_key CHAR(64) NOT NULL UNIQUE,
 room_id BIGINT UNSIGNED NOT NULL,
 room_instance_id VARCHAR(128) NOT NULL,
 lifecycle_epoch BIGINT UNSIGNED NOT NULL,
 creator_id BIGINT UNSIGNED NOT NULL,
 context_hash CHAR(64) NOT NULL,
 created_at TIMESTAMP(3) NOT NULL,
 CHECK(room_id>0 AND lifecycle_epoch>0 AND creator_id>0)
) ENGINE=InnoDB;
CREATE TABLE aoo_room_aa_attempt (
 session_id BIGINT UNSIGNED NOT NULL,
 actor_id BIGINT UNSIGNED NOT NULL,
 attempt_no BIGINT UNSIGNED NOT NULL,
 operation_key CHAR(64) NOT NULL UNIQUE,
 subject_command_hash CHAR(64) NOT NULL,
 binding_hash CHAR(64) NOT NULL,
 state VARCHAR(16) NOT NULL,
 created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(session_id,actor_id,attempt_no),
 FOREIGN KEY(session_id) REFERENCES aoo_room_aa_session(session_id),
 FOREIGN KEY(operation_key) REFERENCES aoo_room_billing_intent_journal(operation_key),
 CHECK(actor_id>0 AND attempt_no>0 AND state IN('PREPARED','SEATED'))
) ENGINE=InnoDB;
CREATE TABLE aoo_room_aa_command (
 session_id BIGINT UNSIGNED NOT NULL,
 command_key CHAR(64) NOT NULL,
 request_id VARCHAR(128) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 actor_id BIGINT UNSIGNED NOT NULL,
 attempt_no BIGINT UNSIGNED NOT NULL,
 preparation_hash CHAR(64) NOT NULL,
 pre_seat INT NOT NULL,
 pre_fence BIGINT UNSIGNED NOT NULL,
 pre_version BIGINT UNSIGNED NOT NULL,
 pre_snapshot_hash CHAR(64) NOT NULL,
 status VARCHAR(16) NOT NULL,
 post_seat INT,
 post_fence BIGINT UNSIGNED,
 post_version BIGINT UNSIGNED,
 post_snapshot_hash CHAR(64),
 response_hash CHAR(64),
 committed_at TIMESTAMP(3),
 created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(session_id,command_key),
 FOREIGN KEY(session_id,actor_id,attempt_no) REFERENCES aoo_room_aa_attempt(session_id,actor_id,attempt_no),
 CHECK(pre_seat>=-1 AND pre_fence>0 AND status IN('PREPARED','COMMITTED')),
 CHECK((status='PREPARED' AND committed_at IS NULL) OR
       (status='COMMITTED' AND post_seat>=0 AND post_fence>0 AND post_version>pre_version
        AND post_snapshot_hash IS NOT NULL AND response_hash IS NOT NULL AND committed_at IS NOT NULL))
) ENGINE=InnoDB;
