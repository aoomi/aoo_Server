-- Forward-only canonical runtime projection repair.
--
-- Signed source: Server/tools/runtime-metadata-canonical-manifest.json
-- Manifest SHA-256: 654e67bb445ba0ed3b85b57cc4a1f8267b01abae178dee9c08d364ea32299ad6
-- Release edition: r9-v4
--
-- This migration deliberately does not edit or reinterpret any historical
-- migration.  Every releaseId and indexGeneration below is a signed literal.
-- No value is derived from game_id, MAX(), a database sequence, an environment
-- variable, a default, or an old snapshot.  Provider factory/implementation
-- semantics are validated separately; this migration never silently rewrites
-- provider_key.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE publish_canonical_runtime_projection_v20261011_01()
BEGIN
  DECLARE expected_identity_count INT DEFAULT 32;
  DECLARE matched_count INT DEFAULT 0;

  START TRANSACTION;

  CREATE TEMPORARY TABLE canonical_runtime_projection_v20261011_01 (
    game_id BIGINT UNSIGNED NOT NULL,
    game_code VARCHAR(64) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    snapshot_schema_version VARCHAR(64) NOT NULL,
    card_codec_version VARCHAR(64) NOT NULL,
    event_interpreter_version VARCHAR(64) NOT NULL,
    protocol_version VARCHAR(64) NOT NULL,
    source_hash CHAR(64) NOT NULL,
    PRIMARY KEY (game_id,region_code,play_version),
    UNIQUE KEY uk_projection_release (release_id,region_code),
    UNIQUE KEY uk_projection_generation (game_id,region_code,play_version,index_generation)
  ) ENGINE=MEMORY;

  INSERT INTO canonical_runtime_projection_v20261011_01(
    game_id,game_code,play_version,region_code,release_id,index_generation,
    snapshot_schema_version,card_codec_version,event_interpreter_version,
    protocol_version,source_hash
  ) VALUES
    (2001,'chengdu2001','chengdu2001-v1.0.0','CN-51-01',1107349,80417,'room-snapshot-v1','card-v3','events-v4','protocol-v2','8e61ed0e712f26536161f6c89e64b78f3f485abe07fc6efb9a24e1856cde3dd5'),
    (2002,'neijiang2002','neijiang2002-v1.0.0','CN-51-10',1124807,87263,'room-snapshot-v1','card-v3','events-v4','protocol-v2','213428f2fbbacf2ddc5eb2a4698718243c26c9ee8a9bc97d32e5fede23082562'),
    (2003,'liangshan2003','liangshan2003-v1.0.0','CN-51-34',1183921,83691,'room-snapshot-v1','card-v3','events-v4','protocol-v2','604fab465eaaf3138b786ca60cbf473006f9658bf51b080d7ff90418e992caf6'),
    (5,'quanguo2998','cn298-v1.0.0','GLOBAL',1095713,91807,'room-snapshot-v1','card-v3','events-v4','protocol-v2','8f266ab075b150ba1aee69717be177b5830c64d5a3f79aa7e7eb275384cae886'),
    (9,'quanguo2997','cn297-v1.0.0','GLOBAL',1168205,84529,'room-snapshot-v1','card-v3','events-v4','protocol-v2','4e2c555d2a50a5c065fb18344fb7290363d110841cb82c818b326d603fa853f7'),
    (630,'chengdu2999','cd299-v1.0.0','CN-51-01',1214679,90173,'room-snapshot-v1','card-v3','events-v4','protocol-v2','2da550e6fd78d5f6c3656bf62a288b294503582f4056e60d0fc835fd177c44df'),
    (2004,'mianning2004','mianning2004-v1.0.0','CN-51-34-33',1142038,88742,'room-snapshot-v1','card-v3','events-v4','protocol-v2','8215a42ea637ca7b834d217e16ecd03764026ac07991f2c4bfe5fd0d861c5606'),
    (2005,'anyue2005','anyue2005-v1.0.0','CN-51-20-21',1179054,82915,'room-snapshot-v1','card-v3','events-v4','protocol-v2','56a96c7edba9cd3228feedc7d77e1b24b802bbd58d1420b4b6b982af4995c6e8'),
    (2006,'panzhihua2006','panzhihua2006-v1.0.0','CN-51-04',1236812,96403,'room-snapshot-v1','card-v3','events-v4','protocol-v2','321291c459cfd941f6e36f4c54e8615f42dbf3385069faf9534a9fba1a04aff0'),
    (2007,'wannian2007','wannian2007-v1.0.0','CN-36-11-29',1087465,81368,'room-snapshot-v1','card-v3','events-v4','protocol-v2','db620d911ee4fda3fc6f44b3c1c95a3453ad8e95d4ccea506c2709d945a77259'),
    (2008,'leping2008','leping2008-v1.0.0','CN-36-02-81',1203591,92517,'room-snapshot-v1','card-v3','events-v4','protocol-v2','a82c87a2c3099dac98ad7d61758cbc351b761d6075eac3595ef17a3efd52055d'),
    (2009,'bozhou2009','bozhou2009-v1.0.0','CN-34-16',1116284,85804,'room-snapshot-v1','card-v3','events-v4','protocol-v2','40f302309fa5483f3be71983f45c3d4fcbe214b902a06f49ffc9fa37b4cc4fd2'),
    (2010,'yugan2010','yugan2010-v1.0.0','CN-36-11-27',1194726,94629,'room-snapshot-v1','card-v3','events-v4','protocol-v2','b21eb52c068d558f63ec75cdc4b55e44d03461876eb483e65d29917d9286fc07'),
    (2011,'jian2011','jian2011-v1.0.0','CN-36-08',1138057,83276,'room-snapshot-v1','card-v3','events-v4','protocol-v2','e4ccc503a641913c3a9819ab7dc6ac2e98b0634b8f83398d6625af5b28363f43'),
    (2012,'gushi2012','gushi2012-v1.0.0','CN-41-15-25',1221643,90741,'room-snapshot-v1','card-v3','events-v4','protocol-v2','b8fa18577891123b5975e1152d27b1cda1e9525f67e66f1798af36bde1af116f'),
    (2013,'xiaogan2013','xiaogan2013-v1.0.0','CN-42-09',1159370,89153,'room-snapshot-v1','card-v3','events-v4','protocol-v2','4a63a0dd407a374162e73cab1e75ae9e45bf5a8dba24a95dbd181ec57325b4da'),
    (2014,'ningdu2014','ningdu2014-v1.0.0','CN-36-07-30',1102865,82408,'room-snapshot-v1','card-v3','events-v4','protocol-v2','643ee14a92b21bcb38e031c23266eef638f6df1e94a8eaa87fa133666b252d7b'),
    (2015,'huaihua2015','huaihua2015-v1.0.0','CN-43-12',1187409,93867,'room-snapshot-v1','card-v3','events-v4','protocol-v2','09ed8f317173ac105cd496745476de156066618d8d0cedb37e4ebb61789803d4'),
    (2016,'xinxian2016','xinxian2016-v1.0.0','CN-41-15-23',1243058,81624,'room-snapshot-v1','card-v3','events-v4','protocol-v2','333472d33ae8394f983ca039961cbecdc6f615c9ffcb12b4d5f438c53be85827'),
    (2017,'jingdezhen2017','jingdezhen2017-v1.0.0','CN-36-02',1129674,95302,'room-snapshot-v1','card-v3','events-v4','protocol-v2','a4d86cf7861b56e7a6c155d663824867ed4a88b0d7040070d329d594e3513d64'),
    (2018,'jiangsu2018','jiangsu2018-v1.0.0','CN-32',1210836,87645,'room-snapshot-v1','card-v3','events-v4','protocol-v2','e370c71bbbaf12d302ac460f92576801a1decd86f9446a9b5ea88e12ac342393'),
    (2019,'henan2019','henan2019-v1.0.0','CN-41',1098347,84219,'room-snapshot-v1','card-v3','events-v4','protocol-v2','1d63162d81894e2fd33dd4da9b3a8f02eeb62bf470f541388c5805468287ba27'),
    (2020,'taizhou2020','taizhou2020-v1.0.0','CN-32-12',1162579,91476,'room-snapshot-v1','card-v3','events-v4','protocol-v2','833c362250f8c555e454b20d958deb75adfbcfdfd5026b8e06fd6455db53cd2a'),
    (2021,'huaibin2021','huaibin2021-v1.0.0','CN-41-15-27',1230942,80538,'room-snapshot-v1','card-v3','events-v4','protocol-v2','de06df5250c234e0c0fd1f5416571c445a22f449a1bafe7236697c474368562a'),
    (2022,'huangchuan2022','huangchuan2022-v1.0.0','CN-41-15-26',1147681,96721,'room-snapshot-v1','card-v3','events-v4','protocol-v2','de9987783bdda700fc44e985d4c1f3f3fe8c3b80ccd06af1c88be7fbfd6e4269'),
    (2023,'poyang2023','poyang2023-v1.0.0','CN-36-11-28',1076295,88304,'room-snapshot-v1','card-v3','events-v4','protocol-v2','f253d855b61db16a2974b7914cc8885883864703ee2e388264897da5359ec9cf'),
    (2024,'jintan2024','jintan2024-v1.0.0','CN-32-04-13',1208147,92768,'room-snapshot-v1','card-v3','events-v4','protocol-v2','77ff234e316520b1c57b3e72a1cb63120b177b63c03bbb65dac815f33eb5439a'),
    (2025,'gaoan2025','gaoan2025-v1.0.0','CN-36-09-83',1110953,81957,'room-snapshot-v1','card-v3','events-v4','protocol-v2','0a2432c8cc9d8e68bfa52835aeb716fecb76a56c47e355abe9d48cebd9070213'),
    (2026,'yixing2026','yixing2026-v1.0.0','CN-32-02-82',1173486,94912,'room-snapshot-v1','card-v3','events-v4','protocol-v2','61b273f219ad311659004b35de471137fc9b74fa7c6b502c1accabf58f7e97f5'),
    (2027,'xuancheng2027','xuancheng2027-v1.0.0','CN-34-18',1227354,86143,'room-snapshot-v1','card-v3','events-v4','protocol-v2','9a6c1c3868a433fb1273f4c303528d5d8e0248b1252dd7381681397a8c995f0f'),
    (2028,'fuzhou2028','fuzhou2028-v1.0.0','CN-36-10',1084139,93506,'room-snapshot-v1','card-v3','events-v4','protocol-v2','8ca3169a23fa125cb18e2d2494c38b1a4977d77ca4800c214b52f80d84a6ad6a'),
    (1001,'chengdu1001','chengdu1001-v1.0.0','CN-51-01',1256724,97318,'room-snapshot-v1','card-v3','events-v4','protocol-v2','eb0a2f95ca1604203acf3f445a9a776b09a3087ab6020afaa0663eead886dcb7');

  SELECT COUNT(*) INTO matched_count
    FROM canonical_runtime_projection_v20261011_01;
  IF matched_count <> expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_PROJECTION_ROW_COUNT_MISMATCH';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN aoo_game_release r
        ON r.release_id=c.release_id
       AND r.game_id=c.game_id
       AND r.play_version=c.play_version
     WHERE r.release_id IS NULL
        OR r.status <> 'ACTIVE'
        OR r.snapshot_schema_version <> c.snapshot_schema_version
        OR r.card_codec_version <> c.card_codec_version
        OR r.event_interpreter_version <> c.event_interpreter_version
        OR r.protocol_version <> c.protocol_version
        OR r.source_hash <> c.source_hash
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_RELEASE_METADATA_MISSING_OR_MISMATCHED';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN aoo_game_release_region rr
        ON rr.release_id=c.release_id
       AND rr.region_code=c.region_code
     WHERE rr.release_id IS NULL OR rr.status <> 'ACTIVE'
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_RELEASE_REGION_MISSING_OR_INACTIVE';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN aoo_compiled_room_create_index i
        ON i.game_id=c.game_id
       AND i.region_code=c.region_code
       AND i.play_version=c.play_version
       AND i.index_generation=c.index_generation
       AND i.release_id=c.release_id
     WHERE i.game_id IS NULL OR i.lifecycle_state <> 'ACTIVE'
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_COMPILED_INDEX_MISSING_OR_INACTIVE';
  END IF;

  -- Retire every non-canonical active row before publishing the pointer.  The
  -- release/index history remains queryable and recoverable; only its active
  -- lifecycle state changes.
  UPDATE aoo_game_release r
  JOIN canonical_runtime_projection_v20261011_01 c
    ON c.game_id=r.game_id AND c.play_version=r.play_version
   SET r.status='RETIRED',
       r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
   WHERE r.status='ACTIVE' AND r.release_id<>c.release_id;

  UPDATE aoo_game_release_region rr
  JOIN aoo_game_release r ON r.release_id=rr.release_id
  JOIN canonical_runtime_projection_v20261011_01 c
    ON c.game_id=r.game_id AND c.play_version=r.play_version
   SET rr.status='RETIRED'
   WHERE rr.status='ACTIVE' AND rr.release_id<>c.release_id;

  UPDATE aoo_compiled_room_create_index i
  JOIN canonical_runtime_projection_v20261011_01 c
    ON c.game_id=i.game_id
   AND c.region_code=i.region_code
   AND c.play_version=i.play_version
   SET i.lifecycle_state='RETIRED',
       i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
   WHERE i.lifecycle_state='ACTIVE' AND i.release_id<>c.release_id;

  UPDATE aoo_compiled_index_active a
  JOIN canonical_runtime_projection_v20261011_01 c
    ON c.game_id=a.game_id
   AND c.region_code=a.region_code
   AND c.play_version=a.play_version
   SET a.index_generation=c.index_generation,
       a.release_id=c.release_id,
       a.cache_epoch=a.cache_epoch+1,
       a.activated_by=1,
       a.activation_reason='Publish signed canonical runtime projection v20261011_01',
       a.activated_at=CURRENT_TIMESTAMP(3);

  UPDATE aoo_published_game_configuration p
  JOIN canonical_runtime_projection_v20261011_01 c
    ON c.game_id=p.game_id AND c.play_version=p.play_version
   SET p.release_id=c.release_id,
       p.reason='Publish signed canonical runtime projection v20261011_01';

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN aoo_compiled_index_active a
        ON a.game_id=c.game_id
       AND a.region_code=c.region_code
       AND a.play_version=c.play_version
       AND a.release_id=c.release_id
       AND a.index_generation=c.index_generation
     WHERE a.game_id IS NULL
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_ACTIVE_POINTER_MISMATCH';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN (
        SELECT r.game_id,r.play_version,COUNT(*) active_count
          FROM aoo_game_release r
         WHERE r.status='ACTIVE'
         GROUP BY r.game_id,r.play_version
      ) active_release
        ON active_release.game_id=c.game_id
       AND active_release.play_version=c.play_version
     WHERE active_release.active_count <> 1
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_ACTIVE_RELEASE_NOT_UNIQUE';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261011_01 c
      LEFT JOIN (
        SELECT i.game_id,i.region_code,i.play_version,COUNT(*) active_count
          FROM aoo_compiled_room_create_index i
         WHERE i.lifecycle_state='ACTIVE'
         GROUP BY i.game_id,i.region_code,i.play_version
      ) active_index
        ON active_index.game_id=c.game_id
       AND active_index.region_code=c.region_code
       AND active_index.play_version=c.play_version
     WHERE active_index.active_count <> 1
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='CANONICAL_ACTIVE_COMPILED_INDEX_NOT_UNIQUE';
  END IF;

  COMMIT;
END$$

CALL publish_canonical_runtime_projection_v20261011_01()$$
DROP PROCEDURE publish_canonical_runtime_projection_v20261011_01$$

DELIMITER ;
