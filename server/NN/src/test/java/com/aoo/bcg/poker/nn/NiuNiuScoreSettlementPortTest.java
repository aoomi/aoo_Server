package com.aoo.bcg.poker.nn;

import com.aoo.bcg.common.settlement.InMemoryRoomScoreLedger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NiuNiuScoreSettlementPortTest {
  @Test void capsByPlayerCarryAndBankerBudgetThenCommitsOnce() {
    var ledger = new InMemoryRoomScoreLedger();
    ledger.seed(2981, Map.of(101L, 5L, 102L, 20L, 103L, 4L));
    var port = new NiuNiuScoreSettlementPort(ledger);
    Map<Integer,Long> players = Map.of(0,101L,1,102L,2,103L);
    var carry = port.capture(2981, players);
    var first = port.settle("cn298:2981:1",2981,1,0,players,carry,List.of(1),List.of(2),Map.of(1,20L,2,10L));
    assertEquals(Map.of(0,-5L,1,9L,2,-4L),first.seatDeltas());
    assertEquals(Map.of(101L,0L,102L,29L,103L,0L),first.receipt().balances());
    var retry = port.settle("cn298:2981:1",2981,1,0,players,carry,List.of(1),List.of(2),Map.of(1,20L,2,10L));
    assertTrue(retry.receipt().replayed()); assertEquals(first.seatDeltas(),retry.seatDeltas());
  }

  @Test void restoredCarrySnapshotDetectsExternalBalanceChange() {
    var ledger = new InMemoryRoomScoreLedger(); ledger.seed(2982,Map.of(101L,10L,102L,10L));
    var port = new NiuNiuScoreSettlementPort(ledger); Map<Integer,Long> players=Map.of(0,101L,1,102L);
    var stale=port.capture(2982,players);
    ledger.commit(new com.aoo.bcg.gamespi.RoomScoreLedger.Command("external",2982,1,stale.revision(),
        stale.playerBalances(),Map.of(101L,-1L,102L,1L)));
    assertThrows(IllegalStateException.class,()->port.settle("cn298:2982:1",2982,1,0,players,stale,
        List.of(1),List.of(),Map.of(1,1L)));
  }
}
