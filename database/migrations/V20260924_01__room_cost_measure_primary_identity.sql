-- A room-cost policy is identified by its authoritative measure dimension.
-- Keep round_count as a trailing compatibility discriminator because retired
-- pre-measure rows can share the same canonical measure after backfill. The
-- authoritative measure columns precede it, so 30 minutes and 30 rounds are
-- distinct policies without deleting or rewriting historical rows.
ALTER TABLE aoo_room_cost_policy
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (
    game_id,
    play_version,
    region_code,
    measure_kind,
    measure_value,
    round_count,
    player_count,
    payer_mode
  );
