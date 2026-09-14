-- Settlement templates are selected by the published game profile. Empty means
-- the resolver's category/family default; only real visual differences are stored.
UPDATE aoo_compiled_room_create_index
SET ui_schema = JSON_REMOVE(
        JSON_SET(COALESCE(ui_schema, JSON_OBJECT()), '$.bigSettleTemplate', 'BigSettleTpl_110'),
        '$.smallSettleTemplate')
WHERE game_id = 629
  AND play_version = 'legacy-equivalent-1'
  AND lifecycle_state = 'STAGED';

UPDATE aoo_game_release
SET ui_snapshot = JSON_REMOVE(
        JSON_SET(COALESCE(ui_snapshot, JSON_OBJECT()), '$.bigSettleTemplate', 'BigSettleTpl_110'),
        '$.smallSettleTemplate')
WHERE game_id = 629
  AND play_version = 'legacy-equivalent-1'
  AND status = 'STAGED';
