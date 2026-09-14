-- Personal and club rooms share one Hall saga and one authority lifecycle.
ALTER TABLE aoo_hall_room
    ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL' AFTER owner_account_id,
    ADD COLUMN club_id BIGINT UNSIGNED NULL AFTER scope_type,
    ADD COLUMN club_template_code VARCHAR(64) NULL AFTER club_id,
    ADD KEY idx_hall_room_club (club_id, state, updated_at, room_id),
    ADD CONSTRAINT chk_hall_room_scope CHECK (
        (scope_type = 'PERSONAL' AND club_id IS NULL AND club_template_code IS NULL)
        OR (scope_type = 'CLUB' AND club_id IS NOT NULL AND club_id > 0 AND club_template_code IS NOT NULL)
    );
