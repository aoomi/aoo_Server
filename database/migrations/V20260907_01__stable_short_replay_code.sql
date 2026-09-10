CREATE TABLE replay_short_code (
    short_code VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    code_length TINYINT UNSIGNED NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    set_id INT UNSIGNED NOT NULL,
    status ENUM('ACTIVE','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NULL,
    PRIMARY KEY (short_code),
    UNIQUE KEY uq_replay_short_target (room_id,set_id),
    KEY idx_replay_short_capacity (code_length,status),
    CONSTRAINT chk_replay_short_code CHECK (
        code_length IN (6,7,8) AND CHAR_LENGTH(short_code)=code_length
        AND short_code REGEXP '^[1-9][0-9]+$'
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE replay_short_code_access (
    short_code VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (short_code,player_id),
    KEY idx_replay_short_access_player (player_id,granted_at),
    CONSTRAINT fk_replay_short_access_code FOREIGN KEY (short_code)
        REFERENCES replay_short_code(short_code) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
