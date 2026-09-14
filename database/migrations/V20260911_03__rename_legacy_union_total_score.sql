DROP PROCEDURE IF EXISTS migrate_legacy_union_total_score;
DELIMITER $$
CREATE PROCEDURE migrate_legacy_union_total_score()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'bigUnion' AND column_name = 'initSports'
    ) THEN
        SET @invalid_union_total_score = 0;
        SET @validate_union_total_score_sql =
            'SELECT COUNT(*) INTO @invalid_union_total_score FROM `bigUnion` WHERE `initSports` < 0 OR `initSports` > 999999999 OR `initSports` <> FLOOR(`initSports`)';
        PREPARE validate_union_total_score_stmt FROM @validate_union_total_score_sql;
        EXECUTE validate_union_total_score_stmt;
        DEALLOCATE PREPARE validate_union_total_score_stmt;

        IF @invalid_union_total_score > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bigUnion.initSports contains invalid alliance total scores';
        END IF;

        ALTER TABLE `bigUnion`
            CHANGE COLUMN `initSports` `union_total_score` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '联盟总分';
    END IF;
END$$
DELIMITER ;

CALL migrate_legacy_union_total_score();
DROP PROCEDURE migrate_legacy_union_total_score;
