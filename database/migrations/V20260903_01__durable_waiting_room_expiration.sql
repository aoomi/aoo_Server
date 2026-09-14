ALTER TABLE aoo_room_authority_route
  ADD COLUMN first_round_started_at TIMESTAMP(3) NULL AFTER created_at,
  ADD KEY idx_room_authority_waiting_expiry(lifecycle_state,first_round_started_at,created_at,room_id);

UPDATE aoo_room_authority_route route
JOIN aoo_room_snapshot snapshot ON snapshot.room_id=route.room_id
SET route.first_round_started_at=COALESCE(snapshot.captured_at,route.updated_at)
WHERE route.first_round_started_at IS NULL
  AND (
    COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(snapshot.state_payload,'$.roundNo')) AS UNSIGNED),0)>0
    OR COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(snapshot.state_payload,'$.setID')) AS UNSIGNED),0)>0
    OR COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(snapshot.state_payload,'$.setId')) AS UNSIGNED),0)>0
    OR JSON_EXTRACT(snapshot.state_payload,'$.started')=TRUE
    OR UPPER(JSON_UNQUOTE(JSON_EXTRACT(snapshot.state_payload,'$.phase'))) IN ('PLAYING','IN_GAME','ROUND_PLAYING','DEALING')
  );
