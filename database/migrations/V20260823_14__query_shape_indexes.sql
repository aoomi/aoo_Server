-- Composite indexes follow equality predicates first, then range/order columns.
ALTER TABLE aoo_business_idempotency
  ADD KEY idx_idempotency_result (request_id, status, expires_at);
ALTER TABLE aoo_room_event
  ADD KEY idx_room_event_view_sequence (room_id, visibility, owner_player_id, event_sequence);
ALTER TABLE aoo_room_snapshot
  ADD KEY idx_snapshot_recoverable (captured_at, room_id);
ALTER TABLE aoo_room_template
  ADD KEY idx_template_active_page (club_id, status, game_id, template_code, template_version);
ALTER TABLE aoo_play_variant
  ADD KEY idx_play_active_region (region_code, status, game_id, play_version);
