-- Replay visibility is an authorization boundary. Negative room trustee placeholders and the
-- public sentinel 0 are not durable player identities and cannot own private replay data.
DELETE FROM replay_participant WHERE player_id = 0;
DELETE FROM replay_participant_archive WHERE player_id = 0;

ALTER TABLE replay_participant
    ADD CONSTRAINT chk_replay_participant_player
        CHECK (player_id > 0);

ALTER TABLE replay_participant_archive
    ADD CONSTRAINT chk_replay_participant_archive_player
        CHECK (player_id > 0);

ALTER TABLE perspective_replay_event
    ADD CONSTRAINT chk_replay_event_owner
        CHECK ((visibility = 'PUBLIC' AND owner_player_id = 0)
            OR (visibility = 'PLAYER_PRIVATE' AND owner_player_id > 0));

ALTER TABLE perspective_replay_event_archive
    ADD CONSTRAINT chk_replay_event_archive_owner
        CHECK ((visibility = 'PUBLIC' AND owner_player_id = 0)
            OR (visibility = 'PLAYER_PRIVATE' AND owner_player_id > 0));
