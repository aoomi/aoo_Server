-- Local-development publication for Chengdu Mahjong 1001, ROUND_COUNT=8.
-- Forward-only and idempotent: legacy zero-cost rows are retained unchanged.
-- Do not run against production without the isolated-development gate.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE PROCEDURE publish_chengdu1001_round8_room_card_v20261018_01()
BEGIN
-- Required isolated-development session marker. The runner must set this
-- explicitly; absence or any other value fails before BEGIN/COMMIT or writes.
IF COALESCE(@aoo_dev_publication_gate,'') <> 'AOO_LOCAL_DEV_20261018' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CHENGDU1001_ROUND8_LOCAL_DEV_GATE_REQUIRED';
END IF;
START TRANSACTION;

SET @publication_id = 2026101801;
SET @policy_version = 2026101801;
SET @pointer_name = 'roomcard';
SET @game_id = 1001;
SET @game_code = 'chengdu1001';
SET @play_version = 'chengdu1001-v1.0.0';
SET @region_code = 'CN-51-01';
SET @source_hash = '9ca9df7a061bebadbe31ccd2acfc169bf968076c6dc459baafe7cf262dc0c87c';
SET @release_id = 100120261005;
SET @index_generation = 2026100605;
SET @refund_ref = 'XQP_R5';

-- Release/index drift guard: fail closed before any write.
SET @binding_matches = (
  SELECT COUNT(*)
    FROM aoo_compiled_index_active a
    JOIN aoo_compiled_room_create_index i
      ON i.game_id=a.game_id AND i.region_code=a.region_code
     AND i.play_version=a.play_version AND i.index_generation=a.index_generation
     AND i.release_id=a.release_id AND i.lifecycle_state='ACTIVE'
    JOIN aoo_game_release r
      ON r.release_id=a.release_id AND r.game_id=a.game_id
     AND r.play_version=a.play_version AND r.status='ACTIVE'
   WHERE a.game_id=@game_id AND a.region_code=@region_code
     AND a.play_version=@play_version AND a.release_id=@release_id
     AND a.index_generation=@index_generation AND r.source_hash=@source_hash);
IF @binding_matches <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CHENGDU1001_ROUND8_RELEASE_BINDING_DRIFT';
END IF;

SET @hash2 = SHA2(CONCAT(@game_id,'|',@play_version,'|',@region_code,'|ROUND_COUNT|8|2|OWNER|ROOM_CARD|3|FIXED|HALL|',@refund_ref),256);
SET @hash3 = SHA2(CONCAT(@game_id,'|',@play_version,'|',@region_code,'|ROUND_COUNT|8|3|OWNER|ROOM_CARD|4|FIXED|HALL|',@refund_ref),256);
SET @hash4 = SHA2(CONCAT(@game_id,'|',@play_version,'|',@region_code,'|ROUND_COUNT|8|4|OWNER|ROOM_CARD|5|FIXED|HALL|',@refund_ref),256);
SET @publication_hash = SHA2(CONCAT(@hash2,'|',@hash3,'|',@hash4,'|',@release_id,'|',@index_generation,'|',@source_hash),256);

-- Idempotency/drift guards for the publication identity.
SET @pub_conflicts = (SELECT COUNT(*) FROM aoo_room_cost_publication
 WHERE publication_id=@publication_id AND content_hash<>@publication_hash);
