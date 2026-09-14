-- 成都跑得快权威开房表允许 8/12/16 局。付费策略是平台治理配置，
-- 不写入用户维护的玩法 Excel；个人房当前统一采用 OWNER。
INSERT INTO aoo_room_cost_policy(
 game_id,play_version,region_code,round_count,player_count,payer_mode,
 currency_code,cost_minor,policy_version,content_hash,status)
SELECT game_id,play_version,region_code,12,player_count,'OWNER','ROOM_CARD',0,1,
       SHA2(CONCAT(game_id,'|',play_version,'|',region_code,'|12|',player_count,'|OWNER|ROOM_CARD|0'),256),'ACTIVE'
FROM (
 SELECT 8 game_id,'1.0.0' play_version,'CN-51-01' region_code
 UNION ALL
 SELECT 629,'legacy-equivalent-1','CN-51-10'
) games
CROSS JOIN (SELECT 2 player_count UNION ALL SELECT 3 UNION ALL SELECT 4) players
WHERE TRUE
ON DUPLICATE KEY UPDATE
 currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),
 policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';
