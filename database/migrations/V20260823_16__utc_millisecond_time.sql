-- DATETIME(3) is the only persisted temporal representation; sessions are UTC.
ALTER TABLE aoo_connection_generation
  MODIFY updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- Database-owned lifecycle timestamps use CURRENT_TIMESTAMP(3); domain event time remains supplied by application Clock/Instant.
ALTER TABLE aoo_currency_balance
  MODIFY updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);
ALTER TABLE perspective_replay_event
  MODIFY created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);
