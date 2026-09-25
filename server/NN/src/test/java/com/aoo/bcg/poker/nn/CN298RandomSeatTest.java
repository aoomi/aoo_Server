package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CN298RandomSeatTest {
  @Test void eightAndTenSeatRoomsAllocateUniqueSeatsFromAuthority() {
    for (int capacity : List.of(8, 10)) {
      NiuNiuSession first = seeded(capacity, 501L);
      NiuNiuSession replay = seeded(capacity, 501L);
      for (int index = 0; index < capacity; index++) {
        long playerId = 1001L + index;
        first.sit(playerId, "sit-" + index);
        replay.sit(playerId, "sit-" + index);
        assertEquals(first.players(), replay.players());
        assertEquals(index + 1L, first.authoritativeState().get("seatRandomSequence"));
      }
      assertEquals(capacity, first.players().size());
      assertEquals(capacity, new HashSet<>(first.players().values()).size());
      assertEquals("CN298_SEATS_FULL", assertThrows(IllegalStateException.class,
          () -> first.sit(9999, "full")).getMessage());
      assertEquals((long) capacity, first.authoritativeState().get("seatRandomSequence"));
    }
  }

  @Test void clickedSeatAndDuplicateAttemptsCannotSelectOrConsumeASeat() {
    CN298Authority first = new CN298Authority(seeded(8, 701L), NiuNiuRules.PLAY_VERSION);
    CN298Authority second = new CN298Authority(seeded(8, 701L), NiuNiuRules.PLAY_VERSION);
    first.execute(sitRequest("sit", 0));
    second.execute(sitRequest("sit", 7));
    assertEquals(first.viewFor(1001L).get("viewerSeat"), second.viewFor(1001L).get("viewerSeat"));
    long sequence = (long) first.authoritativeState().get("seatRandomSequence");
    assertEquals("duplicate CN298 operationId", assertThrows(IllegalStateException.class,
        () -> first.execute(sitRequest("sit", 3))).getMessage());
    assertEquals("CN298_ALREADY_SEATED", assertThrows(IllegalStateException.class,
        () -> first.execute(sitRequest("another-sit", 4))).getMessage());
    assertEquals(sequence, first.authoritativeState().get("seatRandomSequence"));
  }

  @Test void concurrentSitsRemainUniqueAndSerializable() throws Exception {
    NiuNiuSession session = seeded(10, 801L);
    CountDownLatch gate = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    List<Thread> workers = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      long playerId = 1001L + index;
      String operationId = "sit-" + index;
      workers.add(Thread.ofVirtual().start(() -> {
        try {
          gate.await();
          session.sit(playerId, operationId);
        } catch (Throwable error) {
          failure.compareAndSet(null, error);
        }
      }));
    }
    gate.countDown();
    for (Thread worker : workers) worker.join();
    assertNull(failure.get());
    assertEquals(10, session.players().size());
    assertEquals(10, new HashSet<>(session.players().keySet()).size());
    assertEquals(10L, session.authoritativeState().get("seatRandomSequence"));
  }

  @Test void legacySingleSeatRestoresButMultiSeatOrderRemainsUnresolved() {
    NiuNiuSession original = seeded(8, 901L);
    original.sit(1001, "sit-1");
    Map<String, Object> v1 = new LinkedHashMap<>(original.authoritativeState());
    v1.put("schemaVersion", 1);
    v1.remove("seatRandomSeed");
    v1.remove("seatRandomSequence");
    v1.remove("joinOrderSeats");

    NiuNiuSession first = NiuNiuSession.restore(v1);
    NiuNiuSession replay = NiuNiuSession.restore(v1);
    assertEquals(3, first.authoritativeState().get("schemaVersion"));
    assertEquals(1L, first.authoritativeState().get("seatRandomSequence"));
    assertEquals(first.authoritativeState(), replay.authoritativeState());
    first.sit(1002, "sit-2");
    replay.sit(1002, "sit-2");
    assertEquals(first.authoritativeState(), replay.authoritativeState());
    assertEquals(first.authoritativeState(), NiuNiuSession.restore(first.authoritativeState()).authoritativeState());
    assertFalse(first.snapshotFor(1002).containsKey("seatRandomSeed"));
    assertFalse(first.snapshotFor(1002).containsKey("seatRandomSequence"));

    Map<String, Object> incomplete = new LinkedHashMap<>(first.authoritativeState());
    incomplete.remove("seatRandomSequence");
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(incomplete));
    Map<String, Object> missingSeed = new LinkedHashMap<>(first.authoritativeState());
    missingSeed.remove("seatRandomSeed");
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(missingSeed));

    Map<String, Object> v2Multi = new LinkedHashMap<>(first.authoritativeState());
    v2Multi.put("schemaVersion", 2);
    v2Multi.remove("joinOrderSeats");
    assertEquals("CN298_LEGACY_JOIN_ORDER_UNRESOLVED", assertThrows(IllegalArgumentException.class,
        () -> NiuNiuSession.restore(v2Multi)).getMessage());
    Map<String, Object> v1Multi = new LinkedHashMap<>(v2Multi);
    v1Multi.put("schemaVersion", 1);
    v1Multi.remove("seatRandomSeed");
    v1Multi.remove("seatRandomSequence");
    assertEquals("CN298_LEGACY_JOIN_ORDER_UNRESOLVED", assertThrows(IllegalArgumentException.class,
        () -> NiuNiuSession.restore(v1Multi)).getMessage());
  }

  @Test @SuppressWarnings("unchecked") void newSnapshotKeepsJoinOrderAndNextRoundHandsBySeat() {
    NiuNiuSession original = seeded(8, 1001L);
    List<Long> playerIds = List.of(1001L, 1002L, 1003L, 1004L);
    List<Integer> joinedSeats = new ArrayList<>();
    for (long playerId : playerIds) {
      original.sit(playerId, "sit-" + playerId);
      joinedSeats.add((int) original.snapshotFor(playerId).get("viewerSeat"));
    }
    Map<String, Object> saved = new LinkedHashMap<>(original.authoritativeState());
    assertEquals(3, saved.get("schemaVersion"));
    assertEquals(joinedSeats, saved.get("joinOrderSeats"));
    Map<Integer, Long> unordered = (Map<Integer, Long>) saved.get("players");
    Map<Integer, Long> reversed = new LinkedHashMap<>();
    for (int index = joinedSeats.size() - 1; index >= 0; index--) {
      int seat = joinedSeats.get(index);
      reversed.put(seat, unordered.get(seat));
    }
    saved.put("players", reversed);
    NiuNiuSession restored = NiuNiuSession.restore(saved);
    assertEquals(original.authoritativeState(), restored.authoritativeState());

    original.start(1001, "start");
    restored.start(1001, "start");
    finishRoundAndContinue(original);
    finishRoundAndContinue(restored);
    assertEquals(2, original.round());
    assertEquals(original.authoritativeState(), restored.authoritativeState());
    for (long playerId : playerIds) {
      Map<String, Object> before = original.snapshotFor(playerId);
      Map<String, Object> after = restored.snapshotFor(playerId);
      int seat = (int) before.get("viewerSeat");
      assertEquals(seat, after.get("viewerSeat"));
      assertEquals(((Map<Integer, List<Integer>>) before.get("hands")).get(seat),
          ((Map<Integer, List<Integer>>) after.get("hands")).get(seat));
    }

    Map<String, Object> missingOrder = new LinkedHashMap<>(saved);
    missingOrder.remove("joinOrderSeats");
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(missingOrder));
    Map<String, Object> duplicateOrder = new LinkedHashMap<>(saved);
    duplicateOrder.put("joinOrderSeats", List.of(joinedSeats.getFirst(), joinedSeats.getFirst()));
    assertThrows(IllegalArgumentException.class, () -> NiuNiuSession.restore(duplicateOrder));
  }

  @Test @SuppressWarnings("unchecked") void seatStreamDoesNotConsumeExistingShuffleStream() {
    NiuNiuSession first = seeded(8, 11L);
    NiuNiuSession second = seeded(8, 22L);
    NiuNiuSession differentDeck = seeded(8, 11L, 78L);
    for (long playerId : List.of(1001L, 1002L)) {
      first.sit(playerId, "sit-" + playerId);
      second.sit(playerId, "sit-" + playerId);
      differentDeck.sit(playerId, "sit-" + playerId);
    }
    first.start(1001, "start");
    second.start(1001, "start");
    differentDeck.start(1001, "start");
    boolean deckChanged = false;
    for (long playerId : List.of(1001L, 1002L)) {
      Map<String, Object> left = first.snapshotFor(playerId);
      Map<String, Object> right = second.snapshotFor(playerId);
      Map<String, Object> otherDeck = differentDeck.snapshotFor(playerId);
      int leftSeat = (int) left.get("viewerSeat");
      int rightSeat = (int) right.get("viewerSeat");
      int otherSeat = (int) otherDeck.get("viewerSeat");
      List<Integer> hand = ((Map<Integer, List<Integer>>) left.get("hands")).get(leftSeat);
      assertEquals(hand, ((Map<Integer, List<Integer>>) right.get("hands")).get(rightSeat));
      deckChanged |= !hand.equals(((Map<Integer, List<Integer>>) otherDeck.get("hands")).get(otherSeat));
    }
    assertTrue(deckChanged);
  }

  private static NiuNiuSession seeded(int capacity, long seatSeed) {
    return seeded(capacity, seatSeed, 77L);
  }

  private static NiuNiuSession seeded(int capacity, long seatSeed, long shuffleSeed) {
    NiuNiuRules defaults = NiuNiuRules.defaults();
    NiuNiuRules rules = new NiuNiuRules(defaults.rounds(), capacity, defaults.startPlayers(),
        defaults.mode(), defaults.maxRobMultiplier(), defaults.maxPushMultiplier(),
        defaults.standPolicy(), defaults.fastModeEnabled(), defaults.kanShunDouEnabled());
    NiuNiuSession created = new NiuNiuSession(298001, 1001, shuffleSeed, rules);
    Map<String, Object> state = new LinkedHashMap<>(created.authoritativeState());
    state.put("seatRandomSeed", seatSeed);
    return NiuNiuSession.restore(state);
  }

  @SuppressWarnings("unchecked")
  private static void finishRoundAndContinue(NiuNiuSession session) {
    List<Integer> seats = session.players().keySet().stream().sorted().toList();
    for (int seat : seats) session.rob(seat, 0, "rob-" + seat);
    int banker = (int) session.snapshotFor(1001).get("bankerSeat");
    for (int seat : seats) {
      if (seat == banker) continue;
      long playerId = session.players().get(seat);
      List<Integer> options = (List<Integer>) session.snapshotFor(playerId).get("betOptions");
      session.bet(seat, options.getFirst(), "bet-" + seat);
    }
    for (int seat : seats) {
      long playerId = session.players().get(seat);
      Map<Integer, List<Integer>> hands = (Map<Integer, List<Integer>>) session.snapshotFor(playerId).get("hands");
      session.split(seat, hands.get(seat).subList(0, 3), "split-" + seat);
    }
    assertEquals(NiuNiuSession.Phase.SETTLEMENT, session.phase());
    for (int seat : seats) session.continueNextRound(seat, "continue-" + seat);
  }

  private static GameCommandRequest sitRequest(String operationId, int clickedSeat) {
    return new GameCommandRequest("poker.cn298.sit_req", operationId, 1, 298001, 0,
        NiuNiuRules.PLAY_VERSION, "1001", clickedSeat, Map.of());
  }
}
