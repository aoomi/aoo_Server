package com.aoo.bcg.mahjong;

// 本单由 Codex 执行器实施、岗位核验=majiang；AOO-0040 t104 门禁豁免实跑（javac 离线+console-standalone 直跑），见卡 t104 开工登记节。

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

/**
 * AOO-0040 W3: the 54 rows below correspond one-for-one with
 * 番型三方映射表-20260928.md:15-68. XQP is the equivalence authority; legacy differences are comments only.
 */
class Chengdu1001FanRegressionMatrixTest {
    private static final List<Integer> PING = List.of(11,12,13,21,22,23,31,32,33,14,15,16,29,29);
    private static final List<Integer> QING = List.of(11,11,11,12,13,14,14,15,16,16,17,18,12,12);
    private static final List<Integer> ZHONG = List.of(12,13,14,13,14,15,14,15,16,16,17,18,18,18);
    private static final List<Integer> DUI_DUI = List.of(11,11,11,12,12,12,23,23,23,34,34,34,25,25);
    private static final List<Integer> QING_DUI = List.of(11,11,11,12,12,12,13,13,13,14,14,14,15,15);
    private static final List<Integer> SEVEN_PAIRS = List.of(11,11,12,12,13,13,14,14,21,21,22,22,23,23);
    private static final List<Integer> QING_SEVEN_PAIRS = List.of(11,11,12,12,13,13,14,14,15,15,16,16,17,17);
    private static final List<Integer> QING_LONG_SEVEN_PAIRS = List.of(11,11,11,11,12,12,13,13,14,14,15,15,16,16);

    private record Row(int number, String name, String category, String source, Executable assertion) {}

    @TestFactory Stream<DynamicTest> mappingTableHasOneRegressionPerRow() {
        return rows().stream().map(row -> DynamicTest.dynamicTest(
                "%02d-%s-%s".formatted(row.number(), row.category(), row.name()), row.assertion()));
    }

    @Test void matrixCategoryCountsAreExact() {
        assertEquals(54, rows().size());
        assertEquals(12, count("等价"));
        assertEquals(12, count("差异"));
        assertEquals(26, count("仅XQP"));
        assertEquals(4, count("仅legacy"));
    }

    @Test void fengDingZeroMeansLiteralZeroForRepresentativeWinningForms() {
        // -1 才是不封顶哨兵（XQP 现行），0 不得解释为不封顶（CEO 原话，终裁 c）。
        assertEquals(0L, Chengdu1001FanSchema.capFromFengDing(0));
        for (List<Integer> hand : List.of(PING, QING, SEVEN_PAIRS, DUI_DUI)) {
            MahjongState state = state(hand, true);
            assertTrue(Chengdu1001FanScoring.settleChengdu1001(state, 0, 7).isEmpty(),
                    "fengDing=0 must produce no amount for hand=" + hand);
        }
    }

    @Test void fengDingMappingIsPinnedToFinalAdjudication() {
        // -1 才是不封顶哨兵（XQP 现行），0 不得解释为不封顶（CEO 原话，终裁 c）。
        assertEquals(0L, Chengdu1001FanSchema.capFromFengDing(0));
        assertEquals(50L, Chengdu1001FanSchema.capFromFengDing(1));
        assertEquals(100L, Chengdu1001FanSchema.capFromFengDing(2));
        assertEquals(Long.MAX_VALUE, Chengdu1001FanSchema.capFromFengDing(3));
        assertThrows(IllegalArgumentException.class, () -> Chengdu1001FanSchema.capFromFengDing(-1));
        assertThrows(IllegalArgumentException.class, () -> Chengdu1001FanSchema.capFromFengDing(4));
        long aooUnlimited = Chengdu1001FanScoring.settleChengdu1001(state(QING,false),3,1).get(0).points();
        assertEquals(16L, aooUnlimited); // Aoo fd=3 与 XQP MR[0]=-1 均是不封顶语义。
    }

