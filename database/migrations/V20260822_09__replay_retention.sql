ALTER TABLE perspective_replay_event
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ADD KEY idx_replay_retention (created_at, room_id, set_id);

ALTER TABLE replay_participant
    ADD KEY idx_replay_participant_retention (granted_at, room_id, set_id);
