-- Durable player history and resumable perspective-replay query contract.
ALTER TABLE perspective_replay_event
    ADD COLUMN content_hash CHAR(64)
      GENERATED ALWAYS AS (LOWER(SHA2(payload, 256))) STORED AFTER payload,
    ADD KEY idx_replay_chunk (room_id,set_id,event_sequence,visibility,owner_player_id,play_version);

ALTER TABLE perspective_replay_event_archive
    ADD COLUMN content_hash CHAR(64)
      GENERATED ALWAYS AS (LOWER(SHA2(payload, 256))) STORED AFTER payload;

CREATE TABLE IF NOT EXISTS replay_set_manifest (
    room_id BIGINT UNSIGNED NOT NULL,
    set_id INT UNSIGNED NOT NULL,
    play_version VARCHAR(128) NOT NULL,
    schema_version INT NOT NULL,
    event_count BIGINT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    closed_at DATETIME(3) NOT NULL,
    archive_after DATETIME(3) NOT NULL,
    delete_after DATETIME(3) NOT NULL,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY(room_id,set_id),
    KEY idx_replay_manifest_lifecycle(legal_hold,archive_after,delete_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A participant-first covering index prevents the old unscoped settlement scan.
ALTER TABLE replay_participant
    ADD KEY idx_history_player_granted (player_id,granted_at,room_id,set_id);
ALTER TABLE replay_participant_archive
    ADD KEY idx_history_archive_player_granted (player_id,granted_at,room_id,set_id);
