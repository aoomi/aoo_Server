-- Canonical runtime family codes are stable protocol values. Settlement
-- template fields remain optional; clients derive the default from family.
-- Published release and compiled-index content is immutable, so active rows
-- are replaced by a new release/index generation instead of being mutated.
ALTER TABLE aoo_game_family DROP CHECK chk_game_family_code;
ALTER TABLE aoo_game_family ADD CONSTRAINT chk_game_family_code
    CHECK (family_code REGEXP '^[A-Za-z][A-Za-z0-9:_-]{0,63}$');

INSERT INTO aoo_game_family(family_code, category_code, display_name, status) VALUES
('poker:pao-de-kuai', 'POKER', '跑得快', 'ACTIVE'),
('poker:compare-hand', 'POKER', '比牌', 'ACTIVE'),
('poker:generic-card-round', 'POKER', '通用扑克局', 'ACTIVE')
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), status='ACTIVE';

UPDATE aoo_game_catalog
SET family_code = CASE game_id
        WHEN 629 THEN 'poker:pao-de-kuai'
        WHEN 618 THEN 'poker:pao-de-kuai'
        WHEN 9 THEN 'poker:compare-hand'
        WHEN 62 THEN 'poker:generic-card-round'
    END
WHERE game_id IN (9, 62, 618, 629);

UPDATE aoo_published_game_configuration
SET configuration_payload = JSON_SET(
        JSON_REMOVE(configuration_payload, '$.smallSettleTemplate'),
        '$.playFamily',
        CASE game_id
            WHEN 629 THEN 'poker:pao-de-kuai'
            WHEN 618 THEN 'poker:pao-de-kuai'
            WHEN 9 THEN 'poker:compare-hand'
            WHEN 62 THEN 'poker:generic-card-round'
        END)
WHERE game_id IN (9, 62, 618, 629);

