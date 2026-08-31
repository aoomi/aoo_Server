-- Canonical business identities for gameplay variants and club room templates.
CREATE TABLE IF NOT EXISTS aoo_play_variant (
  game_id BIGINT UNSIGNED NOT NULL,
  play_version VARCHAR(64) NOT NULL,
  region_code VARCHAR(32) NOT NULL,
  component_version VARCHAR(64) NOT NULL,
  rule_schema_version INT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (game_id, play_version, region_code),
  UNIQUE KEY uk_play_component_region (component_version, game_id, region_code),
  KEY idx_play_region_status (region_code, status, game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aoo_room_template (
  club_id BIGINT UNSIGNED NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_version BIGINT UNSIGNED NOT NULL,
  game_id BIGINT UNSIGNED NOT NULL,
  play_version VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  rule_payload JSON NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (club_id, template_code, template_version),
  UNIQUE KEY uk_template_active_name (club_id, display_name, status),
  KEY idx_template_catalog (club_id, status, game_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Existing canonical identities are repeated here as an executable migration contract.
ALTER TABLE aoo_club_member
  ADD UNIQUE KEY uk_club_member_business (club_id, player_id);

ALTER TABLE game_profile_version
  ADD UNIQUE KEY uk_game_profile_business (game_id, version);
