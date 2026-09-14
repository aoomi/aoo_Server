-- Matchmaking is global for a published game/play version. Region is never a queue partition.
ALTER TABLE aoo_match_queue DROP INDEX idx_match_queue_candidate;
ALTER TABLE aoo_match_queue DROP COLUMN region_code;
ALTER TABLE aoo_match_queue ADD KEY idx_match_queue_candidate(game_id,play_version,state,enqueued_at,player_id);