DROP PROCEDURE IF EXISTS sp_aoo_migrate_poker_profile;
DELIMITER $$
CREATE PROCEDURE sp_aoo_migrate_poker_profile(
    IN p_game_id BIGINT UNSIGNED,
    IN p_family VARCHAR(64),
    IN p_big_template VARCHAR(64)
)
profile: BEGIN
    DECLARE v_source_release_id BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_source_release_version BIGINT UNSIGNED;
    DECLARE v_new_release_id BIGINT UNSIGNED;
    DECLARE v_new_release_version BIGINT UNSIGNED;
    DECLARE v_source_catalog JSON;
    DECLARE v_source_rule JSON;
    DECLARE v_source_ui JSON;
    DECLARE v_new_catalog JSON;
    DECLARE v_new_rule JSON;
    DECLARE v_new_ui JSON;
    DECLARE v_index_change INT DEFAULT 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_source_release_id = NULL;

    SELECT release_id, release_version, catalog_snapshot, rule_snapshot, ui_snapshot
      INTO v_source_release_id, v_source_release_version, v_source_catalog, v_source_rule, v_source_ui
      FROM aoo_game_release
     WHERE game_id = p_game_id AND status = 'ACTIVE'
     ORDER BY release_version DESC
     LIMIT 1;
    IF v_source_release_id IS NULL THEN LEAVE profile; END IF;

    SET v_new_catalog = JSON_SET(v_source_catalog, '$.family', p_family);
    SET v_new_rule = JSON_SET(v_source_rule, '$.family', p_family);
    SET v_new_ui = JSON_SET(JSON_REMOVE(v_source_ui, '$.smallSettleTemplate'), '$.playFamily', p_family);
    IF p_big_template IS NOT NULL AND p_big_template <> '' THEN
        SET v_new_ui = JSON_SET(v_new_ui, '$.bigSettleTemplate', p_big_template);
    END IF;

    SELECT COUNT(*) INTO v_index_change
      FROM aoo_compiled_index_active a
      JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code
       AND i.play_version=a.play_version AND i.index_generation=a.index_generation
     WHERE a.game_id=p_game_id
       AND (COALESCE(JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.playFamily')),'') <> p_family
         OR JSON_EXTRACT(i.ui_schema,'$.smallSettleTemplate') IS NOT NULL
         OR (p_big_template IS NOT NULL AND p_big_template <> ''
             AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.bigSettleTemplate')),'') <> p_big_template));

    IF JSON_CONTAINS(v_source_catalog, v_new_catalog)
       AND JSON_CONTAINS(v_new_catalog, v_source_catalog)
       AND JSON_CONTAINS(v_source_rule, v_new_rule)
       AND JSON_CONTAINS(v_new_rule, v_source_rule)
       AND JSON_CONTAINS(v_source_ui, v_new_ui)
       AND JSON_CONTAINS(v_new_ui, v_source_ui)
       AND v_index_change = 0 THEN
        LEAVE profile;
    END IF;

    SET v_new_release_version = v_source_release_version + 1;
    SET v_new_release_id = p_game_id * 1000000 + v_new_release_version;

    INSERT INTO aoo_game_release(
        release_id,game_id,play_version,release_version,release_scope,
        catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
        catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
        status,rollout_percent,created_by,reason,validated_at,activated_at)
    SELECT v_new_release_id,game_id,play_version,v_new_release_version,release_scope,
           v_new_catalog,v_new_rule,v_new_ui,component_snapshot,
           SHA2(CAST(v_new_catalog AS CHAR),256),SHA2(CAST(v_new_rule AS CHAR),256),
           SHA2(CAST(v_new_ui AS CHAR),256),component_hash,
           SHA2(CONCAT(bundle_hash,'|canonical-settlement-20260826.06|',p_family,'|',COALESCE(p_big_template,'')),256),
           'ACTIVE',rollout_percent,1,'Versioned canonical Poker family and settlement defaults',
           CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
      FROM aoo_game_release WHERE release_id=v_source_release_id;

    INSERT INTO aoo_game_release_component(
        release_id,component_type,ordinal,component_id,component_key,component_version,
        spi_type,implementation_locator,artifact_digest,parameter_snapshot,content_hash)
    SELECT v_new_release_id,component_type,ordinal,component_id,component_key,component_version,
           spi_type,implementation_locator,artifact_digest,parameter_snapshot,content_hash
      FROM aoo_game_release_component WHERE release_id=v_source_release_id;

    INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
    SELECT v_new_release_id,region_code,rollout_percent,'ACTIVE'
      FROM aoo_game_release_region WHERE release_id=v_source_release_id;

    INSERT INTO aoo_compiled_room_create_index(
        game_id,region_code,play_version,index_generation,release_id,
        component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
        lifecycle_state,compiled_at,validated_at,activated_at)
    SELECT i.game_id,i.region_code,i.play_version,i.index_generation+1,v_new_release_id,
           i.component_chain,i.rule_validator,
           IF(p_big_template IS NULL OR p_big_template='',
              JSON_SET(JSON_REMOVE(i.ui_schema,'$.smallSettleTemplate'),'$.playFamily',p_family),
              JSON_SET(JSON_REMOVE(i.ui_schema,'$.smallSettleTemplate'),'$.playFamily',p_family,'$.bigSettleTemplate',p_big_template)),
           SHA2(CAST(IF(p_big_template IS NULL OR p_big_template='',
              JSON_SET(JSON_REMOVE(i.ui_schema,'$.smallSettleTemplate'),'$.playFamily',p_family),
              JSON_SET(JSON_REMOVE(i.ui_schema,'$.smallSettleTemplate'),'$.playFamily',p_family,'$.bigSettleTemplate',p_big_template)) AS CHAR),256),
           SHA2(CONCAT(i.bundle_hash,'|canonical-settlement-20260826.06|',p_family,'|',COALESCE(p_big_template,'')),256),
           'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
      FROM aoo_compiled_index_active a
      JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code
       AND i.play_version=a.play_version AND i.index_generation=a.index_generation
     WHERE a.game_id=p_game_id AND a.release_id=v_source_release_id;

    UPDATE aoo_game_release_region SET status='RETIRED'
     WHERE release_id=v_source_release_id AND status='ACTIVE';
    UPDATE aoo_compiled_room_create_index i
      JOIN aoo_compiled_index_active a
        ON a.game_id=i.game_id AND a.region_code=i.region_code
       AND a.play_version=i.play_version AND a.index_generation=i.index_generation
       SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
     WHERE a.game_id=p_game_id AND a.release_id=v_source_release_id;
    UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
     WHERE release_id=v_source_release_id;

    UPDATE aoo_compiled_index_active a
      JOIN aoo_compiled_room_create_index n
        ON n.game_id=a.game_id AND n.region_code=a.region_code
       AND n.play_version=a.play_version AND n.release_id=v_new_release_id
       SET a.index_generation=n.index_generation,a.release_id=v_new_release_id,
           a.cache_epoch=a.cache_epoch+1,a.activated_by=1,
           a.activation_reason='Versioned canonical Poker family and settlement defaults',
           a.activated_at=CURRENT_TIMESTAMP(3)
     WHERE a.game_id=p_game_id AND a.release_id=v_source_release_id;

    INSERT IGNORE INTO aoo_game_release_audit(
        audit_id,release_id,action,operator_id,request_id,reason,before_status,after_status,
        before_active_release_id,after_active_release_id,occurred_at)
    VALUES(v_new_release_id,v_new_release_id,'ACTIVATE',1,
        CONCAT('migration-20260826.06-',p_game_id),
        'Versioned canonical Poker family and settlement defaults','ACTIVE','ACTIVE',
        v_source_release_id,v_new_release_id,CURRENT_TIMESTAMP(3));
END$$
DELIMITER ;

CALL sp_aoo_migrate_poker_profile(629,'poker:pao-de-kuai','BigSettleTpl_110');
CALL sp_aoo_migrate_poker_profile(618,'poker:pao-de-kuai',NULL);
CALL sp_aoo_migrate_poker_profile(9,'poker:compare-hand',NULL);
CALL sp_aoo_migrate_poker_profile(62,'poker:generic-card-round',NULL);
DROP PROCEDURE sp_aoo_migrate_poker_profile;
