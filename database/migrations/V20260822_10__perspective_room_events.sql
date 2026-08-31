ALTER TABLE aoo_room_event
    ADD COLUMN visibility VARCHAR(24) NOT NULL DEFAULT 'LEGACY_RESTRICTED',
    ADD COLUMN owner_player_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    ADD KEY idx_room_event_player_view
        (room_id, event_sequence, visibility, owner_player_id);
