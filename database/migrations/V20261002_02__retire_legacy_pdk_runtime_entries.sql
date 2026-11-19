-- Remove legacy PDK identities from the active create/route path.
-- Historical rooms keep their immutable release and compiled-index rows; those
-- rows are retained as RETIRED so recovery can still resolve their resources.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

UPDATE aoo_game_service_route r
JOIN aoo_game_catalog g ON g.game_id=r.game_id
SET r.status='OFFLINE'
WHERE g.family_code='poker:pao-de-kuai'
  AND g.game_code REGEXP '^[A-Z]{2,3}201$'
  AND r.status='ACTIVE';

UPDATE aoo_play_version p
JOIN aoo_game_catalog g ON g.game_id=p.game_id
SET p.status='RETIRED',p.retired_at=COALESCE(p.retired_at,CURRENT_TIMESTAMP(3))
WHERE g.family_code='poker:pao-de-kuai'
  AND g.game_code REGEXP '^[A-Z]{2,3}201$'
  AND p.status='ACTIVE';

UPDATE aoo_game_release r
JOIN aoo_game_catalog g ON g.game_id=r.game_id
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE g.family_code='poker:pao-de-kuai'
  AND g.game_code REGEXP '^[A-Z]{2,3}201$'
  AND r.status='ACTIVE';

UPDATE aoo_compiled_room_create_index i
JOIN aoo_game_catalog g ON g.game_id=i.game_id
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE g.family_code='poker:pao-de-kuai'
  AND g.game_code REGEXP '^[A-Z]{2,3}201$'
  AND i.lifecycle_state='ACTIVE';

-- No current cache epoch or room-rule lock may point at these legacy active
-- pointers.  If one appears, the FK failure intentionally stops the migration
-- instead of deleting a live dependency.
DELETE a
FROM aoo_compiled_index_active a
JOIN aoo_game_catalog g ON g.game_id=a.game_id
WHERE g.family_code='poker:pao-de-kuai'
  AND g.game_code REGEXP '^[A-Z]{2,3}201$';

COMMIT;
