package com.aoo.bcg.poker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable projection of the 22 published *.keys.json/*.generated.json vocabularies. */
final class LegacyPdkRegionCatalog {
    record Region(String gameCode, LegacyPdkVariant variant,
            Map<String,Object> defaults, Map<String,Set<Object>> allowed) { }

    private static final Set<Object> PLAYERS_23 = values(2, 3);
    private static final Set<Object> PAYMENT = values("owner_pays", "split_pays", "winner_pays");
    private static final Set<Object> DEAL_15_16 = values(15, 16);
    private static final Set<Object> TIMEOUTS = values(-1, 180, 300, 60, 30);
    private static final Set<Object> TIMEOUTS_WITH_15 = values(-1, 180, 300, 60, 30, 15);
    private static final Set<Object> DISSOLVE = values(30, 60, 180, 300, -1);
    private static final Set<Object> ROOM_RESTRICTIONS = values("same_ip_blocked",
            "within_100m_blocked", "items_forbidden", "within_200m_blocked",
            "kick_unready_30s", "voice_forbidden", "chat_forbidden");
    private static final Set<Object> BOMB_SETTLEMENT = values("max_bomb_to_owner",
            "max_bomb_per_round", 0, "count_any_bomb");
    private static final Set<Object> BOMB_SCORE = values("double", 10);
    private static final Set<Object> FOUR = values("four_with_one", "four_with_two",
            "four_with_three");
    private static final Set<Object> ROOMS = values("switch_player_count",
            "dissolve_on_round_trustee", "dissolve_after_two_rounds_trustee");
    private static final Set<Object> STANDARD_PLAY = values("first_round_spade_three_first",
            "every_round_spade_three_first", "may_pass", "show_hand", "bomb_splittable",
            "triple_ace_max_bomb", "heart_ten_bonus", "first_play_spade_three_optional",
            "manual_pass", "manual_ready", "last_card_scoring", "no_bomb",
            "triple_ace_bomb", "anti_cheat");

    private static final Map<String,Region> REGIONS = regions();

    private LegacyPdkRegionCatalog() { }

    static Region require(String gameCode) {
        Region region = REGIONS.get(gameCode);
        if (region == null) throw new IllegalArgumentException("unknown legacy PDK code " + gameCode);
        return region;
    }

    static boolean contains(String gameCode) { return REGIONS.containsKey(gameCode); }

    private static Map<String,Region> regions() {
        Map<String,Region> result = new LinkedHashMap<>();
        add(result, PdkBusinessCodes.WANNIAN, LegacyPdkVariant.A1_MINIMAL,
                defaults(4, 2, 15, List.of("first_round_spade_three_first")),
                core(values(4,8,16), PLAYERS_23, values("first_round_spade_three_first",
                        "every_round_spade_three_first", "not_required_four_with_three"),
                        values(-1,180,300), values("same_ip_blocked","same_location_blocked")));
        add(result, PdkBusinessCodes.HUAIBIN, LegacyPdkVariant.A1_MINIMAL,
                defaults(8, 2, 15, List.of("show_hand")),
                fields(values(8,16), PLAYERS_23, values("show_hand"), null, null,
                        Map.of("firstPlayRule", values("first_round_heart_three_first",
                                "every_round_heart_three_first"),
                                "bombScore", BOMB_SCORE,
                                "roomRestriction", values("same_ip_blocked","same_location_blocked"))));

        addStandard(result, PdkBusinessCodes.LEPING, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,12,16), Set.of(), Map.of());
        addStandard(result, PdkBusinessCodes.JIAN, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("match_score_floor_zero","win_current_score_only"), Map.of());
        addStandard(result, PdkBusinessCodes.NINGDU, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("match_score_floor_zero","win_current_score_only"), Map.of());
        addStandard(result, PdkBusinessCodes.XINXIAN, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("spring","anti_spring",
                        "first_round_heart_three_first","every_round_heart_three_first",
                        "first_play_heart_three_optional"),
                Map.of("playerCount", values(2,3,4)));
        addStandard(result, PdkBusinessCodes.YIXING, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("match_score_floor_zero","win_current_score_only"),
                Map.of("piaoMode", values("no_piao","piao_custom",1,2,5),
                        "piaoRounds", values("first_round_only","every_round")));
        addStandard(result, PdkBusinessCodes.JINTAN, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("match_score_floor_zero","win_current_score_only",
                        "four_with_one_is_bomb"),
                Map.of("bombScore", values("double",10,5), "fourWithMode",
                        values("four_with_one","four_with_two"), "operationTimeoutSeconds",
                        TIMEOUTS_WITH_15, "roomTimeLimitMinutes", values(-1,8,10,12,15,20)));
        addStandard(result, PdkBusinessCodes.GAOAN, LegacyPdkVariant.A2_WITH_FOUR,
                values(8,10,16), values("match_score_floor_zero","win_current_score_only"),
                Map.of("operationTimeoutSeconds", TIMEOUTS_WITH_15,
                        "roomTimeLimitMinutes", values(-1,8,10,12,15,20),
                        "roomSettings", values("switch_player_count","dissolve_on_round_trustee",
                                "dissolve_after_two_rounds_trustee","trustee_three_times_irrevocable")));

