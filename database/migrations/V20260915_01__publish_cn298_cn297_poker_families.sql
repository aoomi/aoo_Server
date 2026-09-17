-- Publish canonical Poker identities. Existing historical keys 5/9 are retained;
-- CD299 receives new proxy key 630, the first unused regular catalog key after 629.

INSERT INTO aoo_game_family(family_code,category_code,display_name,status)
VALUES ('poker:betting','POKER','下注类扑克','ACTIVE'),
       ('poker:compare-hand','POKER','比牌类扑克','ACTIVE'),
       ('poker:cd299','POKER','扯旋','ACTIVE')
ON DUPLICATE KEY UPDATE category_code=VALUES(category_code),display_name=VALUES(display_name),status='ACTIVE';

UPDATE aoo_game_catalog
SET game_code='CN298',display_name='牛牛',category_code='POKER',family_code='poker:betting',
    provider_key='com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory',status='ACTIVE',row_version=row_version+1
WHERE game_id=5;

INSERT INTO aoo_game_catalog(
    game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
SELECT 5,'CN298','牛牛','POKER','poker:betting',
       'com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory',1,'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=5);

UPDATE aoo_game_catalog
SET game_code='CN297',display_name='金花',category_code='POKER',family_code='poker:compare-hand',
    provider_key='business.global.pk.zjh.ZJHPokerFamilyProviderFactory',status='ACTIVE',row_version=row_version+1
WHERE game_id=9;

INSERT INTO aoo_game_catalog(
    game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
SELECT 9,'CN297','金花','POKER','poker:compare-hand',
       'business.global.pk.zjh.ZJHPokerFamilyProviderFactory',1,'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=9);

INSERT INTO aoo_game_catalog(
    game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
SELECT 630,'CD299','扯旋','POKER','poker:cd299',
       'com.aoo.bcg.poker.cd299.Cd299ProviderFactory',1,'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=630)
  AND NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_code='CD299');

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES (5,'GLOBAL','AVAILABLE',100),(9,'GLOBAL','AVAILABLE',100),(630,'CN-51-01','AVAILABLE',100)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority);

INSERT INTO aoo_play_version(
    game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
    component_schema_version,content_hash,status,activated_at,created_by)
VALUES
    (5,'cn298-v1.0.0','GLOBAL',1,1,1,SHA2('CN298|poker:betting|cn298-v1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
    (9,'cn297-v1.0.0','GLOBAL',1,1,1,SHA2('CN297|poker:compare-hand|cn297-v1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1),
    (630,'cd299-v1.0.0','CN-51-01',1,1,1,SHA2('CD299|poker:cd299|cd299-v1.0.0',256),'ACTIVE',CURRENT_TIMESTAMP(3),1)
ON DUPLICATE KEY UPDATE default_region_code='GLOBAL',content_hash=VALUES(content_hash),
    status='ACTIVE',activated_at=CURRENT_TIMESTAMP(3),row_version=row_version+1;

INSERT INTO aoo_game_release(
    release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
    catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
    status,rollout_percent,created_by,reason,validated_at,activated_at)
VALUES
    (502026091501,5,'cn298-v1.0.0',1,'GLOBAL',
     JSON_OBJECT('gameId',5,'gameCode','CN298','family','poker:betting'),
     JSON_OBJECT('family','poker:betting','schema','canonical-room-v1'),JSON_OBJECT(),
     JSON_OBJECT('factory','com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory'),
     SHA2('CN298-catalog-v1',256),SHA2('CN298-rules-v1',256),SHA2('CN298-ui-v1',256),
     SHA2('CN298-components-v1',256),SHA2('CN298-runtime-v1',256),
     'ACTIVE',100,1,'Publish canonical CN298 runtime',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)),
    (902026091501,9,'cn297-v1.0.0',1,'GLOBAL',
     JSON_OBJECT('gameId',9,'gameCode','CN297','family','poker:compare-hand'),
     JSON_OBJECT('family','poker:compare-hand','schema','canonical-room-v1'),JSON_OBJECT(),
     JSON_OBJECT('factory','business.global.pk.zjh.ZJHPokerFamilyProviderFactory'),
     SHA2('CN297-catalog-v1',256),SHA2('CN297-rules-v1',256),SHA2('CN297-ui-v1',256),
     SHA2('CN297-components-v1',256),SHA2('CN297-runtime-v1',256),
     'ACTIVE',100,1,'Publish canonical CN297 runtime',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)),
    (6302026091501,630,'cd299-v1.0.0',1,'REGIONAL',
     JSON_OBJECT('gameId',630,'gameCode','CD299','family','poker:cd299'),
     JSON_OBJECT('family','poker:cd299','schema','canonical-room-v1'),
     JSON_OBJECT('scene','GameRoom2D','bundle','poker01-prefab','playFamily','poker:cd299','landscapePrefab','Games/Poker/CX/CD299/Prefab/Landscape/CD299RoomLandscape','portraitPrefab','Games/Poker/CX/CD299/Prefab/Portrait/CD299RoomPortrait'),
     JSON_OBJECT('factory','com.aoo.bcg.poker.cd299.Cd299ProviderFactory'),
     SHA2('CD299-catalog-v1',256),SHA2('CD299-rules-v1',256),SHA2('CD299-ui-v1',256),
     SHA2('CD299-components-v1',256),SHA2('CD299-runtime-v1',256),
     'ACTIVE',100,1,'Publish canonical CD299 runtime',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),
    component_snapshot=VALUES(component_snapshot),status='ACTIVE',rollout_percent=100,
    validated_at=CURRENT_TIMESTAMP(3),activated_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
VALUES (502026091501,'GLOBAL',100,'ACTIVE'),(902026091501,'GLOBAL',100,'ACTIVE'),
       (6302026091501,'CN-51-01',100,'ACTIVE')
ON DUPLICATE KEY UPDATE rollout_percent=100,status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,component_chain,
    rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
    compiled_at,validated_at,activated_at)
VALUES
    (5,'GLOBAL','cn298-v1.0.0',2026091501,502026091501,
     JSON_ARRAY('com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory@cn298-v1.0.0'),
     JSON_OBJECT('gameCode','CN298'),JSON_OBJECT(),SHA2('CN298-index-v1',256),
     SHA2('CN298-runtime-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)),
    (9,'GLOBAL','cn297-v1.0.0',2026091501,902026091501,
     JSON_ARRAY('business.global.pk.zjh.ZJHPokerFamilyProviderFactory@cn297-v1.0.0'),
     JSON_OBJECT('gameCode','CN297'),JSON_OBJECT(),SHA2('CN297-index-v1',256),
     SHA2('CN297-runtime-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)),
    (630,'CN-51-01','cd299-v1.0.0',2026091501,6302026091501,
     JSON_ARRAY('com.aoo.bcg.poker.cd299.Cd299ProviderFactory@cd299-v1.0.0'),
     JSON_OBJECT('gameCode','CD299'),
     JSON_OBJECT('scene','GameRoom2D','bundle','poker01-prefab','playFamily','poker:cd299','landscapePrefab','Games/Poker/CX/CD299/Prefab/Landscape/CD299RoomLandscape','portraitPrefab','Games/Poker/CX/CD299/Prefab/Portrait/CD299RoomPortrait'),
     SHA2('CD299-index-v1',256),SHA2('CD299-runtime-v1',256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),
    rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),
    bundle_hash=VALUES(bundle_hash),lifecycle_state='ACTIVE',validated_at=CURRENT_TIMESTAMP(3),
    activated_at=CURRENT_TIMESTAMP(3),retired_at=NULL;

INSERT INTO aoo_compiled_index_active(
    game_id,region_code,play_version,index_generation,release_id,cache_epoch,
    activated_by,activation_reason,activated_at)
VALUES
    (5,'GLOBAL','cn298-v1.0.0',2026091501,502026091501,1,1,'Activate CN298 runtime',CURRENT_TIMESTAMP(3)),
    (9,'GLOBAL','cn297-v1.0.0',2026091501,902026091501,1,1,'Activate CN297 runtime',CURRENT_TIMESTAMP(3)),
    (630,'CN-51-01','cd299-v1.0.0',2026091501,6302026091501,1,1,'Activate CD299 runtime',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE play_version=VALUES(play_version),index_generation=VALUES(index_generation),
    release_id=VALUES(release_id),cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason=VALUES(activation_reason),activated_at=CURRENT_TIMESTAMP(3);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
VALUES
    (502026091501,5,'cn298-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE'),
    (902026091501,9,'cn297-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE'),
    (6302026091501,630,'cd299-v1.0.0','ws://127.0.0.1:8080/api/v2/gateway/ws',100,'ACTIVE')
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint),priority=100,status='ACTIVE';
