-- Every account starts with 10,000 global CRYSTAL. Existing accounts are
-- normalized once so local/E2E users and newly registered users share the
-- same authoritative opening balance.
INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at)
SELECT account_id,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3)
FROM aoo_account
ON DUPLICATE KEY UPDATE
    balance=10000,
    version=aoo_currency_balance.version+1,
    updated_at=CURRENT_TIMESTAMP(3);

DROP TRIGGER IF EXISTS trg_aoo_account_default_crystal;
CREATE TRIGGER trg_aoo_account_default_crystal
AFTER INSERT ON aoo_account
FOR EACH ROW
INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at)
VALUES(NEW.account_id,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3));
