-- The three canonical Poker families use their own rule names, but billing is
-- deliberately shared and indexes the normalized round/capacity dimensions.
-- Local development policies are free until production pricing is published.
INSERT INTO aoo_room_cost_policy(
    game_id,play_version,region_code,round_count,player_count,payer_mode,
    currency_code,cost_minor,policy_version,content_hash,status)
SELECT game_id,play_version,region_code,round_count,player_count,'OWNER',
       'ROOM_CARD',0,1,
       SHA2(CONCAT(game_id,'|',play_version,'|',round_count,'|',player_count,'|OWNER|ROOM_CARD|0'),256),
       'ACTIVE'
FROM (
    SELECT 630 game_id,'cd299-v1.0.0' play_version,'CN-51-01' region_code,8 round_count,8 player_count
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',10,8
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',10,10
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',20,8
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',20,10
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',30,8
    UNION ALL SELECT 5,'cn298-v1.0.0','GLOBAL',30,10
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',10,8
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',10,10
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',20,8
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',20,10
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',30,8
    UNION ALL SELECT 9,'cn297-v1.0.0','GLOBAL',30,10
) policies
ON DUPLICATE KEY UPDATE currency_code=VALUES(currency_code),cost_minor=VALUES(cost_minor),
    policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status='ACTIVE';
