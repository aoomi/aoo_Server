-- Region is a catalog classification tag only. NJPDK remains bound to its
-- already-published authority index and route; no account, wallet, room or
-- service-routing boundary is introduced by this migration.
INSERT INTO aoo_region(
    region_code,parent_region_code,region_type,country_code,
    subdivision_code,display_name,path,depth,status)
VALUES('CN-51-10','CN-51','CITY','CN','5110','内江','/CN/CN-51/CN-51-10',2,'ACTIVE')
ON DUPLICATE KEY UPDATE
    parent_region_code=VALUES(parent_region_code),
    region_type=VALUES(region_type),
    country_code=VALUES(country_code),
    subdivision_code=VALUES(subdivision_code),
    display_name=VALUES(display_name),
    path=VALUES(path),
    depth=VALUES(depth),
    status='ACTIVE';

INSERT INTO aoo_region_alias(
    source_system,source_region_code,region_code,mapping_status,verified_at)
VALUES
    ('canonical-adcode','511000','CN-51-10','VERIFIED',CURRENT_TIMESTAMP(3)),
    ('legacy-city-id','1041000','CN-51-10','VERIFIED',CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    region_code=VALUES(region_code),
    mapping_status='VERIFIED',
    verified_at=VALUES(verified_at);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
VALUES(629,'CN-51-10','AVAILABLE',1)
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=1;

UPDATE aoo_game_region
SET availability='RETIRED'
WHERE game_id=629 AND region_code='CN-51';
