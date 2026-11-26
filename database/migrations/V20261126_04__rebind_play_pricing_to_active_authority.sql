-- Rebind the generated zero-price play-code policies to the current ACTIVE
-- release/index identity. Amounts and policy dimensions are intentionally
-- immutable in this migration.
DELIMITER $$
CREATE PROCEDURE rebind_play_pricing_to_active_authority_v20261126_04()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_play_code VARCHAR(32);
  DECLARE v_play_version VARCHAR(64);
  DECLARE v_table_name VARCHAR(64);
  DECLARE v_release_id BIGINT UNSIGNED;
  DECLARE v_index_generation BIGINT UNSIGNED;
  DECLARE v_source_hash CHAR(64);
  DECLARE v_registry_count INT DEFAULT 0;
  DECLARE v_bound_count INT DEFAULT 0;
  DECLARE pricing_cursor CURSOR FOR
    SELECT r.play_code,r.play_version,r.table_name,a.release_id,a.index_generation,rel.source_hash
      FROM aoo_room_card_play_registry r
      JOIN aoo_compiled_index_active a
        ON a.game_id=r.game_id AND a.play_version=r.play_version
      JOIN aoo_game_release rel ON rel.release_id=a.release_id
     WHERE r.status='ACTIVE'
     ORDER BY r.play_code;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;

  START TRANSACTION;
  SELECT COUNT(*) INTO v_registry_count
    FROM aoo_room_card_play_registry r
    JOIN aoo_compiled_index_active a
      ON a.game_id=r.game_id AND a.play_version=r.play_version
    JOIN aoo_game_release rel ON rel.release_id=a.release_id
   WHERE r.status='ACTIVE'
     AND r.runtime_source_hash=rel.source_hash;
  IF v_registry_count<>32 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='play pricing ACTIVE authority precondition failed';
  END IF;

  OPEN pricing_cursor;
  pricing_loop: LOOP
    FETCH pricing_cursor INTO v_play_code,v_play_version,v_table_name,v_release_id,v_index_generation,v_source_hash;
    IF done=1 THEN LEAVE pricing_loop; END IF;
    IF v_play_code NOT REGEXP '^[a-z][a-z0-9]{2,31}$'
       OR v_table_name<>CONCAT('aoo_room_card_price_',v_play_code) THEN
      CLOSE pricing_cursor;
      ROLLBACK;
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unsafe play pricing table identity';
    END IF;

    SET @pricing_rows=0;
    SET @pricing_zero_rows=0;
    SET @pricing_sql=CONCAT(
      'SELECT COUNT(*),SUM(amount_minor=0 AND status=''ACTIVE'' AND confirmation_status=''CONFIRMED'') ',
      'INTO @pricing_rows,@pricing_zero_rows FROM `',v_table_name,'` ',
      'WHERE play_code=',QUOTE(v_play_code),' AND play_version=',QUOTE(v_play_version));
    PREPARE pricing_stmt FROM @pricing_sql;
    EXECUTE pricing_stmt;
    DEALLOCATE PREPARE pricing_stmt;
    IF @pricing_rows=0 OR @pricing_rows<>@pricing_zero_rows THEN
      CLOSE pricing_cursor;
      ROLLBACK;
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='play pricing zero-price confirmation precondition failed';
    END IF;

    SET @pricing_sql=CONCAT(
      'UPDATE `',v_table_name,'` SET release_id=',v_release_id,
      ',index_generation=',v_index_generation,
      ',runtime_source_hash=',QUOTE(v_source_hash),
      ' WHERE play_code=',QUOTE(v_play_code),' AND play_version=',QUOTE(v_play_version));
    PREPARE pricing_stmt FROM @pricing_sql;
    EXECUTE pricing_stmt;
    DEALLOCATE PREPARE pricing_stmt;

    UPDATE aoo_room_card_play_registry
       SET release_id=v_release_id,index_generation=v_index_generation,runtime_source_hash=v_source_hash
     WHERE play_code=v_play_code AND play_version=v_play_version AND status='ACTIVE';
  END LOOP;
  CLOSE pricing_cursor;

  SELECT COUNT(*) INTO v_bound_count
    FROM aoo_room_card_play_registry r
    JOIN aoo_compiled_index_active a
      ON a.game_id=r.game_id AND a.play_version=r.play_version
    JOIN aoo_game_release rel ON rel.release_id=a.release_id
   WHERE r.status='ACTIVE'
     AND r.release_id=a.release_id
     AND r.index_generation=a.index_generation
     AND r.runtime_source_hash=rel.source_hash;
  IF v_bound_count<>32 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='play pricing ACTIVE authority verification failed';
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL rebind_play_pricing_to_active_authority_v20261126_04();
DROP PROCEDURE rebind_play_pricing_to_active_authority_v20261126_04;
