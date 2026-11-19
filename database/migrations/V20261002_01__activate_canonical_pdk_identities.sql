-- Activate the current PDK production identities.
--
-- The historical CD201/NJ201/... rows remain addressable for room recovery and
-- replay.  New catalog lookups must use gameId=2001..2028 and the canonical
-- chengdu2001..fuzhou2028 codes.  This migration publishes a new active release
-- for each identity instead of rewriting rows referenced by historical rooms.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

CREATE TEMPORARY TABLE canonical_pdk_identity (
  old_game_id BIGINT NOT NULL PRIMARY KEY,
  new_game_id BIGINT NOT NULL UNIQUE,
  old_code VARCHAR(32) NOT NULL UNIQUE,
  new_code VARCHAR(32) NOT NULL UNIQUE,
  old_version VARCHAR(64) NOT NULL,
  new_version VARCHAR(64) NOT NULL
);

INSERT INTO canonical_pdk_identity VALUES
  (8,2001,'CD201','chengdu2001','1.0.0','chengdu2001-v1.0.0'),
  (629,2002,'NJ201','neijiang2002','legacy-equivalent-1','neijiang2002-v1.0.0'),
  (90005,2003,'LS201','liangshan2003','xqp-equivalent-1','liangshan2003-v1.0.0'),
  (631,2004,'MN201','mianning2004','mn201-v1.0.0','mianning2004-v1.0.0'),
  (632,2005,'AY201','anyue2005','ay201-v1.0.0','anyue2005-v1.0.0'),
  (90006,2006,'PZ201','panzhihua2006','pz201-v1.0.0','panzhihua2006-v1.0.0'),
  (90007,2007,'WN201','wannian2007','legacy-equivalent-1','legacy-equivalent-1'),
  (90008,2008,'LP201','leping2008','legacy-equivalent-1','legacy-equivalent-1'),
  (90009,2009,'BZ201','bozhou2009','legacy-equivalent-1','legacy-equivalent-1'),
  (90010,2010,'YG201','yugan2010','legacy-equivalent-1','legacy-equivalent-1'),
  (90011,2011,'JA201','jian2011','legacy-equivalent-1','legacy-equivalent-1'),
  (90012,2012,'GS201','gushi2012','legacy-equivalent-1','legacy-equivalent-1'),
  (90013,2013,'XG201','xiaogan2013','legacy-equivalent-1','legacy-equivalent-1'),
  (90014,2014,'ND201','ningdu2014','legacy-equivalent-1','legacy-equivalent-1'),
  (90015,2015,'HH201','huaihua2015','legacy-equivalent-1','legacy-equivalent-1'),
  (90016,2016,'XX201','xinxian2016','legacy-equivalent-1','legacy-equivalent-1'),
  (90017,2017,'JD201','jingdezhen2017','legacy-equivalent-1','legacy-equivalent-1'),
  (90018,2018,'JS201','jiangsu2018','legacy-equivalent-1','legacy-equivalent-1'),
  (90019,2019,'HN201','henan2019','legacy-equivalent-1','legacy-equivalent-1'),
  (90020,2020,'TZ201','taizhou2020','legacy-equivalent-1','legacy-equivalent-1'),
  (90021,2021,'HB201','huaibin2021','legacy-equivalent-1','legacy-equivalent-1'),
  (90022,2022,'HC201','huangchuan2022','legacy-equivalent-1','legacy-equivalent-1'),
  (90023,2023,'PY201','poyang2023','legacy-equivalent-1','legacy-equivalent-1'),
  (90024,2024,'JT201','jintan2024','legacy-equivalent-1','legacy-equivalent-1'),
  (90025,2025,'GA201','gaoan2025','legacy-equivalent-1','legacy-equivalent-1'),
  (90026,2026,'YX201','yixing2026','legacy-equivalent-1','legacy-equivalent-1'),
  (90027,2027,'XC201','xuancheng2027','legacy-equivalent-1','legacy-equivalent-1'),
  (90028,2028,'FZ201','fuzhou2028','legacy-equivalent-1','legacy-equivalent-1');

INSERT INTO aoo_game_catalog
  (game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
SELECT m.new_game_id,m.new_code,g.display_name,g.category_code,'poker:pao-de-kuai',
       CONCAT('com.aoo.bcg.poker.PdkGameProvider:',m.new_code),g.catalog_schema_version,'ACTIVE'
FROM canonical_pdk_identity m
JOIN aoo_game_catalog g ON g.game_id=m.old_game_id
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code),display_name=VALUES(display_name),
  category_code=VALUES(category_code),family_code=VALUES(family_code),
  provider_key=VALUES(provider_key),catalog_schema_version=VALUES(catalog_schema_version),status='ACTIVE';

