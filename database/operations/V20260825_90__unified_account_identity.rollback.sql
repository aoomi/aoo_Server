-- Manual rollback only; deliberately outside Flyway migrations to avoid duplicate version execution.
DELIMITER $$
CREATE PROCEDURE aoo_rollback_unified_identity()
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='aoo_account' AND column_name='login_name') THEN
    ALTER TABLE aoo_account ADD COLUMN login_name VARCHAR(64) NULL, ADD UNIQUE KEY uk_aoo_account_login(login_name);
  END IF;
  IF EXISTS(SELECT 1 FROM aoo_account_identity WHERE status='ACTIVE' AND identity_type <> 'ALIAS') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='rollback blocked: active non-alias identities exist';
  END IF;
  UPDATE aoo_account a JOIN aoo_account_identity i ON i.account_id=a.account_id
    SET a.login_name=i.normalized_value
    WHERE i.identity_type='ALIAS' AND i.status='ACTIVE';
  DROP TABLE aoo_identity_migration;
  DROP TABLE aoo_device_challenge;
  DROP TABLE aoo_device_pin;
  DROP TABLE aoo_identity_admin_request;
  DROP TABLE aoo_display_id_history;
  DROP TABLE aoo_account_identity;
  DROP TABLE aoo_display_id_segment;
END$$
DELIMITER ;
CALL aoo_rollback_unified_identity();
DROP PROCEDURE aoo_rollback_unified_identity;
