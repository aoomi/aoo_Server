CREATE TABLE IF NOT EXISTS aoo_club_member (
    club_id BIGINT UNSIGNED NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    member_status VARCHAR(16) NOT NULL,
    member_role VARCHAR(16) NOT NULL,
    online TINYINT NOT NULL DEFAULT 0,
    profile_payload JSON NOT NULL,
    joined_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (club_id, player_id),
    KEY idx_club_member_page (club_id, member_status, player_id),
    KEY idx_club_online_page (club_id, online, player_id),
    KEY idx_club_manager_online (club_id, member_role, online, player_id),
    KEY idx_player_clubs (player_id, member_status, club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