INSERT INTO aoo_play_version
  (game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,
   component_schema_version,content_hash,status,activated_at,created_by)
SELECT m.new_game_id,m.new_version,pv.default_region_code,pv.rule_schema_version,
       pv.ui_schema_version,pv.component_schema_version,
       SHA2(CONCAT('canonical-pdk|',m.new_code,'|',m.new_version),256),'ACTIVE',
       CURRENT_TIMESTAMP(3),pv.created_by
FROM canonical_pdk_identity m
JOIN aoo_play_version pv ON pv.game_id=m.old_game_id AND pv.play_version=m.old_version
WHERE pv.status='ACTIVE'
ON DUPLICATE KEY UPDATE default_region_code=VALUES(default_region_code),
  rule_schema_version=VALUES(rule_schema_version),ui_schema_version=VALUES(ui_schema_version),
  component_schema_version=VALUES(component_schema_version),content_hash=VALUES(content_hash),
  status='ACTIVE',activated_at=COALESCE(aoo_play_version.activated_at,VALUES(activated_at));

INSERT INTO aoo_game_region(game_id,region_code,availability,priority,available_from,available_until)
SELECT m.new_game_id,gr.region_code,'AVAILABLE',gr.priority,gr.available_from,gr.available_until
FROM canonical_pdk_identity m
JOIN aoo_game_region gr ON gr.game_id=m.old_game_id
WHERE gr.availability='AVAILABLE'
ON DUPLICATE KEY UPDATE availability='AVAILABLE',priority=VALUES(priority),
  available_from=VALUES(available_from),available_until=VALUES(available_until);

INSERT INTO aoo_game_service_route(route_id,game_id,play_version,endpoint,priority,status)
SELECT m.new_game_id*1000000+1,m.new_game_id,m.new_version,r.endpoint,r.priority,'ACTIVE'
FROM canonical_pdk_identity m
JOIN aoo_game_service_route r ON r.game_id=m.old_game_id AND r.play_version=m.old_version
WHERE r.status='ACTIVE'
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),
  endpoint=VALUES(endpoint),priority=VALUES(priority),status='ACTIVE';

