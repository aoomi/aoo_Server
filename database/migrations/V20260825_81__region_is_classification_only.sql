-- region_code is gameplay classification/rule provenance only. It must never partition player assets or service routes.
ALTER TABLE aoo_hall_room
  MODIFY region_code VARCHAR(32) NOT NULL COMMENT 'Gameplay classification provenance; server-derived; never an account, asset, club, room-number or service-route partition key';

CREATE TABLE IF NOT EXISTS aoo_region_semantics_policy (
  policy_key VARCHAR(64) NOT NULL,
  policy_value VARCHAR(255) NOT NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(policy_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_region_semantics_policy(policy_key,policy_value) VALUES
 ('SEMANTICS','GAMEPLAY_CLASSIFICATION_ONLY'),
 ('PLAYER_ASSET_PARTITION','FORBIDDEN'),
 ('CLUB_PARTITION','FORBIDDEN'),
 ('ROOM_ROUTE_PARTITION','FORBIDDEN'),
 ('HISTORY_PARTITION','FORBIDDEN')
ON DUPLICATE KEY UPDATE policy_value=VALUES(policy_value),updated_at=CURRENT_TIMESTAMP(3);