        addStandard(result, PdkBusinessCodes.BOZHOU, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(10,20,30), values("first_round_heart_three_first",
                        "every_round_heart_three_first", "first_play_heart_three_optional"),
                Map.of("playMode", values("big_run","small_run")));
        addStandard(result, PdkBusinessCodes.YUGAN, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(6,9,12), Set.of(), Map.of("trusteePenalty", values(5,10,20,50,0)));
        addStandard(result, PdkBusinessCodes.XIAOGAN, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(8,10,16), Set.of(), Map.of("piaoMode", values("no_piao","piao_custom",1,2),
                        "piaoRounds", values("first_round_only","every_round")));
        addStandard(result, PdkBusinessCodes.HUAIHUA, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(8,10,16), values("random_first_play","match_score_floor_zero","win_carried_only"),
                Map.of("cutCardScore", values("no_cut",0.1,0.2,0.5,0.8,1.0),
                        "bigRoundScoring", values("normal","floor_ten","double_below_ten"),
                        "roomSettings", values("switch_player_count","auto_ready",
                                "dissolve_on_round_trustee","dissolve_limit_5","dissolve_limit_3",
                                "dissolve_after_three_rounds_trustee")));
        addStandard(result, PdkBusinessCodes.HENAN, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(8,10,16), values("must_beat_with_big","must_beat_on_run","auto_pass",
                        "allow_triple_with_one","allow_triple_with_two","allow_triple_with_pair",
                        "allow_four_with_one","allow_four_with_two","allow_four_with_three","auto_ready"),
                Map.of("firstPlayMode", values("first_round_heart_three_required",
                                "every_round_heart_three_required","first_round_heart_three_optional",
                                "every_round_heart_three_optional"),
                        "aceThreeBombMode", values("ace_three_max_bomb","ace_three_min_bomb",
                                "ace_three_not_bomb")));
        addStandard(result, PdkBusinessCodes.TAIZHOU, LegacyPdkVariant.A3_WITHOUT_FOUR,
                values(8,10,16), values("every_round_heart_three_first","winner_first","loser_first",
                        "match_score_floor_zero","win_current_score_only","bomb_with_one","small_gate"),
                Map.of("dealCardCount", values(16), "reportScoreMode", values("score_reported","no_score"),
                        "bombSettlementMode", values("max_bomb_per_round",0), "bombScore", values(10,5),
                        "straightLengthMode", values("five_or_more","six_or_more"),
                        "operationTimeoutSeconds", values(-1,180,300,60,30,15,600),
                        "roomTimeLimitMinutes", values(-1,8,10,12,15,20)));

