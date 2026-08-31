ALTER TABLE aoo_room_create_saga
 ADD COLUMN compensation_origin_step VARCHAR(32) NULL AFTER step,
 ADD KEY idx_room_saga_compensation(compensation_origin_step,step,next_retry_at);
