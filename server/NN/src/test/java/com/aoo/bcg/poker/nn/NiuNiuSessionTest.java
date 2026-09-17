package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NiuNiuSessionTest {
  @Test @SuppressWarnings("unchecked") void twoRealPlayersFinishOneRoundAndCanContinue() {
    var session = new NiuNiuSession(298001, 1001, 77, NiuNiuRules.defaults());
    assertEquals("SPECTATOR", session.snapshotFor(1001).get("viewerStatus"));
    session.sit(0, 1001, "sit-1"); session.sit(1, 1002, "sit-2");
    assertEquals("ROBBING", session.snapshotFor(1001).get("phase"));
    session.rob(0, 4, "rob-1"); session.rob(1, 2, "rob-2");
    int banker = (int) session.snapshotFor(1001).get("bankerSeat");
    session.bet(banker == 0 ? 1 : 0, 1, "bet-1");
    Map<Integer, List<Integer>> p1Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1001).get("hands");
    Map<Integer, List<Integer>> p2Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1002).get("hands");
    session.split(0, p1Hands.get(0), "split-1"); session.split(1, p2Hands.get(1), "split-2");
    var settled = session.snapshotFor(1001);
    assertEquals("SETTLEMENT", settled.get("phase"));
    assertNotNull(settled.get("lastResult"));
    session.continueNextRound(0, "continue-1"); session.continueNextRound(1, "continue-2");
    assertEquals(2, session.snapshotFor(1001).get("round"));
  }

  @Test void hidesOpponentsCardsAndRejectsDuplicateOperations() {
    var session = new NiuNiuSession(298002, 2001, 88, NiuNiuRules.defaults());
    session.sit(0, 2001, "sit0"); session.sit(1, 2002, "sit1");
    @SuppressWarnings("unchecked") Map<Integer, List<Integer>> visible =
        (Map<Integer, List<Integer>>) session.snapshotFor(2001).get("hands");
    assertTrue(visible.get(1).stream().allMatch(card -> card == 0));
    session.rob(0, 0, "op");
    assertThrows(IllegalStateException.class, () -> session.rob(0, 0, "op"));
  }

  @Test void deterministicTimeoutFallbackContainsNoRobotPolicy() {
    var session = new NiuNiuSession(298003, 3001, 99, NiuNiuRules.defaults());
    session.sit(0, 3001, "sit0"); session.sit(1, 3002, "sit1");
    session.timeout(0, "timeout-rob0"); session.timeout(1, "timeout-rob1");
    int banker = (int) session.snapshotFor(3001).get("bankerSeat");
    session.timeout(banker == 0 ? 1 : 0, "timeout-bet");
    session.timeout(0, "timeout-split0"); session.timeout(1, "timeout-split1");
    assertEquals("SETTLEMENT", session.snapshotFor(3001).get("phase"));
  }

  @Test void sitIsAtomicFailClosedAndReconnectPreservesViewerStatus() {
    var session = new NiuNiuSession(298004, 4001, 100, NiuNiuRules.defaults());
    assertEquals("SPECTATOR", session.snapshotFor(4001).get("viewerStatus"));
    session.sit(3, 4001, "sit-owner");
    assertEquals("SEATED", session.snapshotFor(4001).get("viewerStatus"));
    assertEquals(3, session.snapshotFor(4001).get("viewerSeat"));
    assertEquals(Set.of(3), session.snapshotFor(4001).get("readySeats"));
    assertEquals("CN298_ALREADY_SEATED", assertThrows(IllegalStateException.class,
        () -> session.sit(4, 4001, "repeat-player")).getMessage());
    assertEquals("CN298_SEAT_OCCUPIED", assertThrows(IllegalStateException.class,
        () -> session.sit(3, 4002, "occupied")).getMessage());
    assertEquals("CN298_INVALID_SEAT", assertThrows(IllegalStateException.class,
        () -> session.sit(8, 4002, "illegal-seat")).getMessage());
    assertEquals("SPECTATOR", session.snapshotFor(4002).get("viewerStatus"));
    assertEquals("SEATED", session.snapshotFor(4001).get("viewerStatus"));
  }
}
