package com.aoo.bcg.mahjong;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** CD101 W1 static fan/settlement-branch archive; production wiring is deferred to W2. */
public final class CDXZFanSchema {
    private CDXZFanSchema() {}

    public static final String PING_HU = "PING_HU";
    public static final String QING_YI_SE = "QING_YI_SE";
    public static final String LONG_QI_DUI = "LONG_QI_DUI";
    public static final String DUI_DUI_HU = "DUI_DUI_HU";
    public static final String GANG_HAS_FAN = "GANG_HAS_FAN";
    public static final String MING_GUI = "MING_GUI";
    public static final String AN_GUI = "AN_GUI";
    public static final String YAO_JIU = "YAO_JIU";
    public static final String ZHONG_ZHANG = "ZHONG_ZHANG";
    public static final String JIANG_DUI = "JIANG_DUI";
    public static final String JIN_GOU_DIAO = "JIN_GOU_DIAO";
    public static final String BIAN_KA_DIAO = "BIAN_KA_DIAO";
    public static final String MEN_QING = "MEN_QING";
    public static final String CHANG_GE = "CHANG_GE";
    public static final String TIAO_WU = "TIAO_WU";
    public static final String BAO_JIAO = "BAO_JIAO";
    public static final String YI_TIAO_LONG = "YI_TIAO_LONG";
    public static final String BAN_BAN_GAO = "BAN_BAN_GAO";
    public static final String DU_ZHANG = "DU_ZHANG";
    public static final String QIA_XIN_WU = "QIA_XIN_WU";
    public static final String QIA_ER_TIAO = "QIA_ER_TIAO";
    public static final String XUAN_QUE = "XUAN_QUE";
    public static final String JIN_GOU_PAO = "JIN_GOU_PAO";
    public static final String BAO_JIAO_DU_ZI_MO = "BAO_JIAO_DU_ZI_MO";
    public static final String ZI_MO_ZI_QIANG_GANG = "ZI_MO_ZI_QIANG_GANG";
    public static final String SHAIZI_BAOZI = "SHAIZI_BAOZI";
    public static final String GANG_SHANG_PAO = "GANG_SHANG_PAO";
    public static final String DA_QUAN = "DA_QUAN";
    public static final String DIAN_GANG = "DIAN_GANG";
    public static final String MING_BU_GANG = "MING_BU_GANG";
    public static final String AN_GANG = "AN_GANG";
    public static final String LIAN_GANG = "LIAN_GANG";
    public static final String TUI_YU = "TUI_YU";
    public static final String HU_JIAO_ZHUAN_YI = "HU_JIAO_ZHUAN_YI";
    public static final String YI_PAO_DUO_XIANG_TUI_YU = "YI_PAO_DUO_XIANG_TUI_YU";
    public static final String CHA_JIAO_TUI_SHUI = "CHA_JIAO_TUI_SHUI";
    public static final String QING_DUI = "QING_DUI";
    public static final String QING_YI_SE_QI_DUI = "QING_YI_SE_QI_DUI";
    public static final String QING_YI_SE_LONG_QI_DUI = "QING_YI_SE_LONG_QI_DUI";
    public static final String JIANG_YI_SE_DUI_DUI_HU = "JIANG_YI_SE_DUI_DUI_HU";
    public static final String QING_YAO_JIU = "QING_YAO_JIU";

