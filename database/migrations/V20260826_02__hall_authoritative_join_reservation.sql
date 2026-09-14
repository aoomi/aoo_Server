-- A Hall member is not allowed to receive a game ticket until the independent
-- Room Authority has accepted the same account/seat reservation.
ALTER TABLE aoo_hall_room_member
  DROP CHECK chk_hall_member_status,
  ADD CONSTRAINT chk_hall_member_status CHECK(status IN('JOINING','JOINED','LEFT','KICKED'));
