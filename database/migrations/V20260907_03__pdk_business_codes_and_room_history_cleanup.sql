-- One-time authorized room/history reset and PDK business-code convergence.
-- Accounts, authentication, wallets, ledger, clubs, club members, orders and
-- operational configuration are deliberately outside the delete set.

CREATE TABLE IF NOT EXISTS aoo_room_history_cleanup_run (
    run_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    account_rows_before BIGINT UNSIGNED NOT NULL,
    ledger_rows_before BIGINT UNSIGNED NOT NULL,
    club_rows_before BIGINT UNSIGNED NOT NULL,
    club_member_rows_before BIGINT UNSIGNED NOT NULL,
    account_rows_after BIGINT UNSIGNED NULL,
    ledger_rows_after BIGINT UNSIGNED NULL,
    club_rows_after BIGINT UNSIGNED NULL,
    club_member_rows_after BIGINT UNSIGNED NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (run_id),
    CONSTRAINT chk_room_cleanup_status CHECK (status IN ('RUNNING','SUCCEEDED','FAILED'))
);

CREATE TABLE IF NOT EXISTS aoo_room_history_cleanup_count (
    run_id VARCHAR(64) NOT NULL,
    phase VARCHAR(8) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    row_filter VARCHAR(500) NOT NULL,
    row_count BIGINT UNSIGNED NOT NULL,
    recorded_at DATETIME(3) NOT NULL,
    PRIMARY KEY (run_id,phase,table_name),
    CONSTRAINT fk_room_cleanup_count_run FOREIGN KEY (run_id)
      REFERENCES aoo_room_history_cleanup_run(run_id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_cleanup_count_phase CHECK (phase IN ('BEFORE','AFTER'))
);

-- Maintenance bypass is session-local and defaults closed for every normal session.
DROP TRIGGER IF EXISTS trg_aoo_room_event_no_delete;
DELIMITER $$
CREATE TRIGGER trg_aoo_room_event_no_delete BEFORE DELETE ON aoo_room_event
FOR EACH ROW
BEGIN
    IF COALESCE(@aoo_authorized_room_cleanup,0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='aoo_room_event is append-only; retention must archive, not delete';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_room_rule_lock_no_delete;
DELIMITER $$
CREATE TRIGGER trg_room_rule_lock_no_delete BEFORE DELETE ON aoo_room_rule_lock
FOR EACH ROW
BEGIN
    IF COALESCE(@aoo_authorized_room_cleanup,0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room rule lock is retention controlled';
    END IF;
END$$
DELIMITER ;

-- Existing catalog entries still use the legacy lowercase grammar. New stable
-- business codes are the only uppercase form allowed.
ALTER TABLE aoo_game_catalog DROP CHECK chk_game_catalog_code;
ALTER TABLE aoo_game_catalog ADD CONSTRAINT chk_game_catalog_code CHECK (
    REGEXP_LIKE(game_code,'^[a-z][a-z0-9_]{0,63}$')
    OR REGEXP_LIKE(game_code,'^[A-Z]{2}[1-5][0-9]{2}$')
);

DROP TRIGGER IF EXISTS trg_pdk_business_code_insert;
DELIMITER $$
CREATE TRIGGER trg_pdk_business_code_insert BEFORE INSERT ON aoo_game_catalog
FOR EACH ROW
BEGIN
    IF (NEW.game_id=8 AND NEW.game_code<>'CD201')
       OR (NEW.game_id=629 AND NEW.game_code<>'NJ201')
       OR (NEW.game_id=90005 AND NEW.game_code<>'LS201') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PDK catalog rows require CD201/NJ201/LS201';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_pdk_business_code_update;
DELIMITER $$
CREATE TRIGGER trg_pdk_business_code_update BEFORE UPDATE ON aoo_game_catalog
FOR EACH ROW
BEGIN
    IF (NEW.game_id=8 AND NEW.game_code<>'CD201')
       OR (NEW.game_id=629 AND NEW.game_code<>'NJ201')
       OR (NEW.game_id=90005 AND NEW.game_code<>'LS201') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PDK catalog rows require CD201/NJ201/LS201';
    END IF;
END$$
DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS assert_pdk_code_room_cleanup$$
CREATE PROCEDURE assert_pdk_code_room_cleanup()
BEGIN
    IF EXISTS(SELECT 1 FROM aoo_room_history_cleanup_count WHERE run_id=@aoo_room_cleanup_run AND phase='AFTER' AND row_count<>0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room/history cleanup left residual rows';
    END IF;
    IF (SELECT COUNT(*) FROM aoo_game_catalog WHERE (game_id=8 AND game_code='CD201') OR (game_id=629 AND game_code='NJ201') OR (game_id=90005 AND game_code='LS201'))<>3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PDK catalog code convergence failed';
    END IF;
    IF EXISTS(SELECT 1 FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=a.release_id
      WHERE a.game_id IN(8,629,90005) AND JSON_UNQUOTE(JSON_EXTRACT(r.catalog_snapshot,'$.gameCode'))
        <> CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='active PDK publication still exposes an old code';
    END IF;
    IF EXISTS(SELECT 1 FROM aoo_room_history_cleanup_run WHERE run_id=@aoo_room_cleanup_run AND
      (account_rows_before<>account_rows_after OR ledger_rows_before<>ledger_rows_after
       OR club_rows_before<>club_rows_after OR club_member_rows_before<>club_member_rows_after)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='preserved account/ledger/club rows changed';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;
SET @aoo_authorized_room_cleanup=1;
SET @aoo_room_cleanup_run='20260907-pdk-code-reset-v1';

INSERT INTO aoo_room_history_cleanup_run(
    run_id,reason,status,account_rows_before,ledger_rows_before,club_rows_before,
    club_member_rows_before,started_at)
SELECT @aoo_room_cleanup_run,
       'User-authorized full historical and active room cleanup before CD201/NJ201/LS201 activation',
       'RUNNING',(SELECT COUNT(*) FROM aoo_account),(SELECT COUNT(*) FROM aoo_ledger),
       (SELECT COUNT(*) FROM aoo_club_state),(SELECT COUNT(*) FROM aoo_club_member),CURRENT_TIMESTAMP(3);

INSERT INTO aoo_room_history_cleanup_count(run_id,phase,table_name,row_filter,row_count,recorded_at)
SELECT @aoo_room_cleanup_run,'BEFORE',table_name,row_filter,row_count,CURRENT_TIMESTAMP(3)
FROM (
 SELECT 'aoo_hall_room' table_name,'all rows' row_filter,COUNT(*) row_count FROM aoo_hall_room
 UNION ALL SELECT 'aoo_hall_room_member','all rows',COUNT(*) FROM aoo_hall_room_member
 UNION ALL SELECT 'aoo_hall_game_ticket','all rows',COUNT(*) FROM aoo_hall_game_ticket
 UNION ALL SELECT 'aoo_spectator_admission','all rows',COUNT(*) FROM aoo_spectator_admission
 UNION ALL SELECT 'aoo_game_share','all rows',COUNT(*) FROM aoo_game_share
 UNION ALL SELECT 'aoo_room_rule_lock','all rows',COUNT(*) FROM aoo_room_rule_lock
 UNION ALL SELECT 'aoo_room_authority_route','all rows',COUNT(*) FROM aoo_room_authority_route
 UNION ALL SELECT 'aoo_room_billing_reservation','all rows; billing ledger preserved',COUNT(*) FROM aoo_room_billing_reservation
 UNION ALL SELECT 'aoo_room_create_saga','all rows',COUNT(*) FROM aoo_room_create_saga
 UNION ALL SELECT 'aoo_room_snapshot','all rows',COUNT(*) FROM aoo_room_snapshot
 UNION ALL SELECT 'aoo_room_lease','all rows',COUNT(*) FROM aoo_room_lease
 UNION ALL SELECT 'aoo_room_event','all rows',COUNT(*) FROM aoo_room_event
 UNION ALL SELECT 'aoo_room_idempotent_allocation','all rows',COUNT(*) FROM aoo_room_idempotent_allocation
 UNION ALL SELECT 'aoo_connection_generation','all rows',COUNT(*) FROM aoo_connection_generation
 UNION ALL SELECT 'aoo_club_room_projection','all rows; club body/member preserved',COUNT(*) FROM aoo_club_room_projection
 UNION ALL SELECT 'aoo_settlement_score','all rows',COUNT(*) FROM aoo_settlement_score
 UNION ALL SELECT 'aoo_settlement_outbox','all rows',COUNT(*) FROM aoo_settlement_outbox
 UNION ALL SELECT 'aoo_settlement','all rows',COUNT(*) FROM aoo_settlement
 UNION ALL SELECT 'replay_short_code_access','all rows',COUNT(*) FROM replay_short_code_access
 UNION ALL SELECT 'replay_short_code','all rows',COUNT(*) FROM replay_short_code
 UNION ALL SELECT 'perspective_replay_event','all rows',COUNT(*) FROM perspective_replay_event
 UNION ALL SELECT 'perspective_replay_event_archive','all rows',COUNT(*) FROM perspective_replay_event_archive
 UNION ALL SELECT 'replay_participant','all rows',COUNT(*) FROM replay_participant
 UNION ALL SELECT 'replay_participant_archive','all rows',COUNT(*) FROM replay_participant_archive
 UNION ALL SELECT 'replay_set_manifest','all rows',COUNT(*) FROM replay_set_manifest
 UNION ALL SELECT 'aoo_hall_idempotency','all room operations',COUNT(*) FROM aoo_hall_idempotency
 UNION ALL SELECT 'aoo_business_idempotency','room_id>0',COUNT(*) FROM aoo_business_idempotency WHERE room_id>0
 UNION ALL SELECT 'aoo_match_member','members of all room matches',COUNT(*) FROM aoo_match_member
 UNION ALL SELECT 'aoo_match','all rows',COUNT(*) FROM aoo_match
 UNION ALL SELECT 'room_safety_location','all rows',COUNT(*) FROM room_safety_location
 UNION ALL SELECT 'room_safety_mute','all rows',COUNT(*) FROM room_safety_mute
 UNION ALL SELECT 'room_safety_report','room_id IS NOT NULL',COUNT(*) FROM room_safety_report WHERE room_id IS NOT NULL
 UNION ALL SELECT 'aoo_player_risk_observation','room_id IS NOT NULL',COUNT(*) FROM aoo_player_risk_observation WHERE room_id IS NOT NULL
 UNION ALL SELECT 'risk_admission_decision','room_id IS NOT NULL',COUNT(*) FROM risk_admission_decision WHERE room_id IS NOT NULL
 UNION ALL SELECT 'risk_review_directive','directives for room-derived risk events',COUNT(*) FROM risk_review_directive d JOIN risk_event e ON e.risk_event_id=d.risk_event_id WHERE e.room_id IS NOT NULL OR e.source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL)
 UNION ALL SELECT 'risk_event','room_id or room telemetry source',COUNT(*) FROM risk_event WHERE room_id IS NOT NULL OR source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL)
 UNION ALL SELECT 'social_presence','detach room_id only',COUNT(*) FROM social_presence WHERE room_id IS NOT NULL
 UNION ALL SELECT 'telemetry_signal','room_id IS NOT NULL',COUNT(*) FROM telemetry_signal WHERE room_id IS NOT NULL
 UNION ALL SELECT 'aoo_outbox','LOWER(aggregate_type)=room',COUNT(*) FROM aoo_outbox WHERE LOWER(aggregate_type)='room'
 UNION ALL SELECT 'aoo_consumed_event','event belongs to room outbox',COUNT(*) FROM aoo_consumed_event WHERE event_id COLLATE utf8mb4_unicode_ci IN (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room')
 UNION ALL SELECT 'aoo_dead_letter_replay_audit','event belongs to room outbox',COUNT(*) FROM aoo_dead_letter_replay_audit WHERE event_id COLLATE utf8mb4_unicode_ci IN (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room')
 UNION ALL SELECT 'aoo_appeal','detach room_id only',COUNT(*) FROM aoo_appeal WHERE room_id IS NOT NULL
) counts;

-- Delete children and derived records first. No ledger row is changed.
DELETE FROM aoo_dead_letter_replay_audit WHERE event_id COLLATE utf8mb4_unicode_ci IN
 (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room');
DELETE FROM aoo_consumed_event WHERE event_id COLLATE utf8mb4_unicode_ci IN
 (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room');
DELETE FROM aoo_outbox WHERE LOWER(aggregate_type)='room';

DELETE d FROM risk_review_directive d JOIN risk_event e ON e.risk_event_id=d.risk_event_id
 WHERE e.room_id IS NOT NULL OR e.source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL);
DELETE FROM risk_event WHERE room_id IS NOT NULL
 OR source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL);
DELETE FROM telemetry_signal WHERE room_id IS NOT NULL;
DELETE FROM risk_admission_decision WHERE room_id IS NOT NULL;
DELETE FROM aoo_player_risk_observation WHERE room_id IS NOT NULL;
DELETE FROM room_safety_location;
DELETE FROM room_safety_mute;
DELETE FROM room_safety_report WHERE room_id IS NOT NULL;
UPDATE social_presence SET room_id=NULL WHERE room_id IS NOT NULL;
UPDATE aoo_appeal SET room_id=NULL WHERE room_id IS NOT NULL;

DELETE mm FROM aoo_match_member mm JOIN aoo_match m ON m.match_id=mm.match_id;
DELETE FROM aoo_match;
DELETE FROM replay_short_code_access;
DELETE FROM replay_short_code;
DELETE FROM perspective_replay_event;
DELETE FROM perspective_replay_event_archive;
DELETE FROM replay_participant;
DELETE FROM replay_participant_archive;
DELETE FROM replay_set_manifest;
DELETE FROM aoo_settlement_score;
DELETE FROM aoo_settlement_outbox;
DELETE FROM aoo_settlement;
DELETE FROM aoo_hall_game_ticket;
DELETE FROM aoo_spectator_admission;
DELETE FROM aoo_game_share;
DELETE FROM aoo_room_rule_lock;
DELETE FROM aoo_room_authority_route;
DELETE FROM aoo_room_billing_reservation;
DELETE FROM aoo_room_create_saga;
DELETE FROM aoo_room_snapshot;
DELETE FROM aoo_room_lease;
DELETE FROM aoo_room_event;
DELETE FROM aoo_room_idempotent_allocation;
DELETE FROM aoo_connection_generation;
DELETE FROM aoo_club_room_projection;
DELETE FROM aoo_hall_idempotency;
DELETE FROM aoo_business_idempotency WHERE room_id>0;
DELETE FROM aoo_hall_room_member;
DELETE FROM aoo_hall_room;

-- Canonical catalog identities. Internal numeric ids remain unchanged.
UPDATE aoo_game_catalog SET game_code='CD201',display_name='成都跑得快',
 provider_key='com.aoo.bcg.poker.PdkGameProvider:CD201',family_code='poker:pao-de-kuai',status='ACTIVE'
 WHERE game_id=8;
UPDATE aoo_game_catalog SET game_code='NJ201',display_name='内江跑得快',
 provider_key='com.aoo.bcg.poker.PdkGameProvider:NJ201',family_code='poker:pao-de-kuai',status='ACTIVE'
 WHERE game_id=629;
UPDATE aoo_game_catalog SET game_code='LS201',display_name='凉山跑得快',
 provider_key='com.aoo.bcg.poker.PdkGameProvider:LS201',family_code='poker:pao-de-kuai',status='ACTIVE'
 WHERE game_id=90005;

-- Publish a new immutable release/index generation for each code. Historical
-- releases remain immutable but are no longer current and no room references them.
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,
 catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,
 ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT CASE a.game_id WHEN 8 THEN 82026090703 WHEN 629 THEN 6292026090703 ELSE 900052026090703 END,
 a.game_id,a.play_version,2026090703,r.release_scope,
 JSON_SET(r.catalog_snapshot,'$.gameCode',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END,
  '$.provider',CONCAT('com.aoo.bcg.poker.PdkGameProvider:',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END)),
 r.rule_snapshot,
 JSON_SET(r.ui_snapshot,'$.regionalProfile',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END),
 JSON_SET(r.component_snapshot,'$.regionalProvider',CONCAT('native-pdk-',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END)),
 SHA2(CONCAT(a.game_id,'|20260907|catalog'),256),r.rule_hash,
 SHA2(CONCAT(a.game_id,'|20260907|ui'),256),SHA2(CONCAT(a.game_id,'|20260907|component'),256),
 SHA2(CONCAT(a.game_id,'|20260907|pdk-common-room'),256),'ACTIVE',100,r.created_by,
 'Activate stable PDK business code after authorized room-history reset',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_game_release r ON r.release_id=(
 SELECT r2.release_id FROM aoo_game_release r2
 WHERE r2.game_id=a.game_id AND r2.play_version=a.play_version AND r2.status='ACTIVE'
 ORDER BY r2.release_version DESC,r2.release_id DESC LIMIT 1
)
WHERE a.game_id IN(8,629,90005);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT CASE a.game_id WHEN 8 THEN 82026090703 WHEN 629 THEN 6292026090703 ELSE 900052026090703 END,
 a.region_code,100,'ACTIVE' FROM aoo_compiled_index_active a WHERE a.game_id IN(8,629,90005);

INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,
 release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
 validated_at,activated_at)
SELECT a.game_id,a.region_code,a.play_version,2026090703,
 CASE a.game_id WHEN 8 THEN 82026090703 WHEN 629 THEN 6292026090703 ELSE 900052026090703 END,
 JSON_ARRAY(CONCAT('com.aoo.bcg.poker.PdkGameProvider:',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END,'@',a.play_version)),
 i.rule_validator,JSON_SET(i.ui_schema,'$.regionalProfile',CASE a.game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END),
 SHA2(CONCAT(a.game_id,'|20260907|index'),256),SHA2(CONCAT(a.game_id,'|20260907|pdk-common-room'),256),
 'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
 ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
 AND i.index_generation=a.index_generation WHERE a.game_id IN(8,629,90005);

UPDATE aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a
 ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version
 SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
 WHERE i.game_id IN(8,629,90005) AND i.index_generation<>2026090703 AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release SET status='RETIRED',retired_at=COALESCE(retired_at,CURRENT_TIMESTAMP(3))
 WHERE game_id IN(8,629,90005)
   AND release_id NOT IN(82026090703,6292026090703,900052026090703) AND status='ACTIVE';

UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id
 SET rr.status='RETIRED' WHERE r.game_id IN(8,629,90005)
 AND r.release_id NOT IN(82026090703,6292026090703,900052026090703) AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active SET index_generation=2026090703,
 release_id=CASE game_id WHEN 8 THEN 82026090703 WHEN 629 THEN 6292026090703 ELSE 900052026090703 END,
 cache_epoch=cache_epoch+1,activated_by=1,activation_reason='Stable PDK business-code activation',
 activated_at=CURRENT_TIMESTAMP(3) WHERE game_id IN(8,629,90005);

UPDATE aoo_published_game_configuration SET
 release_id=CASE game_id WHEN 8 THEN 82026090703 WHEN 629 THEN 6292026090703 ELSE 900052026090703 END,
 configuration_payload=JSON_SET(configuration_payload,'$.gameCode',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END,
 '$.regionalProvider',CONCAT('native-pdk-',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END)),
 reason='Stable PDK business-code activation',created_at=CURRENT_TIMESTAMP(3)
 WHERE game_id IN(8,629,90005);

UPDATE aoo_game_profile_version SET
 profile_payload=JSON_SET(profile_payload,'$.gameCode',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END,
 '$.regionalProfile',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END),
 content_hash=SHA2(CAST(JSON_SET(profile_payload,'$.gameCode',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END,
 '$.regionalProfile',CASE game_id WHEN 8 THEN 'CD201' WHEN 629 THEN 'NJ201' ELSE 'LS201' END) AS CHAR),256)
 WHERE game_id IN(8,629,90005);

INSERT INTO aoo_room_history_cleanup_count(run_id,phase,table_name,row_filter,row_count,recorded_at)
SELECT @aoo_room_cleanup_run,'AFTER',table_name,row_filter,row_count,CURRENT_TIMESTAMP(3)
FROM (
 SELECT 'aoo_hall_room' table_name,'all rows' row_filter,COUNT(*) row_count FROM aoo_hall_room
 UNION ALL SELECT 'aoo_hall_room_member','all rows',COUNT(*) FROM aoo_hall_room_member
 UNION ALL SELECT 'aoo_hall_game_ticket','all rows',COUNT(*) FROM aoo_hall_game_ticket
 UNION ALL SELECT 'aoo_spectator_admission','all rows',COUNT(*) FROM aoo_spectator_admission
 UNION ALL SELECT 'aoo_game_share','all rows',COUNT(*) FROM aoo_game_share
 UNION ALL SELECT 'aoo_room_rule_lock','all rows',COUNT(*) FROM aoo_room_rule_lock
 UNION ALL SELECT 'aoo_room_authority_route','all rows',COUNT(*) FROM aoo_room_authority_route
 UNION ALL SELECT 'aoo_room_billing_reservation','all rows; billing ledger preserved',COUNT(*) FROM aoo_room_billing_reservation
 UNION ALL SELECT 'aoo_room_create_saga','all rows',COUNT(*) FROM aoo_room_create_saga
 UNION ALL SELECT 'aoo_room_snapshot','all rows',COUNT(*) FROM aoo_room_snapshot
 UNION ALL SELECT 'aoo_room_lease','all rows',COUNT(*) FROM aoo_room_lease
 UNION ALL SELECT 'aoo_room_event','all rows',COUNT(*) FROM aoo_room_event
 UNION ALL SELECT 'aoo_room_idempotent_allocation','all rows',COUNT(*) FROM aoo_room_idempotent_allocation
 UNION ALL SELECT 'aoo_connection_generation','all rows',COUNT(*) FROM aoo_connection_generation
 UNION ALL SELECT 'aoo_club_room_projection','all rows; club body/member preserved',COUNT(*) FROM aoo_club_room_projection
 UNION ALL SELECT 'aoo_settlement_score','all rows',COUNT(*) FROM aoo_settlement_score
 UNION ALL SELECT 'aoo_settlement_outbox','all rows',COUNT(*) FROM aoo_settlement_outbox
 UNION ALL SELECT 'aoo_settlement','all rows',COUNT(*) FROM aoo_settlement
 UNION ALL SELECT 'replay_short_code_access','all rows',COUNT(*) FROM replay_short_code_access
 UNION ALL SELECT 'replay_short_code','all rows',COUNT(*) FROM replay_short_code
 UNION ALL SELECT 'perspective_replay_event','all rows',COUNT(*) FROM perspective_replay_event
 UNION ALL SELECT 'perspective_replay_event_archive','all rows',COUNT(*) FROM perspective_replay_event_archive
 UNION ALL SELECT 'replay_participant','all rows',COUNT(*) FROM replay_participant
 UNION ALL SELECT 'replay_participant_archive','all rows',COUNT(*) FROM replay_participant_archive
 UNION ALL SELECT 'replay_set_manifest','all rows',COUNT(*) FROM replay_set_manifest
 UNION ALL SELECT 'aoo_hall_idempotency','all room operations',COUNT(*) FROM aoo_hall_idempotency
 UNION ALL SELECT 'aoo_business_idempotency','room_id>0',COUNT(*) FROM aoo_business_idempotency WHERE room_id>0
 UNION ALL SELECT 'aoo_match_member','members of all room matches',COUNT(*) FROM aoo_match_member
 UNION ALL SELECT 'aoo_match','all rows',COUNT(*) FROM aoo_match
 UNION ALL SELECT 'room_safety_location','all rows',COUNT(*) FROM room_safety_location
 UNION ALL SELECT 'room_safety_mute','all rows',COUNT(*) FROM room_safety_mute
 UNION ALL SELECT 'room_safety_report','room_id IS NOT NULL',COUNT(*) FROM room_safety_report WHERE room_id IS NOT NULL
 UNION ALL SELECT 'aoo_player_risk_observation','room_id IS NOT NULL',COUNT(*) FROM aoo_player_risk_observation WHERE room_id IS NOT NULL
 UNION ALL SELECT 'risk_admission_decision','room_id IS NOT NULL',COUNT(*) FROM risk_admission_decision WHERE room_id IS NOT NULL
 UNION ALL SELECT 'risk_review_directive','directives for room-derived risk events',COUNT(*) FROM risk_review_directive d JOIN risk_event e ON e.risk_event_id=d.risk_event_id WHERE e.room_id IS NOT NULL OR e.source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL)
 UNION ALL SELECT 'risk_event','room_id or room telemetry source',COUNT(*) FROM risk_event WHERE room_id IS NOT NULL OR source_signal_id IN (SELECT signal_id FROM telemetry_signal WHERE room_id IS NOT NULL)
 UNION ALL SELECT 'social_presence','detach room_id only',COUNT(*) FROM social_presence WHERE room_id IS NOT NULL
 UNION ALL SELECT 'telemetry_signal','room_id IS NOT NULL',COUNT(*) FROM telemetry_signal WHERE room_id IS NOT NULL
 UNION ALL SELECT 'aoo_outbox','LOWER(aggregate_type)=room',COUNT(*) FROM aoo_outbox WHERE LOWER(aggregate_type)='room'
 UNION ALL SELECT 'aoo_consumed_event','event belongs to room outbox',COUNT(*) FROM aoo_consumed_event WHERE event_id COLLATE utf8mb4_unicode_ci IN (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room')
 UNION ALL SELECT 'aoo_dead_letter_replay_audit','event belongs to room outbox',COUNT(*) FROM aoo_dead_letter_replay_audit WHERE event_id COLLATE utf8mb4_unicode_ci IN (SELECT event_id FROM aoo_outbox WHERE LOWER(aggregate_type)='room')
 UNION ALL SELECT 'aoo_appeal','detach room_id only',COUNT(*) FROM aoo_appeal WHERE room_id IS NOT NULL
) counts;

UPDATE aoo_room_history_cleanup_run SET status='SUCCEEDED',
 account_rows_after=(SELECT COUNT(*) FROM aoo_account),ledger_rows_after=(SELECT COUNT(*) FROM aoo_ledger),
 club_rows_after=(SELECT COUNT(*) FROM aoo_club_state),club_member_rows_after=(SELECT COUNT(*) FROM aoo_club_member),
 completed_at=CURRENT_TIMESTAMP(3) WHERE run_id=@aoo_room_cleanup_run;

CALL assert_pdk_code_room_cleanup();
SET @aoo_authorized_room_cleanup=0;
COMMIT;
DROP PROCEDURE assert_pdk_code_room_cleanup;
