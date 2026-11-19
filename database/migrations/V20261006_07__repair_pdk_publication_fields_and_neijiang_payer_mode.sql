-- Complete the publication payload for the five identities whose active
-- publication currently has an empty room-rule field projection.  The source
-- fields below are the same canonical preview projections already recorded for
-- these identities; no rule default is inferred here.  payerMode is a
-- server-owned validator field and is deliberately fixed to OWNER, matching
-- the billing policy dimension and the runtime's exact enum.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
START TRANSACTION;

CREATE TEMPORARY TABLE pdk_publication_repair (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  game_code VARCHAR(64) NOT NULL UNIQUE,
  play_version VARCHAR(64) NOT NULL UNIQUE,
  region_code VARCHAR(32) NOT NULL,
  source_hash CHAR(64) NOT NULL,
  rule_fields JSON NOT NULL
);

INSERT INTO pdk_publication_repair(game_id,game_code,play_version,region_code,source_hash,rule_fields)
VALUES
  (2001,'chengdu2001','chengdu2001-v1.0.0','CN-51-01','3c85d2454b22249cec48df543d292fe1b55b77acfe43987007ef91891b71a72e',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":70,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTimeoutSeconds","label":"操作时间","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10000,"options":[{"value":10000,"label":"10000秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":20,"label":"20秒","order":30,"disabled":false}]},{"key":"firstPlayRule","label":"先出牌","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"winner_first","options":[{"value":"winner_first","label":"赢家先出","order":10,"disabled":false},{"value":"spade_three_first","label":"黑桃3优先","order":20,"disabled":false}]},{"key":"bombScore","label":"炸弹记分","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":5,"options":[{"value":5,"label":"5分","order":10,"disabled":false},{"value":10,"label":"10分","order":20,"disabled":false},{"value":20,"label":"20分","order":30,"disabled":false},{"value":30,"label":"30分","order":40,"disabled":false}]},{"key":"rule_chengdu2001_0001","label":"小结算","control":"checkbox","order":120,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"playRule","label":"玩法","control":"checkbox","order":130,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[2,3,4,5],"defaultValue":["four_with_two","triple_ace_bomb","remove_three_four","require_spade_three"],"options":[{"value":"three_no_attachment","label":"三不带","order":10,"disabled":false},{"value":"four_with_two","label":"四带二","order":20,"disabled":false},{"value":"triple_ace_bomb","label":"3A算炸弹","order":30,"disabled":false},{"value":"remove_three_four","label":"去掉3、4","order":40,"disabled":false},{"value":"require_spade_three","label":"必须出黑桃3","order":50,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":140,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"roomRestriction","label":"其他","control":"checkbox","order":150,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"超时托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近警告","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]}]' AS JSON)),
  (2004,'mianning2004','mianning2004-v1.0.0','CN-51-34-33','648e76a9a41930aad64a1bdb4be7a921df04faf57d60736b9d832426ba2705d7',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false}]},{"key":"cardsPerPlayer","label":"发牌张数","control":"radio","order":91,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":13,"options":[{"value":13,"label":"13张","order":10,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"robDealerRule","label":"抢庄","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"rob_dealer","options":[{"value":"rob_dealer","label":"抢庄","order":10,"disabled":false},{"value":"first_round_no_compete","label":"首局不抢","order":20,"disabled":false},{"value":"dealer_cannot_rob_when_idle_pass","label":"闲家都不抢时庄家不能抢","order":30,"disabled":false}]},{"key":"bombScore","label":"炸弹记分","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"no_multiplier","options":[{"value":"no_multiplier","label":"不加番","order":10,"disabled":false},{"value":1,"label":"1番","order":20,"disabled":false},{"value":"unlimited_multiplier","label":"无限番","order":30,"disabled":false}]},{"key":"rule_mianning2004_0001","label":"小结算","control":"checkbox","order":120,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"roomRestriction","label":"其他","control":"checkbox","order":130,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"5次不操作进入托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近提示","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":140,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]}]' AS JSON)),
  (2005,'anyue2005','anyue2005-v1.0.0','CN-51-20-21','4a98640193e56317b8b84d5ad6a1b22460b11077305ec6192b1fc82d2537d241',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTimeoutSeconds","label":"操作时间","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10,"options":[{"value":10,"label":"10秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":20,"label":"20秒","order":30,"disabled":false}]},{"key":"deckRange","label":"牌库范围","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"cut_40","options":[{"value":"cut_40","label":"5-K，2一张，A三张","order":10,"disabled":false}]},{"key":"firstPlayRule","label":"先出牌","control":"radio","order":120,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"spade_five_first","options":[{"value":"spade_five_first","label":"黑桃5优先出","order":10,"disabled":false}]},{"key":"requiredFirstCardRule","label":"首张必出","control":"radio","order":130,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"first_round_spade_five_required","options":[{"value":"first_round_spade_five_required","label":"第一局必须出黑桃5","order":10,"disabled":false}]},{"key":"tripleWithTwoMode","label":"三带二","control":"radio","order":140,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"two_cards_no_compare","options":[{"value":"two_cards_no_compare","label":"只能带两张且不比较带牌","order":10,"disabled":false},{"value":"two_cards_compare","label":"只能带两张且比较带牌","order":20,"disabled":false}]},{"key":"planeWithTwoMode","label":"飞机带二","control":"radio","order":150,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"two_cards_no_compare","options":[{"value":"two_cards_no_compare","label":"只能带两张且不比较带牌","order":10,"disabled":false},{"value":"two_cards_compare","label":"只能带两张且比较带牌","order":20,"disabled":false}]},{"key":"bombScore","label":"炸弹记分","control":"radio","order":160,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":5,"options":[{"value":5,"label":"5分","order":10,"disabled":false},{"value":10,"label":"10分","order":20,"disabled":false},{"value":20,"label":"20分","order":30,"disabled":false}]},{"key":"rule_anyue2005_0001","label":"小结算","control":"checkbox","order":170,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"playRule","label":"玩法","control":"checkbox","order":180,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1,2,3,4],"defaultValue":["three_no_attachment","plane_no_attachment","four_with_two","triple_ace_bomb"],"options":[{"value":"three_no_attachment","label":"三不带（任何时候可出）","order":10,"disabled":false},{"value":"plane_no_attachment","label":"飞机不带（任何时候可出）","order":20,"disabled":false},{"value":"four_with_two","label":"四带二（只能带两张）","order":30,"disabled":false},{"value":"triple_ace_bomb","label":"3A算炸弹","order":40,"disabled":false},{"value":"four_threes_direct_win","label":"四个3直接赢","order":50,"disabled":false},{"value":"triple_ace_plus_two_direct_win","label":"三个A加一个2直接赢","order":60,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":190,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"roomRestriction","label":"其他","control":"checkbox","order":200,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"5次不操作进入托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近提示","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]}]' AS JSON)),
  (2006,'panzhihua2006','panzhihua2006-v1.0.0','CN-51-04','d0d4dc518b8acbae16df64756c9e0b6f32278568b395eee01b4809f9657219e7',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false},{"value":4,"label":"4人","order":30,"disabled":false}]},{"key":"cardsPerPlayer","label":"发牌张数","control":"radio","order":91,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10,"options":[{"value":10,"label":"10张","order":10,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTimeoutSeconds","label":"操作时间","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10,"options":[{"value":10,"label":"10秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":30,"label":"30秒","order":30,"disabled":false},{"value":60,"label":"60秒","order":40,"disabled":false}]},{"key":"requiredFirstCardRule","label":"首张必出","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"first_round_spade_five_then_winner","options":[{"value":"first_round_spade_five_then_winner","label":"首局黑桃5先出，下局赢家先出","order":10,"disabled":false}]},{"key":"bombScore","label":"炸弹记分","control":"radio","order":120,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":10,"options":[{"value":10,"label":"10分","order":10,"disabled":false},{"value":20,"label":"20分","order":20,"disabled":false},{"value":30,"label":"30分","order":30,"disabled":false}]},{"key":"specialPatternScore","label":"特殊牌型计分","control":"radio","order":130,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":5,"options":[{"value":5,"label":"5分","order":10,"disabled":false},{"value":10,"label":"10分","order":20,"disabled":false}]},{"key":"rule_panzhihua2006_0001","label":"小结算","control":"checkbox","order":140,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"playRule","label":"玩法","control":"checkbox","order":150,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1,2,3,4],"defaultValue":["all_singles","all_pairs","all_black","all_red"],"options":[{"value":"all_singles","label":"全单","order":10,"disabled":false},{"value":"all_pairs","label":"全对","order":20,"disabled":false},{"value":"all_black","label":"全黑","order":30,"disabled":false},{"value":"all_red","label":"全红","order":40,"disabled":false},{"value":"all_big","label":"全大","order":50,"disabled":false},{"value":"all_small","label":"全小","order":60,"disabled":false},{"value":"triple_five_to_ace_bomb","label":"3个5-A算炸弹","order":70,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":160,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"roomRestriction","label":"其他","control":"checkbox","order":170,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"距离过近提示","order":20,"disabled":false},{"value":"distance_warning","label":"距离过近提示","order":30,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":40,"disabled":false},{"value":"chat_muted","label":"禁言","order":50,"disabled":false}]}]' AS JSON)),
  (2002,'neijiang2002','neijiang2002-v1.0.0','CN-51-10','b837699d3db2a723adb08a58724be2974c0a11494a7a057b8c599380f0a234c7',CAST('[{"key":"playerCount","label":"人数","control":"radio","order":70,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":2,"options":[{"value":2,"label":"2人","order":10,"disabled":false},{"value":3,"label":"3人","order":20,"disabled":false}]},{"key":"roundCount","label":"局数","control":"radio","order":80,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":8,"options":[{"value":8,"label":"8局","order":10,"disabled":false},{"value":12,"label":"12局","order":20,"disabled":false},{"value":16,"label":"16局","order":30,"disabled":false}]},{"key":"operationTime","label":"操作时间","control":"radio","order":90,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":1000,"options":[{"value":1000,"label":"1000秒","order":10,"disabled":false},{"value":15,"label":"15秒","order":20,"disabled":false},{"value":20,"label":"20秒","order":30,"disabled":false}]},{"key":"rule_neijiang2002_0001","label":"先出牌","control":"radio","order":100,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"winner_first","options":[{"value":"winner_first","label":"赢家先出","order":10,"disabled":false},{"value":"spade_three_first","label":"黑桃3优先","order":20,"disabled":false}]},{"key":"rule_neijiang2002_0002","label":"三带二","control":"radio","order":110,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":"two_cards_no_compare","options":[{"value":"two_cards_no_compare","label":"随便带","order":10,"disabled":false},{"value":"two_cards_compare","label":"带一样","order":20,"disabled":false}]},{"key":"rule_neijiang2002_0003","label":"炸弹记分","control":"radio","order":120,"visible":true,"disabled":false,"required":true,"defaultCandidateIndexes":[1],"defaultValue":5,"options":[{"value":5,"label":"5分","order":10,"disabled":false},{"value":10,"label":"10分","order":20,"disabled":false},{"value":20,"label":"20分","order":30,"disabled":false},{"value":30,"label":"30分","order":40,"disabled":false}]},{"key":"rule_neijiang2002_0004","label":"小结算","control":"checkbox","order":130,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[1],"defaultValue":["option_0001"],"options":[{"value":"option_0001","label":"小局结算弹窗","order":10,"disabled":false}]},{"key":"rule_neijiang2002_0005","label":"玩法","control":"checkbox","order":140,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[2,3,4,5],"defaultValue":["four_with_two","triple_ace_bomb","require_spade_three","four_threes_direct_win"],"options":[{"value":"three_no_attachment","label":"三不带","order":10,"disabled":false},{"value":"four_with_two","label":"四带二","order":20,"disabled":false},{"value":"triple_ace_bomb","label":"3A算炸弹","order":30,"disabled":false},{"value":"require_spade_three","label":"必须出黑桃3","order":40,"disabled":false},{"value":"four_threes_direct_win","label":"四个3关门","order":50,"disabled":false}]},{"key":"hostingMissThreshold","label":"托管次数","control":"radio","order":150,"visible":false,"disabled":true,"required":true,"defaultCandidateIndexes":[1],"defaultValue":3,"options":[{"value":3,"label":"3次","order":10,"disabled":false},{"value":4,"label":"4次","order":20,"disabled":false},{"value":5,"label":"5次","order":30,"disabled":false}]},{"key":"rule_neijiang2002_0006","label":"其他","control":"checkbox","order":160,"visible":true,"disabled":false,"required":false,"defaultCandidateIndexes":[],"defaultValue":[],"options":[{"value":"ip_limit","label":"IP限制","order":10,"disabled":false},{"value":"gps_limit","label":"GPS限制","order":20,"disabled":false},{"value":"timeout_auto_play","label":"超时托管","order":30,"disabled":false},{"value":"distance_warning","label":"距离过近警告","order":40,"disabled":false},{"value":"interaction_forbidden","label":"禁止互动","order":50,"disabled":false},{"value":"chat_muted","label":"禁言","order":60,"disabled":false}]}]' AS JSON));

CREATE TEMPORARY TABLE pdk_publication_validator AS
SELECT p.game_id,
       JSON_ARRAY_APPEND(p.rule_fields,'$',JSON_OBJECT(
         'key','payerMode','label','支付方式','control','radio','order',10000,
         'visible',false,'disabled',true,'required',true,
         'defaultCandidateIndexes',JSON_ARRAY(1),'defaultValue','OWNER',
         'options',JSON_ARRAY(JSON_OBJECT('value','OWNER','label','房主付','order',10,'disabled',false)))) AS fields
FROM pdk_publication_repair p;

CREATE TEMPORARY TABLE pdk_publication_active_release (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  release_scope VARCHAR(16) NOT NULL,
  catalog_snapshot JSON NOT NULL,
  rule_snapshot JSON NOT NULL,
  ui_snapshot JSON NOT NULL,
  component_snapshot JSON NOT NULL,
  rollout_percent DECIMAL(5,2) NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  region_rollout_percent DECIMAL(5,2) NOT NULL
);
INSERT INTO pdk_publication_active_release
SELECT r.game_id,r.release_scope,r.catalog_snapshot,r.rule_snapshot,r.ui_snapshot,
       r.component_snapshot,r.rollout_percent,r.created_by,rr.rollout_percent
FROM aoo_game_release r
JOIN pdk_publication_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
JOIN aoo_game_release_region rr ON rr.release_id=r.release_id
  AND rr.region_code=p.region_code AND rr.status='ACTIVE'
WHERE r.status='ACTIVE';

CREATE TEMPORARY TABLE pdk_publication_active_index (
  game_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  component_chain JSON NOT NULL,
  rule_validator JSON NOT NULL,
  ui_schema JSON NOT NULL
);
INSERT INTO pdk_publication_active_index
SELECT i.game_id,i.component_chain,i.rule_validator,i.ui_schema
FROM aoo_compiled_room_create_index i
JOIN pdk_publication_repair p ON p.game_id=i.game_id AND p.play_version=i.play_version
  AND p.region_code=i.region_code
JOIN aoo_compiled_index_active a ON a.game_id=i.game_id AND a.region_code=i.region_code
  AND a.play_version=i.play_version AND a.index_generation=i.index_generation
WHERE i.lifecycle_state='ACTIVE';

CREATE TEMPORARY TABLE pdk_publication_assertion(
  assertion TINYINT NOT NULL CHECK (assertion=1)
);
INSERT INTO pdk_publication_assertion(assertion)
SELECT IF(COUNT(*)=5,1,0) FROM pdk_publication_active_release;
INSERT INTO pdk_publication_assertion(assertion)
SELECT IF(COUNT(*)=5,1,0) FROM pdk_publication_active_index;

INSERT INTO aoo_game_release(
  release_id,game_id,play_version,release_version,release_scope,
  catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,
  catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,
  status,rollout_percent,created_by,reason,validated_at,activated_at)
SELECT p.game_id*1000000000+2026100607,p.game_id,p.play_version,2026100607,
       r.release_scope,r.catalog_snapshot,
       JSON_SET(COALESCE(r.rule_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSchema',JSON_SET(JSON_OBJECT(
           'gameId',p.game_id,'gameCode',p.game_code,'playVersion',p.play_version),
           '$.fields',v.fields)),
       JSON_SET(COALESCE(r.ui_snapshot,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       r.component_snapshot,
       SHA2(CONCAT(p.game_code,'|catalog|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|rules|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|ui|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|components|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash,'|2026100607'),256),
       'ACTIVE',r.rollout_percent,r.created_by,
       CONCAT('Repair PDK publication fields and payer mode ',p.game_code),
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_publication_repair p
JOIN pdk_publication_validator v ON v.game_id=p.game_id
JOIN pdk_publication_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_scope=VALUES(release_scope),catalog_snapshot=VALUES(catalog_snapshot),
  rule_snapshot=VALUES(rule_snapshot),ui_snapshot=VALUES(ui_snapshot),
  component_snapshot=VALUES(component_snapshot),catalog_hash=VALUES(catalog_hash),
  rule_hash=VALUES(rule_hash),ui_hash=VALUES(ui_hash),component_hash=VALUES(component_hash),
  bundle_hash=VALUES(bundle_hash),status='ACTIVE',rollout_percent=VALUES(rollout_percent),
  reason=VALUES(reason),validated_at=VALUES(validated_at),activated_at=VALUES(activated_at),
  retired_at=NULL;

INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status)
SELECT p.game_id*1000000000+2026100607,p.region_code,r.region_rollout_percent,'ACTIVE'
FROM pdk_publication_repair p
JOIN pdk_publication_active_release r ON r.game_id=p.game_id
ON DUPLICATE KEY UPDATE rollout_percent=VALUES(rollout_percent),status='ACTIVE';

INSERT INTO aoo_compiled_room_create_index(
  game_id,region_code,play_version,index_generation,release_id,
  component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,
  lifecycle_state,compiled_at,validated_at,activated_at)
SELECT p.game_id,p.region_code,p.play_version,2026100607,
       p.game_id*1000000000+2026100607,
       i.component_chain,
       JSON_SET(COALESCE(i.rule_validator,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.source',CONCAT(p.game_code,'-room-rule-workbook'),
         '$.sourceHash',p.source_hash,'$.fields',v.fields),
       JSON_SET(COALESCE(i.ui_schema,JSON_OBJECT()),
         '$.gameId',p.game_id,'$.gameCode',p.game_code,'$.playVersion',p.play_version,
         '$.roomRuleSourceHash',p.source_hash,'$.roomRuleViewHash',p.source_hash,
         '$.fields',p.rule_fields),
       SHA2(CONCAT(p.game_code,'|',p.region_code,'|',p.play_version,'|',p.source_hash),256),
       SHA2(CONCAT(p.game_code,'|bundle|',p.play_version,'|',p.source_hash,'|2026100607'),256),
       'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)
FROM pdk_publication_repair p
JOIN pdk_publication_validator v ON v.game_id=p.game_id
JOIN pdk_publication_active_index i ON i.game_id=p.game_id
ON DUPLICATE KEY UPDATE
  release_id=VALUES(release_id),component_chain=VALUES(component_chain),
  rule_validator=VALUES(rule_validator),ui_schema=VALUES(ui_schema),
  lookup_hash=VALUES(lookup_hash),bundle_hash=VALUES(bundle_hash),
  lifecycle_state='ACTIVE',validated_at=VALUES(validated_at),
  activated_at=VALUES(activated_at),retired_at=NULL;

INSERT INTO aoo_compiled_index_active(
  game_id,region_code,play_version,index_generation,release_id,cache_epoch,
  activated_by,activation_reason,activated_at)
SELECT p.game_id,p.region_code,p.play_version,2026100607,
       p.game_id*1000000000+2026100607,
       CASE WHEN a.index_generation=2026100607 THEN a.cache_epoch ELSE a.cache_epoch+1 END,
       a.activated_by,CONCAT('Repair PDK publication fields and payer mode ',p.game_code),
       CURRENT_TIMESTAMP(3)
FROM pdk_publication_repair p
JOIN aoo_compiled_index_active a ON a.game_id=p.game_id AND a.region_code=p.region_code
ON DUPLICATE KEY UPDATE
  play_version=VALUES(play_version),index_generation=VALUES(index_generation),
  release_id=VALUES(release_id),
  cache_epoch=GREATEST(aoo_compiled_index_active.cache_epoch,VALUES(cache_epoch)),
  activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),
  activated_at=VALUES(activated_at);

UPDATE aoo_compiled_room_create_index i
JOIN pdk_publication_repair p ON p.game_id=i.game_id AND p.play_version=i.play_version
  AND p.region_code=i.region_code
SET i.lifecycle_state='RETIRED',i.retired_at=COALESCE(i.retired_at,CURRENT_TIMESTAMP(3))
WHERE i.lifecycle_state='ACTIVE' AND i.index_generation<>2026100607;

UPDATE aoo_game_release r
JOIN pdk_publication_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET r.status='RETIRED',r.retired_at=COALESCE(r.retired_at,CURRENT_TIMESTAMP(3))
WHERE r.status='ACTIVE' AND r.release_id<>p.game_id*1000000000+2026100607;

UPDATE aoo_game_release_region rr
JOIN aoo_game_release r ON r.release_id=rr.release_id
JOIN pdk_publication_repair p ON p.game_id=r.game_id AND p.play_version=r.play_version
SET rr.status='RETIRED'
WHERE rr.status='ACTIVE' AND r.release_id<>p.game_id*1000000000+2026100607;

DROP TEMPORARY TABLE pdk_publication_active_index;
DROP TEMPORARY TABLE pdk_publication_active_release;
DROP TEMPORARY TABLE pdk_publication_assertion;
DROP TEMPORARY TABLE pdk_publication_validator;
DROP TEMPORARY TABLE pdk_publication_repair;
COMMIT;
