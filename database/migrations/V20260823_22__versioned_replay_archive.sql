ALTER TABLE perspective_replay_event
    ADD COLUMN schema_version INT NOT NULL DEFAULT 1 AFTER message_id,
    ADD COLUMN play_version VARCHAR(128) NOT NULL DEFAULT 'legacy-v1' AFTER schema_version;

CREATE TABLE IF NOT EXISTS perspective_replay_event_archive (
    room_id BIGINT UNSIGNED NOT NULL,set_id INT UNSIGNED NOT NULL,event_sequence BIGINT UNSIGNED NOT NULL,
    visibility VARCHAR(24) NOT NULL,owner_player_id BIGINT UNSIGNED NOT NULL,message_id VARCHAR(128) NOT NULL,
    schema_version INT NOT NULL,play_version VARCHAR(128) NOT NULL,payload MEDIUMBLOB NOT NULL,
    created_at DATETIME(3) NOT NULL,archived_at DATETIME(3) NOT NULL,
    PRIMARY KEY(room_id,set_id,event_sequence,visibility,owner_player_id),
    KEY idx_replay_archive_retention(archived_at,room_id,set_id),
    KEY idx_replay_archive_player(owner_player_id,room_id,set_id,event_sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS replay_participant_archive (
    room_id BIGINT UNSIGNED NOT NULL,set_id INT UNSIGNED NOT NULL,player_id BIGINT UNSIGNED NOT NULL,
    seat_id INT UNSIGNED NOT NULL,granted_at DATETIME(3) NOT NULL,archived_at DATETIME(3) NOT NULL,
    PRIMARY KEY(room_id,set_id,player_id),KEY idx_replay_participant_archive_player(player_id,room_id,set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
