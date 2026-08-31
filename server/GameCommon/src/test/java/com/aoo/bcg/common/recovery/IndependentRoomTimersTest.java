package com.aoo.bcg.common.recovery;
import java.util.Map;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class IndependentRoomTimersTest{
 @Test void settlementContinuationReadyAndNextRoundAreIndependent(){var state=new RoomDeadlineSnapshot(Map.of("settlementDisplay",100L,"continueDecision",200L,"autoReady",300L,"nextRound",400L,"roomExpiration",500L));assertEquals(100L,state.deadlineEpochMillis().get("settlementDisplay"));assertEquals(200L,state.deadlineEpochMillis().get("continueDecision"));assertEquals(300L,state.deadlineEpochMillis().get("autoReady"));assertEquals(400L,state.deadlineEpochMillis().get("nextRound"));assertEquals(500L,state.deadlineEpochMillis().get("roomExpiration"));assertEquals(RoomTimerKind.values().length,state.deadlineEpochMillis().size());}
}
