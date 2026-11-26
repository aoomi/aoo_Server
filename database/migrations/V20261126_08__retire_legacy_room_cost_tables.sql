-- Retire the region/game-id room-cost authority after lossless archival.
-- Active pricing remains exclusively in the play registry and validated
-- aoo_room_card_price_<play_code> tables.

CREATE TABLE aoo_room_card_publication_history LIKE aoo_room_cost_publication;
ALTER TABLE aoo_room_card_publication_history COMMENT =
    'Immutable pre-play-code publication history; never an active authority';

CREATE TABLE aoo_room_card_policy_history LIKE aoo_room_cost_policy;
ALTER TABLE aoo_room_card_policy_history COMMENT =
    'Immutable pre-play-code policy history; excluded from pricing lookup';

CREATE TABLE aoo_room_card_authority_history LIKE aoo_room_cost_active;
ALTER TABLE aoo_room_card_authority_history COMMENT =
    'Immutable retired pointer history; play registry is current authority';

CREATE TABLE aoo_room_card_policy_provenance (
    publication_id BIGINT UNSIGNED NOT NULL,
    policy_key CHAR(64) NOT NULL,
    configuration_version VARCHAR(128) NOT NULL,
    workbook_manifest_hash CHAR(64) NOT NULL,
    authority_hash CHAR(64) NOT NULL,
    source_class VARCHAR(32) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    source_hash VARCHAR(256) NOT NULL,
    free_policy_ref VARCHAR(128) NULL,
    free_valid_from DATETIME(3) NULL,
    free_valid_until DATETIME(3) NULL,
    always_free BOOLEAN NOT NULL,
    PRIMARY KEY (publication_id, policy_key),
    CONSTRAINT fk_room_card_provenance_publication_history
        FOREIGN KEY (publication_id)
        REFERENCES aoo_room_card_publication_history(publication_id),
    CONSTRAINT chk_room_card_provenance_identity
        CHECK (publication_id > 0 AND release_id > 0 AND index_generation > 0),
    CONSTRAINT chk_room_card_provenance_source_class
        CHECK (source_class = 'CURRENT_PUBLISHED'),
    CONSTRAINT chk_room_card_provenance_free_window CHECK (
        (free_policy_ref IS NULL AND free_valid_from IS NULL AND free_valid_until IS NULL AND always_free = FALSE)
        OR (free_policy_ref IS NOT NULL AND always_free = TRUE AND free_valid_from IS NULL AND free_valid_until IS NULL)
        OR (free_policy_ref IS NOT NULL AND always_free = FALSE AND free_valid_from IS NOT NULL
            AND free_valid_until IS NOT NULL AND free_valid_until >= free_valid_from)
    )
) COMMENT = 'Immutable provenance for room-card policy snapshots';

INSERT INTO aoo_room_card_publication_history SELECT * FROM aoo_room_cost_publication;
INSERT INTO aoo_room_card_policy_history SELECT * FROM aoo_room_cost_policy;
INSERT INTO aoo_room_card_authority_history SELECT * FROM aoo_room_cost_active;
INSERT INTO aoo_room_card_policy_provenance (
    publication_id, policy_key, configuration_version, workbook_manifest_hash,
    authority_hash, source_class, release_id, index_generation, source_hash,
    free_policy_ref, free_valid_from, free_valid_until, always_free
)
SELECT publication_id, policy_key, configuration_version, workbook_manifest_hash,
       authority_hash, source_class, release_id, index_generation, source_hash,
       free_policy_ref, free_valid_from, free_valid_until, always_free
FROM aoo_room_cost_policy_provenance;

