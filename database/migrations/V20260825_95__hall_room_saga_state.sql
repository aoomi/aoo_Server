-- RoomCreateSaga persists a room before the Gateway authority is created. Keep
-- that transition explicit and fail closed for every state outside the room
-- lifecycle contract.
ALTER TABLE aoo_hall_room
    DROP CHECK chk_hall_room_state,
    ADD CONSTRAINT chk_hall_room_state
        CHECK (state IN ('CREATING', 'OPEN', 'PLAYING', 'FINISHED', 'DISSOLVED'));