    /**
     * Returns the 41 rows marked "无（未接线）" in the authoritative mapping table.
     * Configurable XQP values use the first matching chessRule_52.xml sample as the offline default constant.
     */
    public static List<MahjongRegionalRules.FanRule> schema() {
        return List.of(
                // #映射行1 | A:XQP MahjongOver.java:804-805,1178 | B:legacy 208-210,243-246 | 等价
                rule(PING_HU, 0),
                // #映射行2 | A:XQP MahjongOver.java:810-825 | B:legacy 222-227 | 等价；默认常量2，可配置改写
                rule(QING_YI_SE, 2, QING_YAO_JIU),
                // #映射行4 | A:XQP MahjongOver.java:841-851,1209-1220 | B:legacy 233-237 | 差异；互斥待W2核实
                rule(LONG_QI_DUI, 3, YAO_JIU),
                // #映射行5 | A:XQP MahjongOver.java:873-885 | B:legacy 211-217 | 差异；默认常量1，可配置改写
                rule(DUI_DUI_HU, 1, JIANG_DUI),
                // #映射行6 | A:XQP MahjongOver.java:887-898 | B:legacy 208-247 | 仅XQP；每杠基础增量1
                rule(GANG_HAS_FAN, 1),
                // #映射行7 | A:XQP MahjongOver.java:900-909 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(MING_GUI, 1),
                // #映射行8 | A:XQP MahjongOver.java:911-920 | B:legacy 208-247 | 仅XQP；默认常量2，可配置改写
                rule(AN_GUI, 2),
                // #映射行9 | A:XQP MahjongOver.java:956-981 | B:legacy 225-227,233-237 | 差异（待裁决）；默认常量3，可配置改写
                rule(YAO_JIU, 3, LONG_QI_DUI, QING_YAO_JIU),
                // #映射行10 | A:XQP MahjongOver.java:1005-1014 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(ZHONG_ZHANG, 1),
                // #映射行11 | A:XQP MahjongOver.java:983-1003 | B:legacy 229-232 | 差异；默认常量3，可配置改写
                rule(JIANG_DUI, 3, DUI_DUI_HU),
                // #映射行12 | A:XQP MahjongOver.java:933-942 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(JIN_GOU_DIAO, 1),
                // #映射行13 | A:XQP MahjongOver.java:854-861,922-931 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(BIAN_KA_DIAO, 1),
                // #映射行14 | A:XQP MahjongOver.java:862-869,944-953 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(MEN_QING, 1),
                // #映射行15 | A:XQP MahjongOver.java:1017-1025 | B:legacy 208-247 | 仅XQP；默认常量1（SING[1]），可配置改写
                rule(CHANG_GE, 1),
                // #映射行16 | A:XQP MahjongOver.java:1027-1034 | B:legacy 208-247 | 仅XQP；默认常量2（DANCE[1]），可配置改写
                rule(TIAO_WU, 2),
                // #映射行17 | A:XQP MahjongOver.java:1035-1043 | B:legacy 225-227 | 差异；默认常量1，可配置改写
                rule(BAO_JIAO, 1),
                // #映射行18 | A:XQP MahjongOver.java:1057-1063 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(YI_TIAO_LONG, 1),
                // #映射行19 | A:XQP MahjongOver.java:1064-1070 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(BAN_BAN_GAO, 1),
                // #映射行20 | A:XQP MahjongOver.java:1071-1077 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(DU_ZHANG, 1),
                // #映射行21 | A:XQP MahjongOver.java:1082-1090 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(QIA_XIN_WU, 1),
                // #映射行22 | A:XQP MahjongOver.java:1092-1100 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(QIA_ER_TIAO, 1),
                // #映射行23 | A:XQP MahjongOver.java:1120-1152 | B:legacy 208-247 | 仅XQP
                rule(XUAN_QUE, 1),
                // #映射行28 | A:XQP MahjongRoom.java:1962-1967 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(JIN_GOU_PAO, 1),
                // #映射行29 | A:XQP MahjongRoom.java:1969-1973 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(BAO_JIAO_DU_ZI_MO, 1),
                // #映射行30 | A:XQP MahjongRoom.java:1975-1987 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(ZI_MO_ZI_QIANG_GANG, 1),
                // #映射行31 | A:XQP MahjongRoom.java:1991-1995 | B:legacy 208-247 | 仅XQP；默认常量1，可配置改写
                rule(SHAIZI_BAOZI, 1),
                // #映射行33 | A:XQP MahjongRoom.java:1997-2001,2030-2033 | B:legacy 218-220 | 等价
                rule(GANG_SHANG_PAO, 1),
                // #映射行35 | A:XQP MahjongOver.java:1155-1163 | B:legacy 208-247 | 仅XQP；默认常量0，可配置返回动态圈分
                rule(DA_QUAN, 0),
                // #映射行40 | A:XQP MahjongRoomHandler.java:2533-2536,2580-2596 | B:legacy 163-169 | 等价；支付倍数2
                rule(DIAN_GANG, 2),
                // #映射行41 | A:XQP MahjongRoomHandler.java:2598-2601 | B:legacy 161-163 | 等价；支付倍数1
                rule(MING_BU_GANG, 1),
                // #映射行42 | A:XQP MahjongRoomHandler.java:2602-2605 | B:legacy 159-161 | 等价；支付倍数2
                rule(AN_GANG, 2),
                // #映射行43 | A:XQP MahjongRoomHandler.java:2533-2536; MahjongRoom.java:2002-2006 | B:legacy 156-173 | 仅XQP；每次连杠增量1
                rule(LIAN_GANG, 1),
                // #映射行44 | A:XQP MahjongRoom.java:2206-2219 | B:legacy 64-82 | 差异；状态分支0番，互斥待W2核实
                rule(TUI_YU, 0),
                // #映射行45 | A:XQP MahjongRoomHandler.java:1744-1749,1768-1772 | B:legacy 64-87,156-173 | 仅XQP；状态分支0番
                rule(HU_JIAO_ZHUAN_YI, 0),
                // #映射行46 | A:XQP MahjongRoomHandler.java:1750-1767 | B:legacy 64-87,156-173 | 仅XQP；状态分支0番
                rule(YI_PAO_DUO_XIANG_TUI_YU, 0),
                // #映射行49 | A:XQP MahjongOver.java:771-1178 | B:legacy 64-82 | 仅legacy；XQP无对应番，0仅作归档
                rule(CHA_JIAO_TUI_SHUI, 0),
                // #映射行50 | A:XQP MahjongOver.java:810-825,873-885 | B:legacy 229-232 | 等价；默认组合常量3，互斥待W2核实
                rule(QING_DUI, 3),
                // #映射行51 | A:XQP MahjongOver.java:810-825,831-844 | B:legacy 233-237 | 等价；默认组合常量4，互斥待W2核实
                rule(QING_YI_SE_QI_DUI, 4),
                // #映射行52 | A:XQP MahjongOver.java:810-825,831-851,1209-1220 | B:legacy 238-241 | 等价；默认组合常量5，互斥待W2核实
                rule(QING_YI_SE_LONG_QI_DUI, 5),
                // #映射行53 | A:XQP MahjongOver.java:983-1003 | B:legacy 229-232 | 仅legacy；以XQP将对默认常量3归档，互斥待W2核实
                rule(JIANG_YI_SE_DUI_DUI_HU, 3),
                // #映射行54 | A:XQP MahjongOver.java:956-981 | B:legacy 233-237 | 差异；XQP ON[0]默认常量3
                rule(QING_YAO_JIU, 3, QING_YI_SE, YAO_JIU));
    }

