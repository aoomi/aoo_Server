-- R08: bind every configuration/index/room lock to the same immutable game
-- release.  The original independent foreign keys allowed a row to combine
-- game/play metadata from one release with the release_id of another game.

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth)
VALUES('CN-51-01','CN-51','CITY','CN','5101','成都','/CN/CN-51/CN-51-01',2),
      ('CN-43-11','CN-43','CITY','CN','4311','永州','/CN/CN-43/CN-43-11',2);

INSERT INTO aoo_region_alias(source_system,source_region_code,region_code,mapping_status,verified_at)
VALUES('bootstrap-catalog','sc','CN-51','VERIFIED',CURRENT_TIMESTAMP(3)),
      ('bootstrap-catalog','cd','CN-51-01','VERIFIED',CURRENT_TIMESTAMP(3)),
      ('bootstrap-catalog','yongzhou','CN-43-11','VERIFIED',CURRENT_TIMESTAMP(3));

ALTER TABLE aoo_game_release
    ADD UNIQUE KEY uk_game_release_identity (release_id,game_id,play_version);

ALTER TABLE aoo_compiled_room_create_index
    ADD UNIQUE KEY uk_compiled_index_release_identity
        (game_id,region_code,play_version,index_generation,release_id),
    ADD CONSTRAINT fk_compiled_index_release_identity
        FOREIGN KEY (release_id,game_id,play_version)
        REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT;

ALTER TABLE aoo_compiled_index_active
    DROP FOREIGN KEY fk_compiled_active_index,
    ADD CONSTRAINT fk_compiled_active_index_release
        FOREIGN KEY (game_id,region_code,play_version,index_generation,release_id)
        REFERENCES aoo_compiled_room_create_index
            (game_id,region_code,play_version,index_generation,release_id) ON DELETE RESTRICT;

ALTER TABLE aoo_room_rule_lock
    DROP FOREIGN KEY fk_room_rule_lock_index,
    ADD CONSTRAINT fk_room_rule_lock_index_release
        FOREIGN KEY (game_id,region_code,play_version,index_generation,release_id)
        REFERENCES aoo_compiled_room_create_index
            (game_id,region_code,play_version,index_generation,release_id) ON DELETE RESTRICT;

ALTER TABLE aoo_room_template_release_lock
    ADD CONSTRAINT fk_template_release_lock_identity
        FOREIGN KEY (release_id,game_id,play_version)
        REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT;

ALTER TABLE aoo_published_game_configuration
    ADD COLUMN release_id BIGINT UNSIGNED NULL AFTER play_version,
    ADD KEY idx_published_configuration_release (release_id,game_id,play_version),
    ADD CONSTRAINT fk_published_configuration_release_identity
        FOREIGN KEY (release_id,game_id,play_version)
        REFERENCES aoo_game_release(release_id,game_id,play_version) ON DELETE RESTRICT;

UPDATE aoo_published_game_configuration published
   SET release_id=(
       SELECT MAX(candidate.release_id)
         FROM aoo_game_release candidate
        WHERE candidate.game_id=published.game_id
          AND candidate.play_version=published.play_version
          AND candidate.status IN ('VALIDATED','ACTIVE','RETIRED','ROLLED_BACK')
   )
 WHERE release_id IS NULL;

DELIMITER $$
CREATE TRIGGER trg_published_configuration_release_required
BEFORE INSERT ON aoo_published_game_configuration
FOR EACH ROW
BEGIN
    DECLARE v_release_status VARCHAR(16) DEFAULT NULL;
    IF NEW.release_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published configuration requires an immutable release';
    END IF;
    SELECT MAX(status) INTO v_release_status
      FROM aoo_game_release
     WHERE release_id=NEW.release_id
       AND game_id=NEW.game_id
       AND play_version=NEW.play_version;
    IF v_release_status IS NULL OR v_release_status NOT IN ('VALIDATED','ACTIVE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='configuration release is not publishable';
    END IF;
END$$
DELIMITER ;

-- Migrations added after the original governance seed must not silently bypass
-- ownership, retention, legal-hold and backup-purge policy.  Classify every
-- current authoritative table by domain and fail the zero-gap view closed for
-- future migration reviews.
INSERT INTO aoo_table_governance(
    table_name,owner_code,data_classification,authoritative_source,
    hot_retention_days,archive_retention_days,purge_after_days,
    legal_hold_supported,anonymization_strategy,backup_purge_sla_days
)
SELECT tables.table_name,
       CASE
         WHEN tables.table_name LIKE '%club%' OR tables.table_name LIKE '%template%' THEN 'CLUB'
         WHEN tables.table_name LIKE '%ledger%' OR tables.table_name LIKE '%payment%'
           OR tables.table_name LIKE '%balance%' OR tables.table_name LIKE '%inventory%'
           OR tables.table_name LIKE '%gift%' THEN 'PLAYER_ASSET'
         WHEN tables.table_name LIKE '%room%' OR tables.table_name LIKE '%replay%'
           OR tables.table_name LIKE '%match%' OR tables.table_name LIKE '%tournament%' THEN 'GAME_RUNTIME'
         WHEN tables.table_name LIKE '%game%' OR tables.table_name LIKE '%component%'
           OR tables.table_name LIKE '%release%' OR tables.table_name LIKE '%configuration%'
           OR tables.table_name LIKE '%region%' THEN 'GAME_CATALOG'
         WHEN tables.table_name LIKE '%audit%' OR tables.table_name LIKE '%privacy%'
           OR tables.table_name LIKE '%risk%' OR tables.table_name LIKE '%auth%'
           OR tables.table_name LIKE '%session%' OR tables.table_name LIKE '%identity%' THEN 'SECURITY_PRIVACY'
         ELSE 'PLATFORM_DATA'
       END,
       CASE
         WHEN tables.table_name LIKE '%ledger%' OR tables.table_name LIKE '%payment%'
           OR tables.table_name LIKE '%balance%' OR tables.table_name LIKE '%audit%'
           OR tables.table_name LIKE '%privacy%' OR tables.table_name LIKE '%risk%'
           OR tables.table_name LIKE '%identity%' THEN 'RESTRICTED'
         WHEN tables.table_name LIKE '%game%' OR tables.table_name LIKE '%component%'
           OR tables.table_name LIKE '%release%' OR tables.table_name LIKE '%configuration%'
           OR tables.table_name LIKE '%region%' THEN 'INTERNAL'
         ELSE 'CONFIDENTIAL'
       END,
       'AOO_DB',365,2555,3650,1,'SUBJECT_ID_TOKENIZE',35
  FROM information_schema.tables tables
  LEFT JOIN aoo_table_governance governed ON governed.table_name=tables.table_name
 WHERE tables.table_schema=DATABASE()
   AND tables.table_type='BASE TABLE'
   AND governed.table_name IS NULL;
