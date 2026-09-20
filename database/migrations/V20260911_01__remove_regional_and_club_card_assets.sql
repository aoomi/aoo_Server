-- DESTRUCTIVE: the product is in development and circle-card assets have been retired.
-- The deployment must back up the development database before Flyway applies this migration.

DELETE FROM aoo_ledger WHERE currency = 'CITY_ROOM_CARD';
DELETE FROM aoo_currency_balance WHERE currency = 'CITY_ROOM_CARD';
DELETE FROM aoo_currency_catalog WHERE currency_code = 'CITY_ROOM_CARD';

ALTER TABLE playerWallet ADD COLUMN accountID BIGINT NOT NULL DEFAULT 0 AFTER id;
UPDATE playerWallet wallet
JOIN player owner ON owner.id = wallet.pid
SET wallet.accountID = owner.accountID;
DELETE FROM playerWallet WHERE accountID <= 0;
ALTER TABLE playerWallet
    DROP INDEX uk_player_wallet_pid,
    ADD UNIQUE KEY uk_player_wallet_account (accountID),
    DROP COLUMN pid;

DROP TABLE IF EXISTS playerCityCurrency;
DROP TABLE IF EXISTS quarantine_player_city_currency_legacy;
DROP TABLE IF EXISTS playerClub;
DROP TABLE IF EXISTS db_clubroomcard;
DROP TABLE IF EXISTS PlayerClubCardLog;
DROP TABLE IF EXISTS FamilyCardChargeLog;
DROP TABLE IF EXISTS ClubCardWinnerRebateLog;
DROP TABLE IF EXISTS ClubRoomCardChargeLog;
DROP TABLE IF EXISTS UnionRoomCardChargeLog;

DROP PROCEDURE IF EXISTS drop_retired_circle_card_column;
DELIMITER $$
CREATE PROCEDURE drop_retired_circle_card_column(IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @drop_circle_card_column_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` DROP COLUMN `', REPLACE(column_name_value, '`', '``'), '`'
        );
        PREPARE drop_circle_card_column_statement FROM @drop_circle_card_column_sql;
        EXECUTE drop_circle_card_column_statement;
        DEALLOCATE PREPARE drop_circle_card_column_statement;
    END IF;
END$$
DELIMITER ;

CALL drop_retired_circle_card_column('family', 'clubCardNum');
CALL drop_retired_circle_card_column('clubMember', 'clubRoomCard');
CALL drop_retired_circle_card_column('PlayerRoomAlone', 'clubCostType');
CALL drop_retired_circle_card_column('PlayerRoomLog', 'clubCostType');
DROP PROCEDURE drop_retired_circle_card_column;
