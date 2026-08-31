-- MySQL 8.0.16+ enforced domain checks. Older engines are rejected by startup readiness.
ALTER TABLE aoo_business_idempotency
  ADD CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING','COMPLETED'));
ALTER TABLE aoo_room_event
  ADD CONSTRAINT chk_room_event_identity CHECK (room_id > 0 AND round_no >= 0 AND event_sequence >= 0),
  ADD CONSTRAINT chk_room_event_visibility CHECK ((visibility IN ('PUBLIC','LEGACY_RESTRICTED') AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id>0));
ALTER TABLE aoo_room_snapshot
  ADD CONSTRAINT chk_snapshot_identity CHECK (room_id > 0 AND fencing_token > 0 AND state_version >= 0 AND last_event_sequence = state_version);
ALTER TABLE aoo_room_lease
  ADD CONSTRAINT chk_lease_identity CHECK (room_id > 0 AND fencing_token > 0);
ALTER TABLE aoo_settlement
  ADD CONSTRAINT chk_settlement_identity CHECK (settlement_id > 0 AND room_id > 0 AND round_no >= 0);
ALTER TABLE aoo_ledger
  ADD CONSTRAINT chk_ledger_identity CHECK (ledger_id > 0 AND player_id > 0 AND delta <> 0);
ALTER TABLE aoo_club_member
  ADD CONSTRAINT chk_club_member_identity CHECK (club_id > 0 AND player_id > 0),
  ADD CONSTRAINT chk_club_member_status CHECK (member_status IN ('ACTIVE','LEFT','BANNED') AND online IN (0,1));
ALTER TABLE aoo_play_variant
  ADD CONSTRAINT chk_play_variant CHECK (game_id > 0 AND rule_schema_version > 0 AND status IN ('DRAFT','ACTIVE','RETIRED'));
ALTER TABLE aoo_room_template
  ADD CONSTRAINT chk_room_template CHECK (club_id > 0 AND game_id > 0 AND template_version > 0 AND created_by > 0 AND status IN ('DRAFT','ACTIVE','RETIRED'));
