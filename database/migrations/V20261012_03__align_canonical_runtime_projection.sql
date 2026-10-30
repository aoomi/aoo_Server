-- Forward-only alignment of all current runtime projections with the signed
-- r50 canonical manifest.  This migration does not rewrite historical rows;
-- it publishes a new immutable release/index for each current identity and
-- retires the previous active projection.  All tuple values below are signed
-- manifest literals; no gameId arithmetic, MAX(), defaults, or old snapshots
-- are used to create identity metadata.
--
-- Manifest: Server/release-input/runtime-metadata-canonical-manifest-
-- current-20261012.json

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DELIMITER $$

CREATE PROCEDURE align_canonical_runtime_projection_v20261012_03()
BEGIN
  DECLARE expected_identity_count INT DEFAULT 32;
  DECLARE matched_identity_count INT DEFAULT 0;

  START TRANSACTION;

  CREATE TEMPORARY TABLE canonical_runtime_projection_v20261012_03 (
    game_id BIGINT UNSIGNED NOT NULL,
    game_code VARCHAR(64) NOT NULL,
    family_code VARCHAR(64) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    source_hash CHAR(64) NOT NULL,
    release_id BIGINT UNSIGNED NOT NULL,
    index_generation BIGINT UNSIGNED NOT NULL,
    contract_version VARCHAR(64) NOT NULL,
    snapshot_schema_version VARCHAR(64) NOT NULL,
    card_codec_version VARCHAR(64) NOT NULL,
    event_interpreter_version VARCHAR(64) NOT NULL,
    protocol_version VARCHAR(64) NOT NULL,
    component_content_hash CHAR(64) NOT NULL,
    rule_content_hash CHAR(64) NOT NULL,
    PRIMARY KEY (game_id,region_code,play_version),
    UNIQUE KEY uk_projection_release (release_id,region_code),
    UNIQUE KEY uk_projection_generation (game_id,region_code,play_version,index_generation)
  ) ENGINE=MEMORY;

  INSERT INTO canonical_runtime_projection_v20261012_03(
      game_id,game_code,family_code,play_version,region_code,source_hash,
      release_id,index_generation,contract_version,snapshot_schema_version,
      card_codec_version,event_interpreter_version,protocol_version,
      component_content_hash,rule_content_hash)
  VALUES
    (2001,'chengdu2001','poker:pao-de-kuai','chengdu2001-v1.0.0','CN-51-01','ca8ffd29cc851946e304a1e942725adebfe661b40323b3dc3614d9707078ac6b',1318087,1006137,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','b5dfd06a2c85d23827f2b27ecd013aa7713fd423c6d24c01bd5794ed68e7525b','d86c3973ba3595efe37c08ffaff21c806ca91e75803edc9773ae378506fde996'),
    (2002,'neijiang2002','poker:pao-de-kuai','neijiang2002-v1.0.0','CN-51-10','e874258955e09787e77801518830d85bf25527ec38ffff2fa117e83c518e5cad',1318093,1006143,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','c69b0625d2fcc285e23814c273b89daf1afba50f16c042e6fc1e15e52e892168','2499b3dca1410c6e4c841c046119e20c96bc7245f8add9fc02c1c1d4aa8cba5b'),
    (2003,'liangshan2003','poker:pao-de-kuai','liangshan2003-v1.0.0','CN-51-34','c5fe7d0e7078992537ba68c5ae1d96614d4ae0e32822c6dd7a1e0b4e862250c3',1318101,1006151,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','0da08967683ee45752c069308ed7f12f608e7b2fb61dd3e1c425b8cf0b3b3e64','2b66b69fe32e3a9a5e8027c3c756ab2f77d4bfdc7b9799b69c50fb73770ce159'),
    (5,'quanguo2998','poker:betting','cn298-v1.0.0','GLOBAL','1a4a372aba8f49eb8d7ea224e19680b5532e1d7288d2cfcf091bbf556b63ad52',1318057,1006107,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','7cd0d1ed23ef7d33274e0f97c7e54622db2b67954d8c27e00081d7292563f141','4cc7cc5ae78a52b54c464083c8eec0d31d0f66a71b7275ce88fda28652d3878e'),
    (9,'quanguo2997','poker:compare-hand','cn297-v1.0.0','GLOBAL','1d7b6785e3c4905cbf482312c54b4fc192726b9274e2dc575cdcf72619d8a546',1318063,1006113,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','e009598ee5adb25eb0a6c6bc1d9a5abf7913a5df80b54892250efe11472a34a2','70a3438c29ec6a667b9b8e6fa6abd41ab4197a19e0b4745dbb498d5f863977a6'),
    (2999,'chengdu2999','poker:cd299','cd299-v1.0.0','CN-51-01','f608c559c969209a1bbdf985fe54b7807bd0022c4c4a711a9a75a94aee6dbab5',2999001,3999001,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','eb63818b2060a975bb8bf5408c90efbaa2099b1ce1b1c3e8033f84f91efd874d','8aa129dcd826a9d49b0a3872731c2452d8532d45cc1d714d43320885b3024e1c'),
    (2004,'mianning2004','poker:pao-de-kuai','mianning2004-v1.0.0','CN-51-34-33','713a219390223ec6926f2c1825a95b28c95a43ef37f86b78a5923d236ef5549a',1318109,1006159,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','62eef1d67112921c29db2177da1d1bfafdf91394e388112b3cceee7ee0d66cc1','9ff749259943a882d96690d3c6560104ce901a2fcf0304b72820a46a434541a6'),
    (2005,'anyue2005','poker:pao-de-kuai','anyue2005-v1.0.0','CN-51-20-21','dabb398a4ebb6dd2700fc7d639ccc7ad589abc1142cd9f00fce2884c5932ce0a',1318117,1006167,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbf62e464276069f0931df75893c6cd1044d810d2117928d80693da915542b2b','041aabc23045ca5da27827bdc0b275d205016853361e25334e7676f1c47dce63'),
    (2006,'panzhihua2006','poker:pao-de-kuai','panzhihua2006-v1.0.0','CN-51-04','2863f927671d0c2bc8dfed680a3388fc68e0f10b2b6ea296a44f6e5e89567fa5',1318123,1006173,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','8760b9f1fd91fa0e056d3b6dd3ad31f3c2e19fbac52c3c8261267eb3d1a10762','88acd42b7d9ec311219f344035bc908696eb94ce093839c2a66582e128f7f823'),
    (2007,'wannian2007','poker:pao-de-kuai','wannian2007-v1.0.0','CN-36-11-29','b41006caa29b13d1f8a7e17966f9f599cd01a233dcbe331b6588fcfbf5579c8a',1318131,1006181,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','f05a652e6d03d02533af32b228e01ac8a02564c109648cf107f1d228c4a32a23'),
    (2008,'leping2008','poker:pao-de-kuai','leping2008-v1.0.0','CN-36-02-81','d04b51b4cded129fd40dc4ace9fc7ab2fa2d6263e755f5925663b746ac24a933',1318139,1006189,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','09b54a0b658dc48c08b90905e9f25374c31ad67f1a3dbeb9611227a23512c649'),
    (2009,'bozhou2009','poker:pao-de-kuai','bozhou2009-v1.0.0','CN-34-16','1695a3e59c6a5daf33071cf17d442f4d5f73cba2d82ac93353c9b39b09e534d3',1318147,1006197,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','3a37f01af34e6993ece021df8b3e47170d5b3259067e1f3ab4c3cde220b74478'),
    (2010,'yugan2010','poker:pao-de-kuai','yugan2010-v1.0.0','CN-36-11-27','8365bf3ebeaa545f9fee2cf4008717833dd96b967f76b4a08002da2e55527627',1318153,1006203,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','76318b47785a2eeff62e73a4ac930811192ae7b0af386cc4095643f4e9cf31f7'),
    (2011,'jian2011','poker:pao-de-kuai','jian2011-v1.0.0','CN-36-08','fb6d8852883d33fa04c9c71f9c604e4ab5fec38a293afa2b75196da10198a42c',1318161,1006211,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','06910170d35025f0fb3d00923af23f28b4e8f9ebe86f6548cec2823cfed1e0d2'),
    (2012,'gushi2012','poker:pao-de-kuai','gushi2012-v1.0.0','CN-41-15-25','17b3caa687fc6de086995f26199c96f87db2bd33336f402e17f9659b7751d180',1318169,1006219,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','1aa5e1fce7108cfe6b4cb680c4bfff06da4fa5dd3f1521d1bad64ca961cb477d'),
    (2013,'xiaogan2013','poker:pao-de-kuai','xiaogan2013-v1.0.0','CN-42-09','d89e9fc10aff8916ce29901b46a51a3d3a1fc398f3b43315e1be9a02a01aa779',1318177,1006227,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','7c819a43ba06ec615eda64646fba11715d76919887d18e10df8bd6b17f531d4e'),
    (2014,'ningdu2014','poker:pao-de-kuai','ningdu2014-v1.0.0','CN-36-07-30','65533898342b858c546668393f7fbddd4d161f561862193eb7e681927db68270',1318183,1006233,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','38c52a3403b9f381da27b1d12a38766fa8827e1d0e903de587b8b24350f2a2a6'),
    (2015,'huaihua2015','poker:pao-de-kuai','huaihua2015-v1.0.0','CN-43-12','50a363b43bb8ae70c9208726be947572b953edc1dabdb6b11f9abe793672c2f7',1318191,1006241,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','4d4303ecfc1ad8828d661c042fea75a96bd31b0907436cdbc3f3e70ba34eff8f'),
    (2016,'xinxian2016','poker:pao-de-kuai','xinxian2016-v1.0.0','CN-41-15-23','459031972aaaf8d25fb025e849712b60df6cf9361f4cf1f3a7e7c97255713b73',1318199,1006249,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','3f823beb09135a34ed5de0c3a90e38a58954ca31ece9b812f5107d5ed957c714'),
    (2017,'jingdezhen2017','poker:pao-de-kuai','jingdezhen2017-v1.0.0','CN-36-02','69283dd4d468617b61245ae2c08913c7ab289fc2339281d7b809d7c38854b7b2',1318207,1006257,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','9a02cb4d1b755f84f1d85e4eafc2b83f6b66620cc66910455231049db875dda9'),
    (2018,'jiangsu2018','poker:pao-de-kuai','jiangsu2018-v1.0.0','CN-32','f4bd0a78f9398cb120fc6705c8d91786d8efe452cb7ab8c6882b2ae3d472dcc5',1318213,1006263,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','beff7d89baa958f298b850553c2cd681750869644d3603e145fc0fa255d3eca3'),
    (2019,'henan2019','poker:pao-de-kuai','henan2019-v1.0.0','CN-41','c7259d1a9542079f5c6a3b52be2255ffd5ee769b965e490cdf8eaaf307b930df',1318221,1006271,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','a4183ad50e72c57f2735d9c8b660fb68361ef580512fb3ef6593e0a6fb993ba5'),
    (2020,'taizhou2020','poker:pao-de-kuai','taizhou2020-v1.0.0','CN-32-12','e0e8e5763652c506c8a0d18eaefe8bc58f3c6fc272c0a2ded8b1cc3626fa54e9',1318229,1006279,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','e850e7cd9851ed3c8e58579790e95c17f3b23bf4e5ade97efe5c0f8ec15430a3'),
    (2021,'huaibin2021','poker:pao-de-kuai','huaibin2021-v1.0.0','CN-41-15-27','1cdbda81f69213ae48db360070b1af4a96aa3bd857b4f7d78e9d7f41d91bba60',1318237,1006287,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','ec58e24749f61512382cb87a53bd9fcc31a2d8cebba8cdbe43bfedf3cdc0e5dc'),
    (2022,'huangchuan2022','poker:pao-de-kuai','huangchuan2022-v1.0.0','CN-41-15-26','11179dda7de218affa7b30cd4b8f0cd2270857b3220f2a2d27f2bd6230eb85c8',1318243,1006293,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','e8c693b893227f9e6fd2f0e3978aaddffb5f7c0951c47cf37080177c977925e7'),
    (2023,'poyang2023','poker:pao-de-kuai','poyang2023-v1.0.0','CN-36-11-28','2046952da61c13511b5acc164efee81b8475c5b34717fe63cd6b4862a8f673c0',1318251,1006301,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','a4222ff240d6d827efff78778f1a7699c979535fa816fd6a3cefcd5acb8da81d'),
    (2024,'jintan2024','poker:pao-de-kuai','jintan2024-v1.0.0','CN-32-04-13','caf2c0631f040188f358fc13823412671b45dee99704ecebf285e4b8ecd5895a',1318259,1006309,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','15928d6d46e500814e6af92826c3dcbdf4eaf38d661126b5b7da2a09c68e8ea0'),
    (2025,'gaoan2025','poker:pao-de-kuai','gaoan2025-v1.0.0','CN-36-09-83','abc410a038074f70381c857359c1043a6316c7e6186864321c1c74dd7032e599',1318267,1006317,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','c593ccd2f96e5c3e677b11cae27a5b69cc183fb19cd31707b5124f59ffb1cad3'),
    (2026,'yixing2026','poker:pao-de-kuai','yixing2026-v1.0.0','CN-32-02-82','49582391861a51b8fc166f1729670d2f403a8ca6925b023d71148802d08cb252',1318273,1006323,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','39418a32800a403af4a69c65035390d8cd6f9c09ace9c6f1b9c559253badc5d8'),
    (2027,'xuancheng2027','poker:pao-de-kuai','xuancheng2027-v1.0.0','CN-34-18','b42092ec31bebf63f04cd3918105c5b3db5757588c81f4612d6410689ee57a33',1318281,1006331,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','759251643d10080608c1f87b82c19771cfd6f3090b8fa0eaf99e01f6dc3e7429'),
    (2028,'fuzhou2028','poker:pao-de-kuai','fuzhou2028-v1.0.0','CN-36-10','e3c08c0afa5b3133b2833f08f390c096f0de9c4718068518b71998bf06e78aee',1318289,1006339,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','bbb5aab5e7663f754d677804d395a1df4c98653507c50ed7d80ceb1fe402cc68','4b6d4faa65bf610281229cfb537fd66551a7c4b022b75cd9b3e2ddfe7a885a3b'),
    (1001,'chengdu1001','mahjong:xue-zhan','chengdu1001-v1.0.0','CN-51-01','c2ec22c31e4e965153c942f1f2f3634ce5c216e3dd4c1b971da578d060d85665',1318079,1006129,'common-room-v1','room-snapshot-v1','card-v3','events-v4','protocol-v2','6ed639cfc253d099c868e5f581c3eb3afe37d8d55e5f8dceaaa660784b681139','d7d453b3ad4264b43dfdac93f914d5fb6c16b39c112da69ee76eb0219cbfdb42');

  SELECT COUNT(*) INTO matched_identity_count
    FROM canonical_runtime_projection_v20261012_03;
  IF matched_identity_count <> expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CANONICAL_RUNTIME_PROJECTION_ROW_COUNT_MISMATCH';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261012_03 c
      LEFT JOIN aoo_compiled_index_active a
        ON a.game_id=c.game_id AND a.region_code=c.region_code
       AND a.play_version=c.play_version
      LEFT JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code
       AND i.play_version=a.play_version AND i.index_generation=a.index_generation
       AND i.release_id=a.release_id AND i.lifecycle_state='ACTIVE'
      LEFT JOIN aoo_game_release r
        ON r.release_id=a.release_id AND r.game_id=a.game_id
       AND r.play_version=a.play_version AND r.status='ACTIVE'
      WHERE a.game_id IS NULL OR i.game_id IS NULL OR r.release_id IS NULL
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CANONICAL_RUNTIME_PROJECTION_PREIMAGE_MISSING';
  END IF;

  -- V20261012_00/V20261012_02 already publish the 2999 tuple with the
  -- canonical release id.  Release and compiled-index content is immutable
  -- after validation, so an existing canonical row is accepted only when its
  -- signed metadata and content hashes already match; it is never updated.
  IF EXISTS (
    SELECT 1
      FROM canonical_runtime_projection_v20261012_03 c
      JOIN aoo_game_release r ON r.release_id=c.release_id
     WHERE r.game_id<>c.game_id OR r.play_version<>c.play_version
        OR r.source_hash<>c.source_hash
        OR r.snapshot_schema_version<>c.snapshot_schema_version
        OR r.card_codec_version<>c.card_codec_version
        OR r.event_interpreter_version<>c.event_interpreter_version
        OR r.protocol_version<>c.protocol_version
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CANONICAL_EXISTING_RELEASE_MISMATCH';
  END IF;

  -- The existing 2999 route was signed by V20261012_02 and therefore keeps
  -- its route-specific lookup/bundle hashes.  The identity tuple above is
  -- the immutable contract check; no hash is rewritten by this migration.

  INSERT INTO aoo_game_release(
      release_id,game_id,play_version,release_version,release_scope,
      catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
      catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
      status,rollout_percent,created_by,reason,validated_at,activated_at,
      snapshot_schema_version,card_codec_version,event_interpreter_version,
      protocol_version,source_hash)
  SELECT c.release_id,c.game_id,c.play_version,2026101203,'REGIONAL',
      JSON_SET(r.catalog_snapshot,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
        '$.releaseId',c.release_id,'$.indexGeneration',c.index_generation),
      JSON_SET(r.rule_snapshot,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
        '$.ruleContentHash',c.rule_content_hash,'$.releaseId',c.release_id,
        '$.indexGeneration',c.index_generation),
      JSON_SET(r.ui_snapshot,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.roomRuleSourceHash',c.source_hash,
        '$.roomRuleViewHash',c.source_hash,'$.releaseId',c.release_id,
        '$.indexGeneration',c.index_generation),
      JSON_SET(r.component_snapshot,'$.sourceHash',c.source_hash,
        '$.componentContentHash',c.component_content_hash,
        '$.contractVersion',c.contract_version),
      SHA2(CAST(JSON_SET(r.catalog_snapshot,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
        '$.releaseId',c.release_id,'$.indexGeneration',c.index_generation) AS CHAR),256),
      SHA2(CAST(JSON_SET(r.rule_snapshot,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
        '$.ruleContentHash',c.rule_content_hash,'$.releaseId',c.release_id,
        '$.indexGeneration',c.index_generation) AS CHAR),256),
      c.source_hash,
      SHA2(CAST(i.component_chain AS CHAR),256),
      SHA2(CONCAT('canonical-runtime-projection|',c.source_hash,'|',c.release_id,'|',c.index_generation),256),
      'ACTIVE',100,1,'Align signed r50 canonical runtime metadata projection',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),
      c.snapshot_schema_version,c.card_codec_version,c.event_interpreter_version,c.protocol_version,c.source_hash
    FROM canonical_runtime_projection_v20261012_03 c
    JOIN aoo_compiled_index_active a ON a.game_id=c.game_id AND a.region_code=c.region_code AND a.play_version=c.play_version
    JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code
      AND i.play_version=a.play_version AND i.index_generation=a.index_generation AND i.release_id=a.release_id
    JOIN aoo_game_release r ON r.release_id=a.release_id AND r.game_id=a.game_id AND r.play_version=a.play_version
   WHERE NOT EXISTS (SELECT 1 FROM aoo_game_release existing WHERE existing.release_id=c.release_id)
  ON DUPLICATE KEY UPDATE
      catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
      component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),
      ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),status='ACTIVE',
      rollout_percent=100,validated_at=VALUES(validated_at),activated_at=VALUES(activated_at),retired_at=NULL,
      snapshot_schema_version=VALUES(snapshot_schema_version),card_codec_version=VALUES(card_codec_version),
      event_interpreter_version=VALUES(event_interpreter_version),protocol_version=VALUES(protocol_version),
      source_hash=VALUES(source_hash);

  INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
    SELECT c.release_id,c.region_code,100,'ACTIVE'
      FROM canonical_runtime_projection_v20261012_03 c
  ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

  INSERT INTO aoo_compiled_room_create_index(
      game_id,region_code,play_version,index_generation,release_id,component_chain,
      rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
      compiled_at,validated_at,activated_at)
  SELECT c.game_id,c.region_code,c.play_version,c.index_generation,c.release_id,
      i.component_chain,
      JSON_SET(i.rule_validator,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
        '$.ruleContentHash',c.rule_content_hash,'$.releaseId',c.release_id,
        '$.indexGeneration',c.index_generation),
      JSON_SET(i.ui_schema,'$.gameId',c.game_id,'$.gameCode',c.game_code,
        '$.family',c.family_code,'$.playVersion',c.play_version,'$.regionCode',c.region_code,
        '$.contractVersion',c.contract_version,'$.roomRuleSourceHash',c.source_hash,
        '$.roomRuleViewHash',c.source_hash,'$.releaseId',c.release_id,
        '$.indexGeneration',c.index_generation),
      SHA2(CONCAT(c.game_code,'|',c.region_code,'|',c.play_version,'|',c.source_hash),256),
      SHA2(CONCAT('canonical-runtime-projection|',c.source_hash,'|',c.release_id,'|',c.index_generation),256),
      'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
    FROM canonical_runtime_projection_v20261012_03 c
    JOIN aoo_compiled_index_active a ON a.game_id=c.game_id AND a.region_code=c.region_code AND a.play_version=c.play_version
    JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code
      AND i.play_version=a.play_version AND i.index_generation=a.index_generation AND i.release_id=a.release_id
   WHERE NOT EXISTS (SELECT 1 FROM aoo_compiled_room_create_index existing
                      WHERE existing.game_id=c.game_id AND existing.region_code=c.region_code
                        AND existing.play_version=c.play_version AND existing.index_generation=c.index_generation
                        AND existing.release_id=c.release_id)
  ON DUPLICATE KEY UPDATE
      release_id=VALUES(release_id),component_chain=VALUES(component_chain),rule_validator=VALUES(rule_validator),
      ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),
      lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),activated_at=VALUES(activated_at),retired_at=NULL;

  UPDATE aoo_compiled_index_active a
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=a.game_id AND c.region_code=a.region_code AND c.play_version=a.play_version
     SET a.index_generation=c.index_generation,a.release_id=c.release_id,
         a.cache_epoch=a.cache_epoch+1,a.activation_reason='Activate signed r50 canonical runtime metadata projection',
         a.activated_at=CURRENT_TIMESTAMP(3);

  UPDATE aoo_compiled_room_create_index i
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=i.game_id AND c.region_code=i.region_code AND c.play_version=i.play_version
     SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
   WHERE i.lifecycle_state='ACTIVE' AND i.index_generation<>c.index_generation;

  UPDATE aoo_game_release_region rr
    JOIN aoo_game_release r ON r.release_id=rr.release_id
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=r.game_id AND c.play_version=r.play_version
     SET rr.status='RETIRED'
   WHERE rr.status='ACTIVE' AND rr.release_id<>c.release_id;

  UPDATE aoo_game_release r
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=r.game_id AND c.play_version=r.play_version
     SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
   WHERE r.status='ACTIVE' AND r.release_id<>c.release_id;

  UPDATE aoo_play_version p
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=p.game_id AND c.play_version=p.play_version
     SET p.content_hash=c.component_content_hash,p.status='ACTIVE',p.retired_at=NULL,
         p.activated_at=COALESCE(p.activated_at,CURRENT_TIMESTAMP(3));

  UPDATE aoo_published_game_configuration p
    JOIN canonical_runtime_projection_v20261012_03 c
      ON c.game_id=p.game_id AND c.play_version=p.play_version
     SET p.release_id=c.release_id,
         p.configuration_payload=JSON_SET(p.configuration_payload,
           '$.gameId',c.game_id,'$.gameCode',c.game_code,'$.family',c.family_code,
           '$.playVersion',c.play_version,'$.regionCode',c.region_code,
           '$.contractVersion',c.contract_version,'$.sourceHash',c.source_hash,
           '$.releaseId',c.release_id,'$.indexGeneration',c.index_generation);

  -- aoo_compiled_index_active is an active-only pointer table; it has no
  -- RETIRED state.  Preserve 630 history in the retired compiled-index and
  -- release rows, but remove its current pointer and cache epoch so the old
  -- identity cannot be admitted by a pointer-only lookup.
  DELETE FROM aoo_configuration_cache_epoch WHERE game_id=630;
  DELETE FROM aoo_compiled_index_active WHERE game_id=630;

  SELECT COUNT(*) INTO matched_identity_count
    FROM canonical_runtime_projection_v20261012_03 c
    JOIN aoo_compiled_index_active a ON a.game_id=c.game_id AND a.region_code=c.region_code AND a.play_version=c.play_version
      AND a.index_generation=c.index_generation AND a.release_id=c.release_id
    JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code
      AND i.play_version=a.play_version AND i.index_generation=a.index_generation AND i.release_id=a.release_id
      AND i.lifecycle_state='ACTIVE'
    JOIN aoo_game_release r ON r.release_id=a.release_id AND r.game_id=a.game_id AND r.play_version=a.play_version
      AND r.status='ACTIVE' AND r.source_hash=c.source_hash
      AND r.snapshot_schema_version=c.snapshot_schema_version AND r.card_codec_version=c.card_codec_version
      AND r.event_interpreter_version=c.event_interpreter_version AND r.protocol_version=c.protocol_version;
  IF matched_identity_count <> expected_identity_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CANONICAL_RUNTIME_PROJECTION_POSTCHECK_FAILED';
  END IF;

  COMMIT;
END$$

CALL align_canonical_runtime_projection_v20261012_03()$$
DROP PROCEDURE align_canonical_runtime_projection_v20261012_03$$

DELIMITER ;