IF @pub_conflicts <> 0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CHENGDU1001_ROUND8_PUBLICATION_ID_CONFLICT';
END IF;
INSERT INTO aoo_room_cost_publication(publication_id,content_hash,environment,validated_at)
VALUES(@publication_id,@publication_hash,'TEST_ONLY',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE publication_id=VALUES(publication_id);

INSERT INTO aoo_room_cost_policy(
 game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,
 payer_mode,currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version,
 content_hash,status,publication_id,billing_kind,billing_context,tier_minor,
 confirmation_status,free_policy_ref,refund_policy_ref)
VALUES
 (@game_id,@play_version,@region_code,'ROUND_COUNT',8,8,2,'OWNER','ROOM_CARD',3,NULL,NULL,@policy_version,@hash2,'ACTIVE',@publication_id,'FIXED','HALL',0,'CONFIRMED',NULL,@refund_ref),
 (@game_id,@play_version,@region_code,'ROUND_COUNT',8,8,3,'OWNER','ROOM_CARD',4,NULL,NULL,@policy_version,@hash3,'ACTIVE',@publication_id,'FIXED','HALL',0,'CONFIRMED',NULL,@refund_ref),
 (@game_id,@play_version,@region_code,'ROUND_COUNT',8,8,4,'OWNER','ROOM_CARD',5,NULL,NULL,@policy_version,@hash4,'ACTIVE',@publication_id,'FIXED','HALL',0,'CONFIRMED',NULL,@refund_ref)
ON DUPLICATE KEY UPDATE
 content_hash=VALUES(content_hash),status=VALUES(status),policy_version=VALUES(policy_version),
 confirmation_status=VALUES(confirmation_status),free_policy_ref=VALUES(free_policy_ref),
 refund_policy_ref=VALUES(refund_policy_ref);

INSERT INTO aoo_room_cost_policy_provenance(
 publication_id,policy_key,configuration_version,workbook_manifest_hash,authority_hash,
 source_class,release_id,index_generation,source_hash,free_policy_ref,free_valid_from,
 free_valid_until,always_free)
VALUES
 (@publication_id,@hash2,'chengdu1001-round8-2026101801','a34ecb9dcb36053098c1e8e4bfe4ba6c2ac19da369cb36ce335b29ccdc2c2103',SHA2('canonical-chengdu1001-owner-fixed-hall-room-card',256),'CURRENT_PUBLISHED',@release_id,@index_generation,@source_hash,NULL,NULL,NULL,FALSE),
 (@publication_id,@hash3,'chengdu1001-round8-2026101801','a34ecb9dcb36053098c1e8e4bfe4ba6c2ac19da369cb36ce335b29ccdc2c2103',SHA2('canonical-chengdu1001-owner-fixed-hall-room-card',256),'CURRENT_PUBLISHED',@release_id,@index_generation,@source_hash,NULL,NULL,NULL,FALSE),
 (@publication_id,@hash4,'chengdu1001-round8-2026101801','a34ecb9dcb36053098c1e8e4bfe4ba6c2ac19da369cb36ce335b29ccdc2c2103',SHA2('canonical-chengdu1001-owner-fixed-hall-room-card',256),'CURRENT_PUBLISHED',@release_id,@index_generation,@source_hash,NULL,NULL,NULL,FALSE)
ON DUPLICATE KEY UPDATE policy_key=VALUES(policy_key);

SET @next_pointer_version = 1;
SELECT version+1 INTO @next_pointer_version
  FROM aoo_room_cost_active
 WHERE pointer_name=@pointer_name
 FOR UPDATE;
INSERT INTO aoo_room_cost_active(pointer_name,publication_id,version,activated_at)
VALUES(@pointer_name,@publication_id,@next_pointer_version,CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE publication_id=VALUES(publication_id),version=VALUES(version),activated_at=VALUES(activated_at);

-- Postconditions: exactly the three new rows, no legacy rewrite, and one pointer.
IF (SELECT COUNT(*) FROM aoo_room_cost_policy WHERE publication_id=@publication_id AND game_id=@game_id AND play_version=@play_version AND region_code=@region_code AND round_count=8 AND player_count IN (2,3,4) AND payer_mode='OWNER' AND currency_code='ROOM_CARD' AND billing_kind='FIXED' AND billing_context='HALL' AND confirmation_status='CONFIRMED' AND free_policy_ref IS NULL AND refund_policy_ref=@refund_ref AND status='ACTIVE') <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CHENGDU1001_ROUND8_POLICY_POSTCHECK_FAILED';
END IF;
IF (SELECT publication_id FROM aoo_room_cost_active WHERE pointer_name=@pointer_name) <> @publication_id THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CHENGDU1001_ROUND8_POINTER_POSTCHECK_FAILED';
END IF;
COMMIT;
END$$
CALL publish_chengdu1001_round8_room_card_v20261018_01()$$
DROP PROCEDURE publish_chengdu1001_round8_room_card_v20261018_01$$
DELIMITER ;