        add(result, PdkBusinessCodes.GUSHI, LegacyPdkVariant.A4_WITHOUT_BOMB_SETTLEMENT,
                defaults(8,2,15,List.of("show_hand")),
                fields(values(8,10,16), PLAYERS_23, values("show_hand","bomb_splittable",
                        "manual_pass","last_card_scoring"), TIMEOUTS, ROOM_RESTRICTIONS,
                        Map.of("firstPlayRule",values("first_round_heart_three_first",
                                "every_round_heart_three_first"),"bombScore",BOMB_SCORE,"roomSettings",ROOMS)));
        add(result, PdkBusinessCodes.XUANCHENG, LegacyPdkVariant.A4_WITHOUT_BOMB_SETTLEMENT,
                defaults(8,2,15,List.of("first_round_heart_three_first")),
                fields(values(8,10,16), PLAYERS_23, values("first_round_heart_three_first","may_pass",
                        "show_hand","bomb_splittable","triple_ace_max_bomb","manual_pass","manual_ready",
                        "last_card_scoring","triple_ace_bomb","anti_cheat","match_score_floor_zero",
                        "win_current_score_only"), TIMEOUTS_WITH_15, ROOM_RESTRICTIONS,
                        Map.of("bombScore",values(5,10,0),"roomSettings",ROOMS,
                                "roomTimeLimitMinutes",values(-1,8,10,12,15,20))));
        add(result, PdkBusinessCodes.FUZHOU, LegacyPdkVariant.A4_WITHOUT_BOMB_SETTLEMENT,
                defaults(8,2,15,List.of("must_beat")),
                fields(values(8,16,24), PLAYERS_23, values("must_beat","winner_first",
                        "must_beat_on_run","auto_pass","show_hand","last_card_scoring","four_with_three"),
                        TIMEOUTS, values("same_ip_blocked","same_location_blocked","items_forbidden"),
                        Map.of("baseScore",values(1,2,5,10),"bombScore",values(0,5,10),
                                "roomSettings",values("switch_player_count"))));

        add(result, PdkBusinessCodes.JINGDEZHEN, LegacyPdkVariant.A5_WITHOUT_BOMB_SCORE,
                defaults(8,2,15,List.of("first_round_spade_three_first")),
                fields(values(8,12,16), PLAYERS_23, values("first_round_spade_three_first",
                        "every_round_spade_three_first","not_required","show_hand","bomb_splittable",
                        "triple_ace_max_bomb","heart_ten_bonus","first_play_spade_three_optional",
                        "manual_pass","auto_ready"), TIMEOUTS, values("same_ip_blocked","within_100m_blocked",
                        "items_forbidden","within_200m_blocked","kick_unready_30s","voice_forbidden"),
                        Map.of("bombSettlementMode",values("count_any_bomb","winner_only",0),
                                "roomSettings",values("switch_player_count"))));
        add(result, PdkBusinessCodes.POYANG, LegacyPdkVariant.A5_WITHOUT_BOMB_SCORE,
                defaults(8,2,15,List.of("first_round_spade_three_first")),
                fields(values(8,16), PLAYERS_23, values("first_round_spade_three_first",
                        "every_round_spade_three_first","not_required","show_hand","bomb_splittable",
                        "triple_ace_max_bomb","heart_ten_bonus","first_play_spade_three_optional",
                        "auto_ready","dissolve_on_round_trustee"), TIMEOUTS, ROOM_RESTRICTIONS,
                        Map.of("bombSettlementMode",values("count_any_bomb","winner_only",0),
                                "roomSettings",values("switch_player_count"))));

