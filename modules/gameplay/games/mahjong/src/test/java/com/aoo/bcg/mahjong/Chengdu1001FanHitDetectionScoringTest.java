package com.aoo.bcg.mahjong;

// 本单由 Codex 执行器实施、岗位核验=majiang；AOO-0040 t104 门禁豁免实跑（javac 离线+console-standalone 直跑），见卡 t104 开工登记节。

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Chengdu1001FanHitDetectionScoringTest {
    private static final List<Integer> PING = List.of(11,12,13,21,22,23,31,32,33,14,15,16,29,29);
    private static final List<Integer> QING = List.of(11,11,11,12,13,14,14,15,16,16,17,18,12,12);
    private static final List<Integer> QING_DUI = List.of(11,11,11,12,12,12,13,13,13,14,14,14,15,15);
    private static final List<Integer> SEVEN_PAIRS = List.of(11,11,12,12,13,13,14,14,21,21,22,22,23,23);

    @Test void pingHuSelfDrawHasThreePayersAndConserves() {
        MahjongState state = state(Map.of(0,PING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var hits = Chengdu1001FanHitDetector.detect(state,0,new Chengdu1001FanHitDetector.WinContext(true,false,false,false,false,true,false));
        assertTrue(hits.contains(Chengdu1001FanSchema.PING_HU));
        // 手算：平胡0 + 门清1 = 1番；baseUnit=2，所以三家各付4，赢家收12，合计0。
        var lines = Chengdu1001FanScoring.settleChengdu1001(state,3,2);
        assertEquals(3,lines.size());
        assertTrue(lines.stream().allMatch(line -> line.receiver()==0 && line.points()==4));
        assertEquals(Map.of(0,12L,1,-4L,2,-4L,3,-4L),settle(state,lines).totals());
    }

    @Test void qingYiSeDiscardIsPaidOnlyByDiscarder() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        var hits = Chengdu1001FanHitDetector.detect(state,0,new Chengdu1001FanHitDetector.WinContext(false,false,false,false,false,true,false));
        assertTrue(hits.contains(Chengdu1001FanSchema.QING_YI_SE));
        // 手算：清一色2 + 门清1 + 选缺1 = 4番；baseUnit=3，点炮者3独付48。
        var lines = Chengdu1001FanScoring.settleChengdu1001(state,3,3);
        assertEquals(List.of(new MahjongRegionalRules.SettlementLine(3,0,48,"WIN","chengdu1001_fan_scoring")),lines);
        assertEquals(Map.of(0,48L,1,0L,2,0L,3,-48L),settle(state,lines).totals());
    }

    @Test void qingDuiCompositePreventsComponentDoubleCounting() {
        MahjongState state = state(Map.of(0,QING_DUI,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        var matched = Chengdu1001FanHitDetector.detect(state,0,new Chengdu1001FanHitDetector.WinContext(false,false,false,false,false,true,false));
        assertTrue(matched.contains(Chengdu1001FanSchema.QING_DUI));
        assertFalse(matched.contains(Chengdu1001FanSchema.QING_YI_SE));
        assertFalse(matched.contains(Chengdu1001FanSchema.DUI_DUI_HU));
        // 手算：复合清对3 + 门清1 + 选缺1 = 5；组成番不重复进入 scoreFans。
        var scored = MahjongRegionalRules.scoreFans(Chengdu1001FanSchema.schema(),matched,Integer.MAX_VALUE);
        assertEquals(5,scored.cappedFan());
    }

    @Test void sevenPairsRequiresConcealedContext() {
        MahjongState state = state(Map.of(0,SEVEN_PAIRS,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var concealed = Chengdu1001FanHitDetector.detect(state,0,new Chengdu1001FanHitDetector.WinContext(true,false,false,false,false,true,false));
        var exposed = Chengdu1001FanHitDetector.detect(state,0,new Chengdu1001FanHitDetector.WinContext(true,false,false,false,false,false,false));
        assertTrue(concealed.contains(Chengdu1001FanSchema.QI_DUI));
        assertFalse(exposed.contains(Chengdu1001FanSchema.QI_DUI));
        // 手算：七对2 + 门清1 + 选缺1 = 4番。
        assertEquals(4,MahjongRegionalRules.scoreFans(Chengdu1001FanSchema.schema(),concealed,Integer.MAX_VALUE).cappedFan());
    }

    @Test void fengDingZeroMeansLiteralZeroAndProducesNoAmount() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        // 终裁 c 逐字口径：封顶为0必须按0，不得解释成不封顶。4番封为0，amount=0*5=0。
        assertEquals(0L,Chengdu1001FanSchema.capFromFengDing(0));
        assertTrue(Chengdu1001FanScoring.settleChengdu1001(state,0,5).isEmpty());
        assertEquals(Map.of(0,0L,1,0L,2,0L,3,0L),settle(state,List.of()).totals());
    }

    @Test void fengDingOneTwoThreeMapToFiftyHundredAndUnlimited() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 1, false, 3);
        assertEquals(50L,Chengdu1001FanSchema.capFromFengDing(1));
        assertEquals(100L,Chengdu1001FanSchema.capFromFengDing(2));
        assertEquals(Long.MAX_VALUE,Chengdu1001FanSchema.capFromFengDing(3));
        // 当前抽象牌型手算均为4番，小于50/100/无限三档，所以三档各为2^4*1=16。
        assertEquals(16,Chengdu1001FanScoring.settleChengdu1001(state,1,1).get(0).points());
        assertEquals(16,Chengdu1001FanScoring.settleChengdu1001(state,2,1).get(0).points());
        assertEquals(16,Chengdu1001FanScoring.settleChengdu1001(state,3,1).get(0).points());
    }

    @Test void twoWinnersAreScoredIndependentlyAgainstOneDiscarder() {
        MahjongState state = state(Map.of(0,QING,1,QING,2,PING,3,PING), Set.of(0,1), 2, false, 3);
        // 每位赢家：2^4*1=16；点炮者3分别付16，共付32；两响独立且总和0。
        var lines = Chengdu1001FanScoring.settleChengdu1001(state,3,1);
        assertEquals(Set.of(0,1),lines.stream().map(MahjongRegionalRules.SettlementLine::receiver).collect(java.util.stream.Collectors.toSet()));
        assertTrue(lines.stream().allMatch(line -> line.payer()==3 && line.points()==16));
        assertEquals(Map.of(0,16L,1,16L,2,0L,3,-32L),settle(state,lines).totals());
    }

    @Test void settlementLinesRemainRestoreCompatibleAndConserved() {
        MahjongState state = state(Map.of(0,QING,1,PING,2,PING,3,PING), Set.of(0), 0, true, -1);
        var lines = Chengdu1001FanScoring.settleChengdu1001(state,3,1);
        assertEquals(Set.of(1,2,3),lines.stream().map(MahjongRegionalRules.SettlementLine::payer).collect(java.util.stream.Collectors.toSet()));
        assertTrue(lines.stream().allMatch(line -> line.receiver()==0 && line.category().equals("WIN") && line.source().equals("chengdu1001_fan_scoring")));
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