-- Publish one canonical active release per game. The old release remains valid for
-- rooms that were created before this migration.
INSERT INTO aoo_game_release
  (release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,
   ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
   status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT m.new_game_id*1000000000+2026100201,m.new_game_id,m.new_version,r.release_version+1,
       r.release_scope,
       JSON_SET(CAST(REPLACE(REPLACE(CAST(r.catalog_snapshot AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
          '$.gameId',m.new_game_id,'$.gameCode',m.new_code,'$.provider',CONCAT('com.aoo.bcg.poker.PdkGameProvider:',m.new_code),'$.playVersion',m.new_version),
       CAST(REPLACE(REPLACE(CAST(r.rule_snapshot AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       CAST(REPLACE(REPLACE(CAST(r.ui_snapshot AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       CAST(REPLACE(REPLACE(CAST(r.component_snapshot AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       SHA2(CONCAT(m.new_code,'|catalog|',m.new_version),256),
       SHA2(CONCAT(m.new_code,'|rules|',m.new_version),256),
       SHA2(CONCAT(m.new_code,'|ui|',m.new_version),256),
       SHA2(CONCAT(m.new_code,'|components|',m.new_version),256),
       SHA2(CONCAT(m.new_code,'|bundle|',m.new_version),256),
       'ACTIVE',r.rollout_percent,r.created_by,CONCAT('Activate canonical PDK identity ',m.new_code),
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM canonical_pdk_identity m
JOIN aoo_game_release r ON r.game_id=m.old_game_id AND r.play_version=m.old_version AND r.status='ACTIVE'
ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),release_version=VALUES(release_version),
  catalog_snapshot=VALUES(catalog_snapshot),rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
  component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),rule_hash=VALUES(rule_hash),
  ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),bundle_hash=VALUES(bundle_hash),status='ACTIVE',
  rollout_percent=VALUES(rollout_percent),reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT m.new_game_id*1000000000+2026100201,rr.region_code,rr.rollout_percent,'ACTIVE'
FROM canonical_pdk_identity m
JOIN aoo_game_release oldr ON oldr.game_id=m.old_game_id AND oldr.play_version=m.old_version AND oldr.status='ACTIVE'
JOIN aoo_game_release_region rr ON rr.release_id=oldr.release_id AND rr.status='ACTIVE'
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index
  (game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,
   lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at)
SELECT m.new_game_id,i.region_code,m.new_version,2026100201,m.new_game_id*1000000000+2026100201,
       CAST(REPLACE(REPLACE(CAST(i.component_chain AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       CAST(REPLACE(REPLACE(CAST(i.rule_validator AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       CAST(REPLACE(REPLACE(CAST(i.ui_schema AS CHAR),m.old_code,m.new_code),m.old_version,m.new_version) AS JSON),
       SHA2(CONCAT(m.new_code,'|index|',2026100201,'|',m.new_version),256),
       SHA2(CONCAT(m.new_code,'|bundle|',m.new_version),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM canonical_pdk_identity m
JOIN aoo_compiled_room_create_index i ON i.game_id=m.old_game_id AND i.play_version=m.old_version AND i.lifecycle_state='ACTIVE'
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),lookup_hash=VALUES(lookup_hash),
  bundle_hash=VALUES(bundle_hash),lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),activated_at=VALUES(activated_at);

INSERT INTO aoo_compiled_index_active
  (game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at)
SELECT m.new_game_id,i.region_code,m.new_version,2026100201,m.new_game_id*1000000000+2026100201,
       i.cache_epoch+1,i.activated_by,CONCAT('Activate canonical PDK identity ',m.new_code),CURRENT_TIMESTAMP(3)
FROM canonical_pdk_identity m
JOIN aoo_compiled_index_active i ON i.game_id=m.old_game_id AND i.play_version=m.old_version
ON DUPLICATE KEY UPDATE release_id=VALUES(release_id),cache_epoch=VALUES(cache_epoch),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);

INSERT INTO aoo_room_cost_policy
  (game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,
   currency_code,cost_minor,club_cost_minor,union_cost_minor,policy_version,content_hash,status)
SELECT m.new_game_id,m.new_version,p.region_code,p.measure_kind,p.measure_value,p.round_count,p.player_count,
       p.payer_mode,p.currency_code,p.cost_minor,p.club_cost_minor,p.union_cost_minor,p.policy_version+1,
       SHA2(CONCAT('canonical-pdk|',m.new_code,'|',m.new_version,'|',p.region_code,'|',p.measure_kind,'|',p.measure_value,'|',p.round_count,'|',p.player_count,'|',p.payer_mode,'|',p.cost_minor),256),'ACTIVE'
FROM canonical_pdk_identity m
JOIN aoo_room_cost_policy p ON p.game_id=m.old_game_id AND p.play_version=m.old_version AND p.status='ACTIVE'
ON DUPLICATE KEY UPDATE cost_minor=VALUES(cost_minor),club_cost_minor=VALUES(club_cost_minor),
  union_cost_minor=VALUES(union_cost_minor),policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';

INSERT INTO aoo_room_rule_definition
  (game_id,play_version,rule_key,rule_kind,value_type,required_value,default_value,minimum_value,
   maximum_value,validation_pattern,description,ordinal,schema_version,content_hash,status)
SELECT m.new_game_id,m.new_version,
       REPLACE(REPLACE(d.rule_key,m.old_code,m.new_code),m.old_version,m.new_version),d.rule_kind,d.value_type,
       d.required_value,d.default_value,d.minimum_value,d.maximum_value,d.validation_pattern,d.description,
       d.ordinal,d.schema_version,
       SHA2(CONCAT(m.new_code,'|',m.new_version,'|',d.rule_key,'|',d.schema_version),256),'ACTIVE'
FROM canonical_pdk_identity m
JOIN aoo_room_rule_definition d ON d.game_id=m.old_game_id AND d.play_version=m.old_version AND d.status='ACTIVE'
ON DUPLICATE KEY UPDATE default_value=VALUES(default_value),minimum_value=VALUES(minimum_value),maximum_value=VALUES(maximum_value),
  validation_pattern=VALUES(validation_pattern),description=VALUES(description),ordinal=VALUES(ordinal),
  schema_version=VALUES(schema_version),content_hash=VALUES(content_hash),status='ACTIVE';

-- Retire old catalog identities only. Historical releases, rooms and snapshots
-- continue to reference their original IDs and remain recoverable.
UPDATE aoo_game_region gr JOIN canonical_pdk_identity m ON m.old_game_id=gr.game_id
SET gr.availability='RETIRED'
WHERE gr.availability='AVAILABLE';
UPDATE aoo_game_catalog g JOIN canonical_pdk_identity m ON m.old_game_id=g.game_id
SET g.status='RETIRED';

DROP TEMPORARY TABLE canonical_pdk_identity;
COMMIT;