        addStandard(result, PdkBusinessCodes.JIANGSU, LegacyPdkVariant.A6_SPECIAL_STRUCTURE,
                values(8,10,16), Set.of(), Map.of("threeWithMode",values("triple_with_one",
                        "triple_with_two","triple_with_pair"),"capMode",values("no_cap","cap_20",
                        "cap_60","cap_100","cap_200"),"roomSettings",values("switch_player_count")));
        add(result, PdkBusinessCodes.HUANGCHUAN, LegacyPdkVariant.A6_SPECIAL_STRUCTURE,
                defaults(8,2,15,List.of("show_hand")),
                fields(values(8,10,16), PLAYERS_23, values("show_hand","bomb_splittable",
                        "manual_pass","last_card_scoring","not_required_beat","keep_three_door_double"),
                        TIMEOUTS, ROOM_RESTRICTIONS, Map.of("firstPlayRule",values(
                                "first_round_heart_three_first","every_round_heart_three_first"),
                                "threeWithMode",values("triple_with_one","triple_with_pair"),
                                "aceThreeBombMode",values("ace_three_max_bomb","ace_three_min_bomb",
                                        "ace_three_not_bomb"),"bombScore",values(10),"roomSettings",
                                values("switch_player_count","dissolve_on_round_trustee","dissolve_limit_5",
                                        "dissolve_after_two_rounds_trustee","dissolve_limit_3"))));
        return Map.copyOf(result);
    }

    private static void addStandard(Map<String,Region> target, String code, LegacyPdkVariant variant,
            Set<Object> rounds, Set<Object> extraPlay, Map<String,Set<Object>> overrides) {
        Set<Object> play = new java.util.LinkedHashSet<>(STANDARD_PLAY);
        play.addAll(extraPlay);
        Map<String,Set<Object>> extra = new LinkedHashMap<>();
        extra.put("bombSettlementMode", BOMB_SETTLEMENT);
        extra.put("bombScore", BOMB_SCORE);
        extra.put("roomSettings", ROOMS);
        if (variant.hasFourWith()) extra.put("fourWithMode", FOUR);
        extra.putAll(overrides);
        int defaultRounds = rounds.stream().mapToInt(value -> ((Number) value).intValue()).min().orElse(8);
        add(target, code, variant, defaults(defaultRounds, 2, 15,
                List.of("first_round_spade_three_first")),
                fields(rounds, PLAYERS_23, Set.copyOf(play), TIMEOUTS, ROOM_RESTRICTIONS, extra));
    }

    private static Map<String,Object> defaults(int rounds, int players, int cards, List<String> play) {
        return Map.of("roundCount", rounds, "playerCount", players, "paymentMode", "owner_pays",
                "dealCardCount", cards, "playRule", play);
    }

    private static Map<String,Set<Object>> core(Set<Object> rounds, Set<Object> players,
            Set<Object> play, Set<Object> timeouts, Set<Object> restrictions) {
        return fields(rounds, players, play, timeouts, restrictions, Map.of());
    }

    private static Map<String,Set<Object>> fields(Set<Object> rounds, Set<Object> players,
            Set<Object> play, Set<Object> timeouts, Set<Object> restrictions,
            Map<String,Set<Object>> extra) {
        Map<String,Set<Object>> fields = new LinkedHashMap<>();
        fields.put("roundCount", rounds); fields.put("playerCount", players);
        fields.put("paymentMode", PAYMENT); fields.put("dealCardCount", DEAL_15_16);
        fields.put("playRule", play);
        if (timeouts != null) fields.put("operationTimeoutSeconds", timeouts);
        fields.put("dissolveTimeoutSeconds", DISSOLVE);
        if (restrictions != null) fields.put("roomRestriction", restrictions);
        fields.putAll(extra);
        return Map.copyOf(fields);
    }

    private static void add(Map<String,Region> target, String code, LegacyPdkVariant variant,
            Map<String,Object> defaults, Map<String,Set<Object>> allowed) {
        Map<String,Object> complete = new LinkedHashMap<>(defaults);
        if (allowed.containsKey("operationTimeoutSeconds")) complete.put("operationTimeoutSeconds", -1);
        if (allowed.containsKey("dissolveTimeoutSeconds")) complete.put("dissolveTimeoutSeconds", 30);
        if (allowed.containsKey("roomRestriction")) complete.put("roomRestriction", List.of("same_ip_blocked"));
        if (allowed.containsKey("roomSettings")) complete.put("roomSettings", List.of("switch_player_count"));
        if (allowed.containsKey("fourWithMode")) complete.put("fourWithMode", List.of("four_with_one"));
        if (allowed.containsKey("bombSettlementMode")) complete.put("bombSettlementMode",
                Set.of(PdkBusinessCodes.JINGDEZHEN, PdkBusinessCodes.POYANG,
                        PdkBusinessCodes.HENAN).contains(code) ? "count_any_bomb"
                        : PdkBusinessCodes.TAIZHOU.equals(code) ? "max_bomb_per_round"
                        : "max_bomb_to_owner");
        if (allowed.containsKey("bombScore")) complete.put("bombScore",
                Set.of(PdkBusinessCodes.XUANCHENG).contains(code) ? 5
                        : Set.of(PdkBusinessCodes.FUZHOU).contains(code) ? 0
                        : Set.of(PdkBusinessCodes.TAIZHOU, PdkBusinessCodes.HUANGCHUAN).contains(code)
                                ? 10 : "double");
        if (allowed.containsKey("firstPlayRule")) complete.put("firstPlayRule",
                "first_round_heart_three_first");
        target.put(code, new Region(code, variant, Map.copyOf(complete), allowed));
    }

    private static Set<Object> values(Object... values) { return Set.of(values); }
}
