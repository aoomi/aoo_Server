package com.aoo.bcg.mahjong;

// 本单由 Codex 执行器实施、岗位核验=majiang；Java 实跑登记待环境条件，禁止 mvn/javac，本测试为离线用例对照

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CDXZFanHitDetectionScoringTest {
    private static final List<Integer> PING = List.of(11,12,13,21,22,23,31,32,33,14,15,16,29,29);
    private static final List<Integer> QING = List.of(11,12,13,11,12,13,14,15,16,14,15,16,17,17);
    private static final List<Integer> QING_DUI = List.of(11,11,11,12,12,12,13,13,13,14,14,14,15,15);
    private static final List<Integer> SEVEN_PAIRS = List.of(11,11,12,12,13,13,14,14,21,21,22,22,23,23);

    @Test void pingHuSelfDrawHasThreePayersAndConserves() {
        MahjongState state = state(Map.of(0,PING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var hits = CDXZFanHitDetector.detect(state,0,new CDXZFanHitDetector.WinContext(true,false,false,false,false,true,false));
        assertTrue(hits.contains(CDXZFanSchema.PING_HU));
        // 手算：平胡0 + 门清1 = 1番；baseUnit=2，所以三家各付2，赢家收6，合计0。
        var lines = CDXZFanScoring.settleCd101(state,3,2);
        assertEquals(3,lines.size());
        assertTrue(lines.stream().allMatch(line -> line.receiver()==0 && line.points()==2));
        assertEquals(Map.of(0,6L,1,-2L,2,-2L,3,-2L),settle(state,lines).totals());
    }

    @Test void qingYiSeDiscardIsPaidOnlyByDiscarder() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        var hits = CDXZFanHitDetector.detect(state,0,new CDXZFanHitDetector.WinContext(false,false,false,false,false,true,false));
        assertTrue(hits.contains(CDXZFanSchema.QING_YI_SE));
        // 手算：清一色2 + 门清1 + 选缺1 = 4番；baseUnit=3，点炮者3独付12。
        var lines = CDXZFanScoring.settleCd101(state,3,3);
        assertEquals(List.of(new MahjongRegionalRules.SettlementLine(3,0,12,"WIN","CDXZ_FAN_SCORING")),lines);
        assertEquals(Map.of(0,12L,1,0L,2,0L,3,-12L),settle(state,lines).totals());
    }

    @Test void qingDuiCompositePreventsComponentDoubleCounting() {
        MahjongState state = state(Map.of(0,QING_DUI,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        var matched = CDXZFanHitDetector.detect(state,0,new CDXZFanHitDetector.WinContext(false,false,false,false,false,true,false));
        assertTrue(matched.contains(CDXZFanSchema.QING_DUI));
        assertFalse(matched.contains(CDXZFanSchema.QING_YI_SE));
        assertFalse(matched.contains(CDXZFanSchema.DUI_DUI_HU));
        // 手算：复合清对3 + 门清1 + 选缺1 = 5；组成番不重复进入 scoreFans。
        var scored = MahjongRegionalRules.scoreFans(CDXZFanSchema.schema(),matched,Integer.MAX_VALUE);
        assertEquals(5,scored.cappedFan());
    }

    @Test void sevenPairsRequiresConcealedContext() {
        MahjongState state = state(Map.of(0,SEVEN_PAIRS,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var concealed = CDXZFanHitDetector.detect(state,0,new CDXZFanHitDetector.WinContext(true,false,false,false,false,true,false));
        var exposed = CDXZFanHitDetector.detect(state,0,new CDXZFanHitDetector.WinContext(true,false,false,false,false,false,false));
        assertTrue(concealed.contains(CDXZFanSchema.LONG_QI_DUI));
        assertFalse(exposed.contains(CDXZFanSchema.LONG_QI_DUI));
        // 手算：龙七对3 + 门清1 + 选缺1 = 5番。
        assertEquals(5,MahjongRegionalRules.scoreFans(CDXZFanSchema.schema(),concealed,Integer.MAX_VALUE).cappedFan());
    }

    @Test void fengDingZeroMeansLiteralZeroAndProducesNoAmount() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        // 终裁 c 逐字口径：封顶为0必须按0，不得解释成不封顶。4番封为0，amount=0*5=0。
        assertEquals(0L,CDXZFanSchema.capFromFengDing(0));
        assertTrue(CDXZFanScoring.settleCd101(state,0,5).isEmpty());
        assertEquals(Map.of(0,0L,1,0L,2,0L,3,0L),settle(state,List.of()).totals());
    }

    @Test void fengDingOneTwoThreeMapToFiftyHundredAndUnlimited() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        assertEquals(50L,CDXZFanSchema.capFromFengDing(1));
        assertEquals(100L,CDXZFanSchema.capFromFengDing(2));
        assertEquals(Long.MAX_VALUE,CDXZFanSchema.capFromFengDing(3));
        // 当前抽象牌型手算均为4番，小于50/100/无限三档，所以三档各为4*1=4。
        assertEquals(4,CDXZFanScoring.settleCd101(state,1,1).get(0).points());
        assertEquals(4,CDXZFanScoring.settleCd101(state,2,1).get(0).points());
        assertEquals(4,CDXZFanScoring.settleCd101(state,3,1).get(0).points());
    }

    @Test void twoWinnersAreScoredIndependentlyAgainstOneDiscarder() {
        MahjongState state = state(Map.of(0,QING,1,QING,2,PING,3,PING), Set.of(0,1), 2, false, 3);
        // 每位赢家：4番*1=4；点炮者3分别付4，共付8；两响独立且总和0。
        var lines = CDXZFanScoring.settleCd101(state,3,1);
        assertEquals(Set.of(0,1),lines.stream().map(MahjongRegionalRules.SettlementLine::receiver).collect(java.util.stream.Collectors.toSet()));
        assertTrue(lines.stream().allMatch(line -> line.payer()==3 && line.points()==4));
        assertEquals(Map.of(0,4L,1,4L,2,0L,3,-8L),settle(state,lines).totals());
    }

    @Test void settlementLinesRemainRestoreCompatibleAndConserved() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var lines = CDXZFanScoring.settleCd101(state,3,1);
        assertEquals(Set.of(1,2,3),lines.stream().map(MahjongRegionalRules.SettlementLine::payer).collect(java.util.stream.Collectors.toSet()));
        assertTrue(lines.stream().allMatch(line -> line.receiver()==0 && line.category().equals("WIN") && line.source().equals("CDXZ_FAN_SCORING")));
        // restoreSettlementDetail只按同字段重建并比 totals；此处覆盖字段形态并验证Σtotals=0。
        assertEquals(0L,settle(state,lines).totals().values().stream().mapToLong(Long::longValue).sum());
    }

    private static MahjongState state(Map<Integer,List<Integer>> hands, Set<Integer> winners,
            int currentSeat, boolean drawn, int lastDiscardSeat) {
        return new MahjongState(List.of(),hands,currentSeat,drawn,lastDiscardSeat<0?-1:19,lastDiscardSeat,
                MahjongOperationWindow.empty(),true,winners.iterator().next(),Map.of(),winners,0,
                MahjongRegionalRules.OpeningSelections.empty());
    }

    private static MahjongRegionalRules.SettlementDetail settle(MahjongState state,
            List<MahjongRegionalRules.SettlementLine> lines) {
        return MahjongRegionalRules.settle(state.hands().keySet().stream().sorted().toList(),lines);
    }
}
