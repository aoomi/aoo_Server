-- One authoritative action can produce a public event and one private event per
-- player at the same sequence. Visibility and owner are therefore part of the
-- immutable event identity.
ALTER TABLE aoo_room_event
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (room_id, event_sequence, visibility, owner_player_id);
