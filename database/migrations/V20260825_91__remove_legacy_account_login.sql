-- Execute only after V20260825_90 backfill and the unified Account binary are deployed.
-- This closes the old account.login_name write path; aliases now exist only as identities.
ALTER TABLE aoo_account DROP INDEX uk_aoo_account_login, DROP COLUMN login_name;
