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
    int ownerSeat = sit(session, 1001, "sit-1");
    int guestSeat = sit(session, 1002, "sit-2");
    session.start(1001, "start");
    assertEquals("ROBBING", session.snapshotFor(1001).get("phase"));
    assertEquals(4, ((Map<Integer, List<Integer>>) session.snapshotFor(1001).get("hands")).get(ownerSeat).size());
    session.rob(ownerSeat, 4, "rob-1"); session.rob(guestSeat, 2, "rob-2");
    int banker = (int) session.snapshotFor(1001).get("bankerSeat");
    session.bet(banker == ownerSeat ? guestSeat : ownerSeat, 1, "bet-1");
    Map<Integer, List<Integer>> p1Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1001).get("hands");
    Map<Integer, List<Integer>> p2Hands =
        (Map<Integer, List<Integer>>) session.snapshotFor(1002).get("hands");
    assertEquals(5, p1Hands.get(ownerSeat).size());
    session.split(ownerSeat, p1Hands.get(ownerSeat).subList(0, 3), "split-1");
    session.split(guestSeat, p2Hands.get(guestSeat).subList(0, 3), "split-2");
    var settled = session.snapshotFor(1001);
    assertEquals("SETTLEMENT", settled.get("phase"));
    assertFalse(((Map<?, ?>) settled.get("roundSettlement")).isEmpty());
    assertEquals(2, ((Map<?, ?>) settled.get("playerStats")).size());
    assertEquals(List.of(Math.min(ownerSeat, guestSeat), Math.max(ownerSeat, guestSeat)), settled.get("pendingSeats"));
    session.continueNextRound(ownerSeat, "continue-1");
    assertEquals(List.of(guestSeat), session.snapshotFor(1001).get("pendingSeats"));
    session.continueNextRound(guestSeat, "continue-2");
    assertEquals(2, session.snapshotFor(1001).get("round"));
  }

  @Test @SuppressWarnings("unchecked") void publishesOnlyXqpRobAndBetOptions() {
    var session = new NiuNiuSession(298005, 5001, 101, NiuNiuRules.defaults());
    int ownerSeat = sit(session, 5001, "sit0");
    int guestSeat = sit(session, 5002, "sit1");
    session.start(5001, "start");
    assertEquals(List.of(0, 2, 3, 4), session.snapshotFor(5001).get("robOptions"));
    assertThrows(IllegalArgumentException.class, () -> session.rob(ownerSeat, 1, "bad-rob"));
    session.rob(ownerSeat, 4, "rob0"); session.rob(guestSeat, 2, "rob1");
    assertEquals(List.of(1, 2), session.snapshotFor(5002).get("betOptions"));
  }

  @Test void hidesOpponentsCardsAndRejectsDuplicateOperations() {
    var session = new NiuNiuSession(298002, 2001, 88, NiuNiuRules.defaults());
    int ownerSeat = sit(session, 2001, "sit0");
    int guestSeat = sit(session, 2002, "sit1");
    session.start(2001, "start");
    @SuppressWarnings("unchecked") Map<Integer, List<Integer>> visible =
        (Map<Integer, List<Integer>>) session.snapshotFor(2001).get("hands");
    assertTrue(visible.get(guestSeat).stream().allMatch(card -> card == 0));
    session.rob(ownerSeat, 0, "op");
    assertThrows(IllegalStateException.class, () -> session.rob(ownerSeat, 0, "op"));
  }

  @Test void deterministicTimeoutFallbackContainsNoRobotPolicy() {
    var session = new NiuNiuSession(298003, 3001, 99, NiuNiuRules.defaults());
    int ownerSeat = sit(session, 3001, "sit0");
    int guestSeat = sit(session, 3002, "sit1");
    session.start(3001, "start");
    session.timeout(ownerSeat, "timeout-rob0"); session.timeout(guestSeat, "timeout-rob1");
    int banker = (int) session.snapshotFor(3001).get("bankerSeat");
    session.timeout(banker == ownerSeat ? guestSeat : ownerSeat, "timeout-bet");
    session.timeout(ownerSeat, "timeout-split0"); session.timeout(guestSeat, "timeout-split1");
    assertEquals("SETTLEMENT", session.snapshotFor(3001).get("phase"));
  }

  @Test @SuppressWarnings("unchecked") void splitRejectsInvalidOrRepeatedSelectionWithoutAdvancingState() {
    var session = new NiuNiuSession(298008, 8001, 104, NiuNiuRules.defaults());
    int ownerSeat = sit(session, 8001, "sit0");
    int guestSeat = sit(session, 8002, "sit1");
    session.start(8001, "start");
    session.rob(ownerSeat, 4, "rob0"); session.rob(guestSeat, 0, "rob1");
    session.bet(guestSeat, 1, "bet1");
    Map<Integer, List<Integer>> hands = (Map<Integer, List<Integer>>) session.snapshotFor(8001).get("hands");
    List<Integer> hand = hands.get(ownerSeat);
    long version = session.stateVersion();
    assertThrows(IllegalArgumentException.class, () -> session.split(ownerSeat, hand.subList(0, 2), "short"));
    assertThrows(IllegalArgumentException.class,
        () -> session.split(ownerSeat, List.of(hand.get(0), hand.get(0), hand.get(1)), "duplicate-card"));
    assertThrows(IllegalArgumentException.class,
        () -> session.split(ownerSeat, List.of(hand.get(0), hand.get(1), 999), "foreign-card"));
    assertEquals(version, session.stateVersion());
    session.split(ownerSeat, hand.subList(0, 3), "valid");
    long acceptedVersion = session.stateVersion();
    assertThrows(IllegalArgumentException.class, () -> session.split(ownerSeat, hand.subList(0, 3), "repeat-seat"));
    assertEquals(acceptedVersion, session.stateVersion());
  }

  @Test void sitIsAtomicFailClosedAndReconnectPreservesViewerStatus() {
    var session = new NiuNiuSession(298004, 4001, 100, NiuNiuRules.defaults());
    assertEquals("SPECTATOR", session.snapshotFor(4001).get("viewerStatus"));
    int ownerSeat = sit(session, 4001, "sit-owner");
    assertEquals("SEATED", session.snapshotFor(4001).get("viewerStatus"));
    assertTrue(ownerSeat >= 0 && ownerSeat < (int) session.snapshotFor(4001).get("maxPlayers"));
    assertEquals(ownerSeat, session.snapshotFor(4001).get("viewerSeat"));
    assertEquals(Set.of(ownerSeat), session.snapshotFor(4001).get("readySeats"));
    assertEquals("CN298_ALREADY_SEATED", assertThrows(IllegalStateException.class,
        () -> session.sit(4001, "repeat-player")).getMessage());
    assertEquals("SPECTATOR", session.snapshotFor(4002).get("viewerStatus"));
    assertEquals("SEATED", session.snapshotFor(4001).get("viewerStatus"));
    int capacity = (int) session.snapshotFor(4001).get("maxPlayers");
    for (int index = 2; index <= capacity; index++) sit(session, 4000L + index, "sit-" + index);
    long sequence = (long) session.authoritativeState().get("seatRandomSequence");
    assertEquals("CN298_SEATS_FULL", assertThrows(IllegalStateException.class,
        () -> session.sit(4999, "full")).getMessage());
    assertEquals(sequence, session.authoritativeState().get("seatRandomSequence"));
  }

  @Test void onlyOwnerStartsAndDuplicateStartIsRejectedWithoutStateCorruption() {
    var session = new NiuNiuSession(298006, 6001, 102, NiuNiuRules.defaults());
    sit(session, 6001, "sit0"); sit(session, 6002, "sit1");
    assertEquals("CN298_ONLY_OWNER_CAN_START", assertThrows(IllegalStateException.class,
        () -> session.start(6002, "guest-start")).getMessage());
    session.start(6001, "owner-start");
    assertEquals("ROBBING", session.phase().name());
    assertEquals("duplicate CN298 operationId", assertThrows(IllegalStateException.class,
        () -> session.start(6001, "owner-start")).getMessage());
  }

  @Test void concurrentStartAndSitHaveOneSerializableOutcome() throws Exception {
    var session = new NiuNiuSession(298007, 7001, 103, NiuNiuRules.defaults());
    sit(session, 7001, "sit0"); sit(session, 7002, "sit1");
    var gate = new CountDownLatch(1);
    var startFailure = new AtomicReference<Throwable>();
    var sitFailure = new AtomicReference<Throwable>();
    Thread start = Thread.ofVirtual().start(() -> runAfter(gate,
        () -> session.start(7001, "start"), startFailure));
    Thread sit = Thread.ofVirtual().start(() -> runAfter(gate,
        () -> session.sit(7003, "sit2"), sitFailure));
    gate.countDown(); start.join(); sit.join();
    assertEquals("ROBBING", session.phase().name());
    assertTrue(session.players().size() == 2 || session.players().size() == 3);
    assertNull(startFailure.get());
    if (session.players().size() == 2) assertNotNull(sitFailure.get()); else assertNull(sitFailure.get());
  }

  @Test void durableSnapshotRestoresWaitingAndInProgressAuthorityExactly() {
    var waiting = new NiuNiuSession(298009, 9001, 105, NiuNiuRules.defaults());
    assertEquals(waiting.authoritativeState(), NiuNiuSession.restore(waiting.authoritativeState()).authoritativeState());
    int ownerSeat = sit(waiting, 9001, "sit0");
    sit(waiting, 9002, "sit1");
    waiting.start(9001, "start");
    var restored = NiuNiuSession.restore(waiting.authoritativeState());
    assertEquals(waiting.authoritativeState(), restored.authoritativeState());
    waiting.rob(ownerSeat, 4, "rob0"); restored.rob(ownerSeat, 4, "rob0");
    assertEquals(waiting.authoritativeState(), restored.authoritativeState());
    assertEquals(waiting.snapshotFor(9001), restored.snapshotFor(9001));
  }

  @Test void durableSnapshotRejectsUnknownOrIncompleteSchemas() {
    var session = new NiuNiuSession(298010, 10001, 106, NiuNiuRules.defaults());
    var wrong = new java.util.LinkedHashMap<>(session.authoritativeState()); wrong.put("schemaVersion", 4);
    assertEquals("CN298_UNSUPPORTED_AUTHORITY_SNAPSHOT",
        assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(wrong)).getMessage());
    var incomplete = new java.util.LinkedHashMap<>(session.authoritativeState()); incomplete.remove("rules");
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(incomplete));
  }

  private static int sit(NiuNiuSession session, long playerId, String operationId) {
    session.sit(playerId, operationId);
    return (int) session.snapshotFor(playerId).get("viewerSeat");
  }

  private static void runAfter(CountDownLatch gate, Runnable action, AtomicReference<Throwable> failure) {
    try { gate.await(); action.run(); } catch (Throwable error) { failure.set(error); }
  }
}