DELIMITER $$
CREATE PROCEDURE verify_room_card_history_before_retirement()
BEGIN
    IF (SELECT COUNT(*) FROM aoo_room_card_publication_history)
       <> (SELECT COUNT(*) FROM aoo_room_cost_publication) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'publication history count mismatch';
    END IF;
    IF (SELECT COUNT(*) FROM aoo_room_card_policy_history)
       <> (SELECT COUNT(*) FROM aoo_room_cost_policy) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'policy history count mismatch';
    END IF;
    IF (SELECT COUNT(*) FROM aoo_room_card_authority_history)
       <> (SELECT COUNT(*) FROM aoo_room_cost_active) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authority history count mismatch';
    END IF;
    IF (SELECT COUNT(*) FROM aoo_room_card_policy_provenance)
       <> (SELECT COUNT(*) FROM aoo_room_cost_policy_provenance) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'provenance count mismatch';
    END IF;
    IF EXISTS (
        SELECT 1 FROM aoo_room_cost_publication old
        LEFT JOIN aoo_room_card_publication_history archived
          ON archived.publication_id=old.publication_id
         AND archived.content_hash=old.content_hash
         AND archived.environment=old.environment
         AND archived.validated_at=old.validated_at
        WHERE archived.publication_id IS NULL
    ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'publication content mismatch'; END IF;
    IF EXISTS (
        SELECT 1 FROM aoo_room_cost_policy old
        LEFT JOIN aoo_room_card_policy_history archived
          ON archived.game_id=old.game_id AND archived.play_version=old.play_version
         AND archived.region_code=old.region_code AND archived.measure_kind=old.measure_kind
         AND archived.measure_value=old.measure_value AND archived.round_count=old.round_count
         AND archived.player_count=old.player_count AND archived.payer_mode=old.payer_mode
         AND archived.publication_id=old.publication_id AND archived.billing_kind=old.billing_kind
         AND archived.billing_context=old.billing_context AND archived.tier_minor=old.tier_minor
         AND archived.currency_code=old.currency_code AND archived.cost_minor=old.cost_minor
         AND archived.club_cost_minor <=> old.club_cost_minor
         AND archived.union_cost_minor <=> old.union_cost_minor
         AND archived.policy_version=old.policy_version AND archived.content_hash=old.content_hash
         AND archived.status=old.status AND archived.confirmation_status=old.confirmation_status
         AND archived.free_policy_ref <=> old.free_policy_ref
         AND archived.refund_policy_ref <=> old.refund_policy_ref
        WHERE archived.game_id IS NULL
    ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'policy content mismatch'; END IF;
    IF EXISTS (
        SELECT 1 FROM aoo_room_cost_policy_provenance old
        LEFT JOIN aoo_room_card_policy_provenance archived
          ON archived.publication_id=old.publication_id AND archived.policy_key=old.policy_key
         AND archived.configuration_version=old.configuration_version
         AND archived.workbook_manifest_hash=old.workbook_manifest_hash
         AND archived.authority_hash=old.authority_hash AND archived.source_class=old.source_class
         AND archived.release_id=old.release_id AND archived.index_generation=old.index_generation
         AND archived.source_hash=old.source_hash AND archived.free_policy_ref <=> old.free_policy_ref
         AND archived.free_valid_from <=> old.free_valid_from
         AND archived.free_valid_until <=> old.free_valid_until
         AND archived.always_free=old.always_free
        WHERE archived.publication_id IS NULL
    ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'provenance content mismatch'; END IF;
    IF EXISTS (
        SELECT 1 FROM aoo_room_cost_active old
        LEFT JOIN aoo_room_card_authority_history archived
          ON archived.pointer_name=old.pointer_name AND archived.publication_id=old.publication_id
         AND archived.version=old.version AND archived.activated_at=old.activated_at
        WHERE archived.pointer_name IS NULL
    ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authority content mismatch'; END IF;
    IF (SELECT COUNT(*) FROM aoo_room_card_play_registry WHERE status='ACTIVE') <> 32 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'active play registry incomplete';
    END IF;
END$$
DELIMITER ;

CALL verify_room_card_history_before_retirement();
DROP PROCEDURE verify_room_card_history_before_retirement;

DROP TABLE aoo_room_cost_policy_provenance;
DROP TABLE aoo_room_cost_active;
DROP TABLE aoo_room_cost_policy;
DROP TABLE aoo_room_cost_publication;
