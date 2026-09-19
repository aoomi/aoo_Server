package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NiuNiuSessionTest {
  @Test @SuppressWarnings("unchecked") void twoRealPlayersFinishOneRoundAndCanContinue() {
    var session = new NiuNiuSession(298001, 1001, 77, NiuNiuRules.defaults());
    assertEquals("SPECTATOR", session.snapshotFor(1001).get("viewerStatus"));
    session.sit(0, 1001, "sit-1"); session.sit(1, 1002, "sit-2"); session.start(1001, "start");
    assertEquals("ROBBING", session.snapshotFor(1001).get("phase"));
    assertEquals(4, ((Map<Integer, List<Integer>>) session.snapshotFor(1001).get("hands")).get(0).size());
    session.rob(0, 4, "rob-1"); session.rob(1, 2, "rob-2");
    int banker = (int) session.snapshotFor(1001).get("bankerSeat");
    session.bet(banker == 0 ? 1 : 0, 1, "bet-1");
    Map<Integer, List<Integer>> p1Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1001).get("hands");
    Map<Integer, List<Integer>> p2Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1002).get("hands");
    assertEquals(5, p1Hands.get(0).size());
    session.split(0, p1Hands.get(0).subList(0, 3), "split-1");
    session.split(1, p2Hands.get(1).subList(0, 3), "split-2");
    var settled = session.snapshotFor(1001);
    assertEquals("SETTLEMENT", settled.get("phase"));
    assertFalse(((Map<?, ?>) settled.get("roundSettlement")).isEmpty());
    assertEquals(2, ((Map<?, ?>) settled.get("playerStats")).size());
    assertEquals(List.of(0, 1), settled.get("pendingSeats"));
    session.continueNextRound(0, "continue-1");
    assertEquals(List.of(1), session.snapshotFor(1001).get("pendingSeats"));
    session.continueNextRound(1, "continue-2");
    assertEquals(2, session.snapshotFor(1001).get("round"));
  }

  @Test @SuppressWarnings("unchecked") void publishesOnlyXqpRobAndBetOptions() {
    var session = new NiuNiuSession(298005, 5001, 101, NiuNiuRules.defaults());
    session.sit(0, 5001, "sit0"); session.sit(1, 5002, "sit1"); session.start(5001, "start");
    assertEquals(List.of(0, 2, 3, 4), session.snapshotFor(5001).get("robOptions"));
    assertThrows(IllegalArgumentException.class, () -> session.rob(0, 1, "bad-rob"));
    session.rob(0, 4, "rob0"); session.rob(1, 2, "rob1");
    assertEquals(List.of(1, 2), session.snapshotFor(5002).get("betOptions"));
  }

  @Test void hidesOpponentsCardsAndRejectsDuplicateOperations() {
    var session = new NiuNiuSession(298002, 2001, 88, NiuNiuRules.defaults());
    session.sit(0, 2001, "sit0"); session.sit(1, 2002, "sit1"); session.start(2001, "start");
    @SuppressWarnings("unchecked") Map<Integer, List<Integer>> visible =
        (Map<Integer, List<Integer>>) session.snapshotFor(2001).get("hands");
    assertTrue(visible.get(1).stream().allMatch(card -> card == 0));
    session.rob(0, 0, "op");
    assertThrows(IllegalStateException.class, () -> session.rob(0, 0, "op"));
  }

  @Test void deterministicTimeoutFallbackContainsNoRobotPolicy() {
    var session = new NiuNiuSession(298003, 3001, 99, NiuNiuRules.defaults());
    session.sit(0, 3001, "sit0"); session.sit(1, 3002, "sit1"); session.start(3001, "start");
    session.timeout(0, "timeout-rob0"); session.timeout(1, "timeout-rob1");
    int banker = (int) session.snapshotFor(3001).get("bankerSeat");
    session.timeout(banker == 0 ? 1 : 0, "timeout-bet");
    session.timeout(0, "timeout-split0"); session.timeout(1, "timeout-split1");
    assertEquals("SETTLEMENT", session.snapshotFor(3001).get("phase"));
  }

  @Test @SuppressWarnings("unchecked") void splitRejectsInvalidOrRepeatedSelectionWithoutAdvancingState() {
    var session = new NiuNiuSession(298008, 8001, 104, NiuNiuRules.defaults());
    session.sit(0, 8001, "sit0"); session.sit(1, 8002, "sit1"); session.start(8001, "start");
    session.rob(0, 4, "rob0"); session.rob(1, 0, "rob1");
    session.bet(1, 1, "bet1");
    Map<Integer, List<Integer>> hands = (Map<Integer, List<Integer>>) session.snapshotFor(8001).get("hands");
    List<Integer> hand = hands.get(0);
    long version = session.stateVersion();
    assertThrows(IllegalArgumentException.class, () -> session.split(0, hand.subList(0, 2), "short"));
    assertThrows(IllegalArgumentException.class,
        () -> session.split(0, List.of(hand.get(0), hand.get(0), hand.get(1)), "duplicate-card"));
    assertThrows(IllegalArgumentException.class,
        () -> session.split(0, List.of(hand.get(0), hand.get(1), 999), "foreign-card"));
    assertEquals(version, session.stateVersion());
    session.split(0, hand.subList(0, 3), "valid");
    long acceptedVersion = session.stateVersion();
    assertThrows(IllegalArgumentException.class, () -> session.split(0, hand.subList(0, 3), "repeat-seat"));
    assertEquals(acceptedVersion, session.stateVersion());
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

  @Test void onlyOwnerStartsAndDuplicateStartIsRejectedWithoutStateCorruption() {
    var session = new NiuNiuSession(298006, 6001, 102, NiuNiuRules.defaults());
    session.sit(0, 6001, "sit0"); session.sit(1, 6002, "sit1");
    assertEquals("CN298_ONLY_OWNER_CAN_START", assertThrows(IllegalStateException.class,
        () -> session.start(6002, "guest-start")).getMessage());
    session.start(6001, "owner-start");
    assertEquals("ROBBING", session.phase().name());
    assertEquals("duplicate CN298 operationId", assertThrows(IllegalStateException.class,
        () -> session.start(6001, "owner-start")).getMessage());
  }

  @Test void concurrentStartAndSitHaveOneSerializableOutcome() throws Exception {
    var session = new NiuNiuSession(298007, 7001, 103, NiuNiuRules.defaults());
    session.sit(0, 7001, "sit0"); session.sit(1, 7002, "sit1");
    var gate = new CountDownLatch(1);
    var startFailure = new AtomicReference<Throwable>();
    var sitFailure = new AtomicReference<Throwable>();
    Thread start = Thread.ofVirtual().start(() -> runAfter(gate,
        () -> session.start(7001, "start"), startFailure));
    Thread sit = Thread.ofVirtual().start(() -> runAfter(gate,
        () -> session.sit(2, 7003, "sit2"), sitFailure));
    gate.countDown(); start.join(); sit.join();
    assertEquals("ROBBING", session.phase().name());
    assertTrue(session.players().size() == 2 || session.players().size() == 3);
    assertNull(startFailure.get());
    if (session.players().size() == 2) assertNotNull(sitFailure.get()); else assertNull(sitFailure.get());
  }

  @Test void durableSnapshotRestoresWaitingAndInProgressAuthorityExactly() {
    var waiting = new NiuNiuSession(298009, 9001, 105, NiuNiuRules.defaults());
    assertEquals(waiting.authoritativeState(), NiuNiuSession.restore(waiting.authoritativeState()).authoritativeState());
    waiting.sit(0, 9001, "sit0"); waiting.sit(1, 9002, "sit1"); waiting.start(9001, "start");
    var restored = NiuNiuSession.restore(waiting.authoritativeState());
    assertEquals(waiting.authoritativeState(), restored.authoritativeState());
    waiting.rob(0, 4, "rob0"); restored.rob(0, 4, "rob0");
    assertEquals(waiting.authoritativeState(), restored.authoritativeState());
    assertEquals(waiting.snapshotFor(9001), restored.snapshotFor(9001));
  }

  @Test void durableSnapshotRejectsUnknownOrIncompleteSchemas() {
    var session = new NiuNiuSession(298010, 10001, 106, NiuNiuRules.defaults());
    var wrong = new java.util.LinkedHashMap<>(session.authoritativeState()); wrong.put("schemaVersion", 2);
    assertEquals("CN298_UNSUPPORTED_AUTHORITY_SNAPSHOT",
        assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(wrong)).getMessage());
    var incomplete = new java.util.LinkedHashMap<>(session.authoritativeState()); incomplete.remove("rules");
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(incomplete));
  }

  private static void runAfter(CountDownLatch gate, Runnable action, AtomicReference<Throwable> failure) {
    try { gate.await(); action.run(); } catch (Throwable error) { failure.set(error); }
  }
}
