ALTER TABLE aoo_room_authority_route
  ADD COLUMN last_business_activity_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    AFTER first_round_started_at,
  ADD KEY idx_room_authority_inactivity_expiry(
    lifecycle_state,first_round_started_at,last_business_activity_at,room_id
  );

-- Existing active rooms receive a fresh twelve-hour window when this policy is deployed.
UPDATE aoo_room_authority_route
SET last_business_activity_at=CURRENT_TIMESTAMP(3)
WHERE lifecycle_state IN ('CREATING','RECOVERING','ACTIVE');
