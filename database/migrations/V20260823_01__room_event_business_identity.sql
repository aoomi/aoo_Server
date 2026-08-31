ALTER TABLE aoo_room_event
    ADD COLUMN round_no INT NULL AFTER room_id,
    ADD COLUMN business_event_id VARCHAR(128) NULL AFTER event_sequence,
    ADD COLUMN schema_version INT NOT NULL DEFAULT 1 AFTER event_type;

UPDATE aoo_room_event
SET round_no = 0,
    business_event_id = CONCAT('migrated:', event_sequence, ':', visibility, ':', owner_player_id)
WHERE round_no IS NULL OR business_event_id IS NULL;

ALTER TABLE aoo_room_event
    MODIFY COLUMN round_no INT NOT NULL,
    MODIFY COLUMN business_event_id VARCHAR(128) NOT NULL,
    ADD UNIQUE KEY uq_room_round_business_view
        (room_id, round_no, business_event_id, visibility, owner_player_id),
    ADD KEY idx_room_round_type_sequence (room_id, round_no, event_type, event_sequence);
