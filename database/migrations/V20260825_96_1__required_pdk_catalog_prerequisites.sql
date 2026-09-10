-- Corrective prerequisite for the PDK publication migrations.
--
-- The original publication chain expected these management-owned catalog rows
-- to have been seeded before Flyway ran.  A clean database has no such external
-- seed, so later versioned migrations could not create their FK children.
-- Existing rows are deliberately left untouched.  Conflicting game_code or
-- provider_key values still fail through the table's unique constraints.

INSERT INTO aoo_game_catalog(
    game_id,game_code,display_name,category_code,family_code,provider_key,
    catalog_schema_version,status)
SELECT 629,'njpdk','内江跑得快','POKER','POKER_UNCLASSIFIED',
       'business.global.pk.njpdk.NJPDKGameProvider',1,'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=629);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
SELECT 629,'CN-51','AVAILABLE',1
WHERE EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=629)
  AND NOT EXISTS (
      SELECT 1 FROM aoo_game_region WHERE game_id=629 AND region_code='CN-51');

INSERT INTO aoo_play_version(
    game_id,play_version,default_region_code,rule_schema_version,
    ui_schema_version,component_schema_version,content_hash,status,
    activated_at,created_by)
SELECT 629,'legacy-equivalent-1','CN-51',1,1,1,
       SHA2('business.global.pk.njpdk.NJPDKGameProvider:legacy-equivalent-1',256),
       'ACTIVE',CURRENT_TIMESTAMP(3),1
WHERE EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=629)
  AND NOT EXISTS (
      SELECT 1 FROM aoo_play_version
      WHERE game_id=629 AND play_version='legacy-equivalent-1');

INSERT INTO aoo_game_catalog(
    game_id,game_code,display_name,category_code,family_code,provider_key,
    catalog_schema_version,status)
SELECT 8,'pdk','成都跑得快','POKER','POKER_UNCLASSIFIED','catalog.pdk',1,'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=8);

INSERT INTO aoo_game_region(game_id,region_code,availability,priority)
SELECT 8,'GLOBAL','AVAILABLE',100
WHERE EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=8)
  AND NOT EXISTS (
      SELECT 1 FROM aoo_game_region WHERE game_id=8 AND region_code='GLOBAL');

INSERT INTO aoo_play_version(
    game_id,play_version,default_region_code,rule_schema_version,
    ui_schema_version,component_schema_version,content_hash,status,
    activated_at,created_by)
SELECT 8,'1.0.0','GLOBAL',1,1,1,SHA2('catalog.pdk:1.0.0',256),
       'ACTIVE',CURRENT_TIMESTAMP(3),1
WHERE EXISTS (SELECT 1 FROM aoo_game_catalog WHERE game_id=8)
  AND NOT EXISTS (
      SELECT 1 FROM aoo_play_version WHERE game_id=8 AND play_version='1.0.0');
