package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

final class PokerDissolveLifecycleTest {
  @Test
  void unanimousVotePublishesClosedTerminalSnapshotAndDuplicateApprovalIsHarmless() {
    PokerAuthoritativeSession session = started();
    session.execute(command("dissolve_req", "dissolve", 4, 20, 0));
    session.execute(command("dissolve_agree_req", "approve-21", 5, 21, 1));
    session.execute(command("dissolve_agree_req", "approve-22", 6, 22, 2));
    session.execute(command("dissolve_agree_req", "approve-23", 7, 23, 3));

    long terminalVersion = session.stateVersion();
    assertTrue(session.isTerminal());
    assertEquals("DISSOLVED", session.viewFor(20).get("phase"));
    assertClosedDeadline(session.viewFor(20).get("operationDeadline"));
    assertClosedDeadline(session.viewFor(20).get("nextRoundDeadline"));
    assertTrue(session.invariantViolations().isEmpty());

    var duplicate = session.execute(command("dissolve_agree_req", "approve-retry", 8, 21, 1));
    assertEquals(terminalVersion, session.stateVersion());
    assertEquals("DISSOLVED", duplicate.body().get("phase"));
    assertEquals(session.authoritativeState(),
        PokerAuthoritativeSession.restore(session.authoritativeState(), family()).authoritativeState());
  }

  @Test
  void refusalLeavesTheRoomPlayableAndANewVoteCanStillCloseIt() {
    PokerAuthoritativeSession session = started();
    session.execute(command("dissolve_req", "first", 4, 20, 0));
    session.execute(command("dissolve_refuse_req", "refuse", 5, 21, 1));

    assertFalse(session.isTerminal());
    assertEquals("PLAYING", session.viewFor(20).get("phase"));
    assertEquals("VOTE_REJECTED", session.viewFor(20).get("dissolveReason"));

    session.execute(command("dissolve_req", "second", 6, 20, 0));
    session.execute(command("dissolve_agree_req", "approve-21", 7, 21, 1));
    session.execute(command("dissolve_agree_req", "approve-22", 8, 22, 2));
    session.execute(command("dissolve_agree_req", "approve-23", 9, 23, 3));
    assertTrue(session.isTerminal());
    assertEquals("VOTE_APPROVED", session.terminalReason());
  }

  @Test
  void concurrentFinalVotesHaveOneAtomicTerminalOutcome() throws Exception {
    PokerAuthoritativeSession session = started();
    session.participantPresence(21, false, java.time.Instant.now());
    session.execute(command("dissolve_req", "dissolve", 6, 20, 0));

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Void>> votes = List.of(
          () -> { session.execute(command("dissolve_agree_req", "approve-21", 7, 21, 1)); return null; },
          () -> { session.execute(command("dissolve_agree_req", "approve-22", 8, 22, 2)); return null; },
          () -> { session.execute(command("dissolve_agree_req", "approve-23", 9, 23, 3)); return null; });
      for (var result : executor.invokeAll(votes)) result.get();
    } finally {
      executor.shutdownNow();
    }

    assertTrue(session.isTerminal());
    assertEquals("VOTE_APPROVED", session.terminalReason());
    assertEquals("DISSOLVED", session.viewFor(22).get("phase"));
    assertTrue(session.invariantViolations().isEmpty());
  }

  private static PokerAuthoritativeSession started() {
    int seats = 4;
    PokerAuthoritativeSession session = new PokerAuthoritativeSession(91, 20, seats, 13, family());
    long sequence = 1;
    for (int seat = 1; seat < seats; seat++)
      session.execute(command("join_req", "join-" + seat, sequence++, 20 + seat, seat));
    for (int seat = 0; seat < seats; seat++)
      session.execute(command("ready_req", "ready-" + seat, sequence++, 20 + seat, seat));
    assertEquals("PLAYING", session.viewFor(20).get("phase"));
    return session;
  }

  private static GameCommandRequest command(String msgId, String requestId, long sequence,
      long player, int seat) {
    return new GameCommandRequest("common.room." + msgId, requestId, sequence, 91, 1,
        "pdk-v1", Long.toString(player), seat, Map.of());
  }

  private static PaoDeKuaiFamily family() {
    return new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults());
  }

  private static void assertClosedDeadline(Object value) {
    assertTrue(value instanceof Map<?, ?>);
    assertEquals(0L, ((Number) ((Map<?, ?>) value).get("deadlineEpochMillis")).longValue());
  }
}
