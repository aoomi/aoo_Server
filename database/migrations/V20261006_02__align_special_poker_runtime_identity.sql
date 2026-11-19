-- Publish a new immutable release generation for the three specialised poker
-- identities. Earlier catalog rows used display aliases (CN298/CN297/CD299)
-- while providers and client protocols use quanguo2998/quanguo2997/chengdu2999.
-- Validated releases and compiled indexes are immutable, so history is never
-- rewritten; a corrected generation is created and activated instead.

START TRANSACTION;

UPDATE aoo_game_catalog
   SET game_code=CASE game_id
       WHEN 5 THEN 'quanguo2998'
       WHEN 9 THEN 'quanguo2997'
       WHEN 630 THEN 'chengdu2999'
       END,
       row_version=row_version+1
 WHERE game_id IN (5,9,630)
   AND status='ACTIVE';

INSERT INTO aoo_game_release(
    release_id,game_id,play_version,release_version,release_scope,
    catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
    catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
    status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT CASE r.game_id
         WHEN 5 THEN 502026100602
         WHEN 9 THEN 902026100602
         WHEN 630 THEN 6302026100602
       END,
       r.game_id,r.play_version,2026100602,r.release_scope,
       JSON_SET(COALESCE(r.catalog_snapshot,JSON_OBJECT()),'$.gameCode',
         CASE r.game_id WHEN 5 THEN 'quanguo2998' WHEN 9 THEN 'quanguo2997' WHEN 630 THEN 'chengdu2999' END),
       JSON_SET(COALESCE(r.rule_snapshot,JSON_OBJECT()),
         '$.gameCode',CASE r.game_id WHEN 5 THEN 'quanguo2998' WHEN 9 THEN 'quanguo2997' WHEN 630 THEN 'chengdu2999' END,
         '$.roomRuleSchema.gameCode',CASE r.game_id WHEN 5 THEN 'quanguo2998' WHEN 9 THEN 'quanguo2997' WHEN 630 THEN 'chengdu2999' END),
       r.ui_snapshot,r.component_snapshot,
       SHA2(CONCAT('special-poker-catalog|',r.game_id,'|2026100602'),256),
       SHA2(CONCAT('special-poker-rules|',r.game_id,'|2026100602'),256),
       r.ui_hash,r.component_hash,
       SHA2(CONCAT('special-poker-runtime|',r.game_id,'|2026100602'),256),
       'ACTIVE',100,r.created_by,
       'Align specialised poker runtime identity without mutating validated history',
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_game_release r
JOIN aoo_compiled_index_active a
  ON a.game_id=r.game_id AND a.release_id=r.release_id
WHERE r.game_id IN (5,9,630)
  AND r.status='ACTIVE';

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT CASE r.game_id
         WHEN 5 THEN 502026100602
         WHEN 9 THEN 902026100602
         WHEN 630 THEN 6302026100602
       END,
       rr.region_code,100,'ACTIVE'
FROM aoo_game_release r
JOIN aoo_compiled_index_active a
  ON a.game_id=r.game_id AND a.release_id=r.release_id
JOIN aoo_game_release_region rr ON rr.release_id=r.release_id
WHERE r.game_id IN (5,9,630)
  AND r.status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
    game_id,region_code,play_version,index_generation,release_id,component_chain,
    rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,
    compiled_at,validated_at,activated_at)
SELECT i.game_id,i.region_code,i.play_version,2026100602,
       CASE i.game_id WHEN 5 THEN 502026100602 WHEN 9 THEN 902026100602 WHEN 630 THEN 6302026100602 END,
       i.component_chain,
       JSON_SET(COALESCE(i.rule_validator,JSON_OBJECT()),'$.gameCode',
         CASE i.game_id WHEN 5 THEN 'quanguo2998' WHEN 9 THEN 'quanguo2997' WHEN 630 THEN 'chengdu2999' END),
       i.ui_schema,
       SHA2(CONCAT('special-poker-index|',i.game_id,'|2026100602'),256),
       i.bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM aoo_compiled_room_create_index i
JOIN aoo_compiled_index_active a
  ON a.game_id=i.game_id AND a.region_code=i.region_code
 AND a.play_version=i.play_version AND a.index_generation=i.index_generation
WHERE i.game_id IN (5,9,630)
  AND i.lifecycle_state='ACTIVE';

UPDATE aoo_compiled_room_create_index i
JOIN aoo_compiled_index_active a
  ON a.game_id=i.game_id AND a.region_code=i.region_code
 AND a.play_version=i.play_version AND a.index_generation=i.index_generation
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.game_id IN (5,9,630) AND i.lifecycle_state='ACTIVE';

UPDATE aoo_game_release r
JOIN aoo_compiled_index_active a ON a.game_id=r.game_id AND a.release_id=r.release_id
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.game_id IN (5,9,630) AND r.status='ACTIVE';

UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
SET rr.status='RETIRED'
WHERE r.game_id IN (5,9,630) AND r.status='RETIRED' AND rr.status='ACTIVE';

UPDATE aoo_compiled_index_active
SET index_generation=2026100602,
    release_id=CASE game_id WHEN 5 THEN 502026100602 WHEN 9 THEN 902026100602 WHEN 630 THEN 6302026100602 END,
    cache_epoch=cache_epoch+1,activated_by=1,
    activation_reason='Align specialised poker runtime identity',
    activated_at=CURRENT_TIMESTAMP(3)
WHERE game_id IN (5,9,630);

UPDATE aoo_published_game_configuration
SET release_id=CASE game_id WHEN 5 THEN 502026100602 WHEN 9 THEN 902026100602 WHEN 630 THEN 6302026100602 END,
    reason='Align specialised poker runtime identity',created_at=CURRENT_TIMESTAMP(3)
WHERE game_id IN (5,9,630);

COMMIT;
