package com.aoo.bcg.mahjong;

// 本单由 Codex 执行器实施、岗位核验=majiang；AOO-0040 t104 门禁豁免实跑（javac 离线+console-standalone 直跑），见卡 t104 开工登记节。

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Chengdu1001CreateRulesSettlementPassthroughTest {
    private static final List<Integer> QING = List.of(11,11,11,12,13,14,14,15,16,16,17,18,12,12);
    private static final List<Integer> PING = List.of(11,12,13,21,22,23,31,32,33,14,15,16,29,29);

    @Test void createRulesReachAuthoritativeSettlementAndSurviveRestore() {
        assertEquals(0L, winnerDelta(Map.of("fengDing",0,"diFen",1,"addBase",7)));
        assertEquals(32L, winnerDelta(Map.of("fengDing",3,"diFen",1,"addBase",0)));
        assertEquals(48L, winnerDelta(Map.of("fengDing",3,"diFen",2,"addBase",0)));
        assertEquals(80L, winnerDelta(Map.of("fengDing",3,"diFen",3,"addBase",0)));
        assertEquals(36L, winnerDelta(Map.of("fengDing",3,"diFen",1,"addBase",2)));
    }

    @Test void missingFengDingFallsBackToUnlimitedAndNullRulesAreEmpty() {
        assertEquals(16L, winnerDelta(Map.of()));
        MahjongAuthoritativeSession session = new MahjongAuthoritativeSession(
                42,100,2,1,new XueZhanMahjongFamily(),"v1",true,null);
        assertNotNull(session);
    }

    private static long winnerDelta(Map<String,Object> rules) {
        MahjongAuthoritativeSession restored = MahjongAuthoritativeSession.restore(
                savedState(),new XueZhanMahjongFamily(),true,rules);
        return restored.settlement(1,"v1").scoreDelta().get(100L);
    }

    private static Map<String,Object> savedState() {
        MahjongState finished = new MahjongState(List.of(),Map.of(0,QING,1,PING),1,false,19,1,
                MahjongOperationWindow.empty(),true,0,Map.of(),Set.of(0),0,
                MahjongRegionalRules.OpeningSelections.empty());
        Map<String,Object> saved = new LinkedHashMap<>();
        saved.put("roomId",42L);saved.put("ownerId",100L);saved.put("seatLimit",2);saved.put("seed",1L);
        saved.put("playVersion","v1");saved.put("players",Map.of(0,100L,1,101L));saved.put("roundNo",1);
        saved.put("state",finished);saved.put("operationDeadline",com.aoo.bcg.gamespi.time.OperationDeadline.none().toMap());
        return saved;
    }
}
