-- The canonical Neijiang identity is 2002/neijiang2002.  Its historical
-- 629/NJ201 policy rows are retained for old rooms, but billing for a new
-- canonical room must resolve against the canonical identity and city scope.
-- Keep this repair explicit and idempotent so initialization order cannot leave
-- the default 8-round room without a cost policy.
INSERT INTO aoo_room_cost_policy(
  game_id,play_version,region_code,measure_kind,measure_value,round_count,
  player_count,payer_mode,currency_code,cost_minor,club_cost_minor,
  union_cost_minor,policy_version,content_hash,status)
SELECT 2002,'neijiang2002-v1.0.0','CN-51-10','ROUND_COUNT',round_count,
       round_count,player_count,'OWNER','ROOM_CARD',0,0,0,1,
       SHA2(CONCAT('canonical-neijiang2002|',round_count,'|',player_count,
                   '|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (SELECT 8 AS round_count UNION ALL SELECT 12 UNION ALL SELECT 16) rounds
CROSS JOIN (SELECT 2 AS player_count UNION ALL SELECT 3) players
WHERE TRUE
ON DUPLICATE KEY UPDATE
  measure_kind=VALUES(measure_kind),measure_value=VALUES(measure_value),
  currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),
  club_cost_minor=VALUES(club_cost_minor),union_cost_minor=VALUES(union_cost_minor),
  policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),
  status='ACTIVE';