    /**
     * Maps the CEO-authoritative room value to a point cap.
     * 封顶为0必须按0不得解释成不封顶
     *
     * <p>This intentionally differs from XQP MahjongOver.java:1188-1190 (0→30区间) and legacy
     * FengDing 0→2番. The conflict is registered upstream; this implementation follows the CEO wording.
     */
    public static long capFromFengDing(int fengDingValue) {
        return switch (fengDingValue) {
            case 0 -> 0L;
            case 1 -> 50L;
            case 2 -> 100L;
            case 3 -> Long.MAX_VALUE;
            default -> throw new IllegalArgumentException("unsupported fengDing: " + fengDingValue);
        };
    }

    /** Records authority boundaries only; W1 deliberately implements no call-ready/flower-pig amount formula. */
    public static Map<String, String> pendingAuthorityNotes() {
        String note = "依例外条款 e（待用户确认）：查叫/花猪金额语义唯一权威源=Aoo 现行开房规则表+血战产品规则，不在 XQP 权威范围";
        return Map.of("CHA_JIAO_AMOUNT", note, "HUA_ZHU_AMOUNT", note);
    }

    private static MahjongRegionalRules.FanRule rule(String code, int fan, String... excludes) {
        return new MahjongRegionalRules.FanRule(code, fan, Set.of(excludes));
    }
}