    private static List<Row> rows() {
        return List.of(
            eq(1,"平胡","映射表:15",() -> { assertDetected(PING, Chengdu1001FanSchema.PING_HU); assertSchemaHit(Chengdu1001FanSchema.PING_HU,0); }),
            eq(2,"清一色","映射表:16",() -> { assertDetected(QING, Chengdu1001FanSchema.QING_YI_SE); assertSchemaHit(Chengdu1001FanSchema.QING_YI_SE,2); }),
            eq(3,"七对（小七对）","映射表:17",() -> { assertDetected(SEVEN_PAIRS,"QI_DUI"); assertSchemaHit("QI_DUI",2); }),
            diff(4,"龙七对／豪华七对（一根）","映射表:18",() -> assertSchemaHit(Chengdu1001FanSchema.LONG_QI_DUI,3)), // legacy=4；按XQP默认3。
            diff(5,"对对胡／对子胡","映射表:19",() -> { assertDetected(DUI_DUI,Chengdu1001FanSchema.DUI_DUI_HU); assertSchemaHit(Chengdu1001FanSchema.DUI_DUI_HU,1); }), // legacy可切2；按XQP默认1。
            xqp(6,"点/明/暗杠“杠会点”加番","映射表:20",() -> assertSchemaHit(Chengdu1001FanSchema.GANG_HAS_FAN,1)),
            xqp(7,"明鬼","映射表:21",() -> assertSchemaHit(Chengdu1001FanSchema.MING_GUI,1)),
            xqp(8,"暗鬼","映射表:22",() -> assertSchemaHit(Chengdu1001FanSchema.AN_GUI,2)),
            diff(9,"幺九（混/清幺九）","映射表:23",() -> assertSchemaHit(Chengdu1001FanSchema.YAO_JIU,3)), // legacy拆2/4；终裁b按XQP ON[0]=3。
            xqp(10,"中张","映射表:24",() -> { assertDetected(ZHONG,Chengdu1001FanSchema.ZHONG_ZHANG); assertSchemaHit(Chengdu1001FanSchema.ZHONG_ZHANG,1); }),
            diff(11,"将对","映射表:25",() -> assertSchemaHit(Chengdu1001FanSchema.JIANG_DUI,3)), // XQP可配且有互斥回退；当前默认3。
            xqp(12,"金钩钓","映射表:26",() -> assertSchemaHit(Chengdu1001FanSchema.JIN_GOU_DIAO,1)),
            xqp(13,"边卡吊","映射表:27",() -> assertSchemaHit(Chengdu1001FanSchema.BIAN_KA_DIAO,1)),
            xqp(14,"门清","映射表:28",() -> assertSchemaHit(Chengdu1001FanSchema.MEN_QING,1)),
            xqp(15,"唱歌","映射表:29",() -> assertNoDetectorEmission(Chengdu1001FanSchema.CHANG_GE)),
            xqp(16,"跳舞","映射表:30",() -> assertNoDetectorEmission(Chengdu1001FanSchema.TIAO_WU)),
            diff(17,"报叫","映射表:31",() -> assertSchemaHit(Chengdu1001FanSchema.BAO_JIAO,1)), // legacy固定2；按XQP默认1。
            xqp(18,"一条龙","映射表:32",() -> assertSchemaHit(Chengdu1001FanSchema.YI_TIAO_LONG,1)),
            xqp(19,"板板高","映射表:33",() -> assertNoDetectorEmission(Chengdu1001FanSchema.BAN_BAN_GAO)),
            xqp(20,"独张","映射表:34",() -> assertNoDetectorEmission(Chengdu1001FanSchema.DU_ZHANG)),
            xqp(21,"卡心五","映射表:35",() -> assertNoDetectorEmission(Chengdu1001FanSchema.QIA_XIN_WU)),
            xqp(22,"卡二条","映射表:36",() -> assertNoDetectorEmission(Chengdu1001FanSchema.QIA_ER_TIAO)),
            xqp(23,"选缺","映射表:37",() -> assertSchemaHit(Chengdu1001FanSchema.XUAN_QUE,1)),
            diff(24,"天胡","映射表:38",() -> assertAbsentFromSchema("TIAN_HU")), // XQP配置番；新schema未实现，差异保留。
            diff(25,"地胡","映射表:39",() -> assertAbsentFromSchema("DI_HU")), // XQP按模式/报叫变化；新schema未实现。
            xqp(26,"海底捞（自摸）","映射表:40",() -> assertAbsentFromSchema("HAI_DI_LAO")),
            xqp(27,"海底炮（点炮）","映射表:41",() -> assertAbsentFromSchema("HE_DI_LAO")),
            xqp(28,"金钩炮","映射表:42",() -> assertSchemaHit(Chengdu1001FanSchema.JIN_GOU_PAO,1)),
            xqp(29,"报叫独自摸","映射表:43",() -> assertNoDetectorEmission(Chengdu1001FanSchema.BAO_JIAO_DU_ZI_MO)),
            xqp(30,"自摸自抢杠","映射表:44",() -> assertSchemaHit(Chengdu1001FanSchema.ZI_MO_ZI_QIANG_GANG,1)),
            xqp(31,"骰子豹子","映射表:45",() -> assertNoDetectorEmission(Chengdu1001FanSchema.SHAIZI_BAOZI)),
            diff(32,"杠上花","映射表:46",() -> assertAbsentFromSchema("GANG_SHANG_HUA")), // XQP固定+1；新schema未实现。
            eq(33,"杠上炮","映射表:47",() -> assertSchemaHit(Chengdu1001FanSchema.GANG_SHANG_PAO,1)),
            xqp(34,"抢杠胡","映射表:48",() -> assertAbsentFromSchema("QIANG_GANG_HU")),
            xqp(35,"打圈（圈分）","映射表:49",() -> assertSchemaHit(Chengdu1001FanSchema.DA_QUAN,0)),
            eq(36,"自摸支付","映射表:50",Chengdu1001FanRegressionMatrixTest::assertSelfDrawPayment),
            eq(37,"点炮支付","映射表:51",Chengdu1001FanRegressionMatrixTest::assertDiscardPayment),
            diff(38,"番封顶","映射表:52",() -> { assertEquals(0L,Chengdu1001FanSchema.capFromFengDing(0)); assertEquals(Long.MAX_VALUE,Chengdu1001FanSchema.capFromFengDing(3)); }),
            diff(39,"番数→分数公式","映射表:53",Chengdu1001FanRegressionMatrixTest::assertXqpExponentialAmount), // XQP主体=2^fan；当前线性实现偏离即FAIL。
            eq(40,"点杠（刮风/下雨）","映射表:54",() -> assertSchemaHit(Chengdu1001FanSchema.DIAN_GANG,2)),
            eq(41,"明/补杠（刮风/下雨）","映射表:55",() -> assertSchemaHit(Chengdu1001FanSchema.MING_BU_GANG,1)),
            eq(42,"暗杠（刮风/下雨）","映射表:56",() -> assertSchemaHit(Chengdu1001FanSchema.AN_GANG,2)),
            xqp(43,"连杠倍率/加番","映射表:57",() -> assertNoDetectorEmission(Chengdu1001FanSchema.LIAN_GANG)),
            diff(44,"杠上炮/抢杠退雨","映射表:58",() -> assertSchemaHit(Chengdu1001FanSchema.TUI_YU,0)), // XQP最后一次雨时序；legacy退全部杠分。
            xqp(45,"呼叫转移（转雨）","映射表:59",() -> assertNoDetectorEmission(Chengdu1001FanSchema.HU_JIAO_ZHUAN_YI)),
            xqp(46,"一炮多响退全部雨","映射表:60",() -> assertNoDetectorEmission(Chengdu1001FanSchema.YI_PAO_DUO_XIANG_TUI_YU)),
            legacy(47,"查叫","映射表:61",Chengdu1001FanRegressionMatrixTest::assertPendingAuthorityNotes),
            legacy(48,"花猪（查叫无缺一门）","映射表:62",Chengdu1001FanRegressionMatrixTest::assertPendingAuthorityNotes),
            legacy(49,"查叫退税（PeiZi）","映射表:63",Chengdu1001FanRegressionMatrixTest::assertPendingAuthorityNotes),
            eq(50,"清对（清一色+对对胡）","映射表:64",() -> { assertDetected(QING_DUI,Chengdu1001FanSchema.QING_DUI); assertSchemaHit(Chengdu1001FanSchema.QING_DUI,3); }),
            eq(51,"清一色七对","映射表:65",() -> { assertDetected(QING_SEVEN_PAIRS,Chengdu1001FanSchema.QING_YI_SE_QI_DUI); assertSchemaHit(Chengdu1001FanSchema.QING_YI_SE_QI_DUI,4); }),
            eq(52,"清一色豪华七对","映射表:66",() -> { assertDetected(QING_LONG_SEVEN_PAIRS,Chengdu1001FanSchema.QING_YI_SE_LONG_QI_DUI); assertSchemaHit(Chengdu1001FanSchema.QING_YI_SE_LONG_QI_DUI,5); }),
            legacy(53,"将一色对对胡","映射表:67",Chengdu1001FanRegressionMatrixTest::assertPendingAuthorityNotes),
            diff(54,"清幺九","映射表:68",() -> assertSchemaHit(Chengdu1001FanSchema.QING_YAO_JIU,3)) // legacy=4；按XQP单一ON[0]=3。
        );
    }

