-- Run on an authorized sanitized production-scale snapshot and store the JSON and
-- ANALYZE output in aoo_query_plan_observation. Replace variables; do not edit SQL shape.
SET @game_id = 1;
SET @region_code = 'GLOBAL';
SET @room_id = 1;
SET @after_sequence = 0;
SET @owner_player_id = 0;
SET @player_id = 1;
SET @currency_code = 'ROOM_CARD';
SET @currency_scope_id = 0;
SET @before_time = CURRENT_TIMESTAMP(3);
SET @before_id = 18446744073709551615;

EXPLAIN FORMAT=JSON
SELECT compiled.release_id,compiled.component_chain,compiled.rule_validator,compiled.ui_schema
FROM aoo_compiled_index_active active
JOIN aoo_compiled_room_create_index compiled
  ON compiled.game_id=active.game_id
 AND compiled.region_code=active.region_code
 AND compiled.play_version=active.play_version
 AND compiled.index_generation=active.index_generation
WHERE active.game_id=@game_id AND active.region_code=@region_code;

EXPLAIN ANALYZE
SELECT event_sequence,event_type,event_payload
FROM aoo_room_event
WHERE room_id=@room_id AND event_sequence>@after_sequence
  AND visibility='PUBLIC' AND owner_player_id=@owner_player_id
ORDER BY event_sequence
LIMIT 500;

EXPLAIN ANALYZE
SELECT ledger_id,business_id,delta,balance_after,created_at
FROM aoo_ledger
WHERE player_id=@player_id AND currency=@currency_code AND currency_scope_id=@currency_scope_id
  AND (created_at,ledger_id)<(@before_time,@before_id)
ORDER BY created_at DESC,ledger_id DESC
LIMIT 200;

-- Negative comparison only: capture its examined rows to prove why OFFSET is banned.
EXPLAIN ANALYZE
SELECT event_sequence
FROM aoo_room_event
WHERE room_id=@room_id
ORDER BY event_sequence
LIMIT 1000000,100;
