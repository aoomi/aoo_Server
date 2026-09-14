ALTER TABLE aoo_room_event
    ADD COLUMN is_compensation BOOLEAN NOT NULL DEFAULT FALSE AFTER schema_version,
    ADD COLUMN compensates_business_event_id VARCHAR(128) NULL AFTER is_compensation,
    ADD COLUMN compensation_reason VARCHAR(500) NULL AFTER compensates_business_event_id,
    ADD CONSTRAINT chk_room_event_compensation CHECK (
        (is_compensation=FALSE AND compensates_business_event_id IS NULL AND compensation_reason IS NULL)
        OR
        (is_compensation=TRUE AND compensates_business_event_id IS NOT NULL AND compensation_reason IS NOT NULL)
    ),
    ADD KEY idx_room_event_compensation (room_id,compensates_business_event_id);

DELIMITER $$
CREATE TRIGGER trg_aoo_room_event_no_update
BEFORE UPDATE ON aoo_room_event
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='aoo_room_event is append-only; write a compensation event';
END$$

CREATE TRIGGER trg_aoo_room_event_no_delete
BEFORE DELETE ON aoo_room_event
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='aoo_room_event is append-only; retention must archive, not delete';
END$$
DELIMITER ;