    private static Row eq(int n,String name,String source,Executable assertion){return new Row(n,name,"等价",source,assertion);}
    private static Row diff(int n,String name,String source,Executable assertion){return new Row(n,name,"差异",source,assertion);}
    private static Row xqp(int n,String name,String source,Executable assertion){return new Row(n,name,"仅XQP",source,assertion);}
    private static Row legacy(int n,String name,String source,Executable assertion){return new Row(n,name,"仅legacy",source,assertion);}
    private static long count(String category){return rows().stream().filter(row -> row.category().equals(category)).count();}

    private static void assertSchemaHit(String code,int expectedFan) {
        Map<String,MahjongRegionalRules.FanRule> schema = MahjongRegionalRules.compileFanSchema(Chengdu1001FanSchema.schema());
        assertTrue(schema.containsKey(code), "schema missing authoritative code " + code);
        var scored = MahjongRegionalRules.scoreFans(Chengdu1001FanSchema.schema(),Set.of(code),Integer.MAX_VALUE);
        assertEquals(expectedFan,scored.rawFan());
        assertTrue(scored.applied().contains(code));
    }

    private static void assertDetected(List<Integer> hand,String code) {
        var hits = Chengdu1001FanHitDetector.detect(state(hand,true),0,new Chengdu1001FanHitDetector.WinContext(true,false,false,false,false,true,false));
        assertTrue(hits.contains(code), "detector missing authoritative hit " + code);
    }

