ALTER TABLE aoo_room_snapshot
    ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0 AFTER fencing_token,
    ADD KEY idx_snapshot_state_event_version (room_id,state_version,last_event_sequence);

UPDATE aoo_room_snapshot SET state_version=last_event_sequence WHERE state_version=0;
