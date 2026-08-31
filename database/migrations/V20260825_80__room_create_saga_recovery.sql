ALTER TABLE aoo_room_create_saga
 ADD COLUMN request_json JSON NULL AFTER request_hash,
 ADD COLUMN processing_token VARCHAR(64) NULL AFTER trace_id,
 ADD COLUMN processing_until DATETIME(3) NULL AFTER processing_token,
 ADD KEY idx_room_saga_processing(processing_until,step,updated_at);
UPDATE aoo_room_create_saga SET request_json=JSON_OBJECT('roomId',room_id,'gameId',game_id,'playVersion',play_version,'stateVersion',1,'rules',JSON_OBJECT(),'_traceId',trace_id) WHERE request_json IS NULL AND step NOT IN('CONFIRMED','COMPENSATED');