    private static void assertNoDetectorEmission(String code) {
        assertTrue(MahjongRegionalRules.compileFanSchema(Chengdu1001FanSchema.schema()).containsKey(code));
        var hits = Chengdu1001FanHitDetector.detect(state(PING,true),0,new Chengdu1001FanHitDetector.WinContext(true,false,false,false,false,false,false));
        assertFalse(hits.contains(code), "unimplemented/history-only row must not be fabricated");
    }

    private static void assertAbsentFromSchema(String code) {
        assertFalse(MahjongRegionalRules.compileFanSchema(Chengdu1001FanSchema.schema()).containsKey(code));
        assertFalse(Chengdu1001FanHitDetector.detect(state(PING,true),0,
                new Chengdu1001FanHitDetector.WinContext(true,true,true,true,true,true,true)).contains(code));
    }

    private static void assertSelfDrawPayment() {
        var lines = Chengdu1001FanScoring.settleChengdu1001(state(QING,true),3,1);
        assertEquals(Set.of(1,2,3),lines.stream().map(MahjongRegionalRules.SettlementLine::payer).collect(java.util.stream.Collectors.toSet()));
        assertTrue(lines.stream().allMatch(line -> line.receiver()==0));
    }

    private static void assertDiscardPayment() {
        var lines = Chengdu1001FanScoring.settleChengdu1001(state(QING,false),3,1);
        assertEquals(1,lines.size());
        assertEquals(3,lines.get(0).payer());
        assertEquals(0,lines.get(0).receiver());
    }

    private static void assertXqpExponentialAmount() {
        // QING produces 4 applied fan in the W2 fixture; XQP authority is 2^fan * base = 16.
        var lines = Chengdu1001FanScoring.settleChengdu1001(state(QING,false),3,1);
        assertEquals(16L,lines.get(0).points(),"XQP exponential amount; linear fan*base must fail");
    }

    private static void assertPendingAuthorityNotes() {
        // 条款 e 仍待用户确认：查叫/花猪金额只登记，不写强金额断言；其余仅legacy同样不复判。
        Map<String,String> notes = Chengdu1001FanSchema.pendingAuthorityNotes();
        assertTrue(notes.get("CHA_JIAO_AMOUNT").contains("待用户确认"));
        assertTrue(notes.get("HUA_ZHU_AMOUNT").contains("待用户确认"));
    }

    private static MahjongState state(List<Integer> winnerHand,boolean selfDraw) {
        return new MahjongState(List.of(),Map.of(0,winnerHand,1,PING,2,PING,3,PING),selfDraw?0:1,selfDraw,
                selfDraw?-1:19,selfDraw?-1:3,MahjongOperationWindow.empty(),true,0,Map.of(),Set.of(0),0,
                MahjongRegionalRules.OpeningSelections.empty());
    }
}
