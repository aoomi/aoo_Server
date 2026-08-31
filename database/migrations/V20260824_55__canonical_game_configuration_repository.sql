CREATE TABLE aoo_published_game_configuration (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    configuration_payload JSON NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,play_version),
    CONSTRAINT fk_published_configuration_play FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_published_game_configuration(game_id,play_version,configuration_payload,created_by,reason,created_at)
SELECT legacy.game_id,legacy.version,legacy.profile_json,legacy.created_by,
       CONCAT('migrated: ',legacy.reason),legacy.published_at
FROM game_profile_version legacy
JOIN aoo_play_version play ON play.game_id=legacy.game_id AND play.play_version=legacy.version
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id);
