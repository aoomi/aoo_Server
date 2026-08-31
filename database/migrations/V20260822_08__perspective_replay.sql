CREATE TABLE IF NOT EXISTS perspective_replay_event (
    room_id BIGINT UNSIGNED NOT NULL,
    set_id INT UNSIGNED NOT NULL,
    event_sequence BIGINT UNSIGNED NOT NULL,
    visibility VARCHAR(24) NOT NULL,
    owner_player_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    message_id VARCHAR(128) NOT NULL,
    payload MEDIUMBLOB NOT NULL,
    PRIMARY KEY (room_id, set_id, event_sequence, visibility, owner_player_id),
    KEY idx_replay_player_view (owner_player_id, room_id, set_id, event_sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS replay_participant (
    room_id BIGINT UNSIGNED NOT NULL,
    set_id INT UNSIGNED NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    seat_id INT UNSIGNED NOT NULL,
    granted_at DATETIME(3) NOT NULL,
    PRIMARY KEY (room_id, set_id, player_id),
    KEY idx_replay_participant_player (player_id, room_id, set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
