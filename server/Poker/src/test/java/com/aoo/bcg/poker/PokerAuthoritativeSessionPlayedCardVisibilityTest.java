package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PokerAuthoritativeSessionPlayedCardVisibilityTest {
  @Test
  void allInOrderPublishesActiveSeatHistoryToEveryRecipientAndAfterRestore() {
    PaoDeKuaiFamily family = family("all-in-order", PaoDeKuaiConfig.PlayedCardVisibility.ALL_IN_ORDER);
    PokerAuthoritativeSession session = twoPlayerSession(101, family);
    PlayedCards played = playNonTerminal(session, 10);

    assertAllInOrderActiveHistory(session, played);
    PokerAuthoritativeSession restored = PokerAuthoritativeSession.restore(
        session.authoritativeState(), family);
    assertAllInOrderActiveHistory(restored, played);

    finishRound(restored, played.nextSequence());
    assertFinishedHistoryMatchesAuthority(restored);
  }

  @Test
  void lastOnlyKeepsActiveSeatHistoryHiddenAndPublishesItOnlyAfterRoundEnd() {
    PaoDeKuaiFamily family = family("last-only", PaoDeKuaiConfig.PlayedCardVisibility.LAST_ONLY);
    PokerAuthoritativeSession session = twoPlayerSession(102, family);
    PlayedCards played = playNonTerminal(session, 10);

    assertActiveSeatHistoryHidden(session);
    PokerAuthoritativeSession restored = PokerAuthoritativeSession.restore(
        session.authoritativeState(), family);
    assertActiveSeatHistoryHidden(restored);

    finishRound(restored, played.nextSequence());
    assertFinishedHistoryMatchesAuthority(restored);
  }

  private static PaoDeKuaiFamily family(
      String version, PaoDeKuaiConfig.PlayedCardVisibility visibility) {
    Map<String, Object> published = Map.of(
        "cardsPerPlayer", 4,
        "playedCardVisibility", visibility.name());
    PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published, PaoDeKuaiConfig.defaults());
    List<Integer> deck = List.of(103, 203, 303, 403, 104, 204, 304, 404);
    Map<String, Object> withDeck = new LinkedHashMap<>(published);
    withDeck.put("deckCards", deck);
    PokerRuleProfile base = new PokerRuleProfile(
        version, deck.size(), 2, 2, PokerRuleProfile.FirstLead.RANDOM, null, 5, 2,
        false, false, true, true, 1, 16, 2, 8, deck);
    return new PaoDeKuaiFamily(
        config, PdkPublishedRuleOptions.profile(version, withDeck, config, base));
  }

  private static PokerAuthoritativeSession twoPlayerSession(long room, PaoDeKuaiFamily family) {
    PokerAuthoritativeSession session = new PokerAuthoritativeSession(room, 10, 2, 4, family, 8);
    session.execute(command(session, "join_req", 1, 1, 11, Map.of()));
    session.execute(command(session, "ready_req", 2, 0, 10, Map.of()));
    session.execute(command(session, "ready_req", 3, 1, 11, Map.of()));
    return session;
  }

  @SuppressWarnings("unchecked")
  private static PlayedCards playNonTerminal(PokerAuthoritativeSession session, long sequence) {
    int seat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
    long player = 10L + seat;
    Map<String, Object> ownSeat = seat(session.viewFor(player), seat);
    int cardsBeforePlay = ((Number) ownSeat.get("cardCount")).intValue();
    var hint = session.execute(command(session, "hint_req", sequence++, seat, player, Map.of()));
    List<CardCombination> hints = (List<CardCombination>) hint.body().get("hints");
    CardCombination played = hints.stream()
        .filter(candidate -> candidate.cards().size() < cardsBeforePlay)
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected a non-terminal authoritative play"));
    session.execute(command(
        session, "play_req", sequence++, seat, player, Map.of("cards", played.cards())));
    assertFalse((Boolean) session.viewFor(10).get("finished"));
    return new PlayedCards(seat, List.copyOf(played.cards()), sequence);
  }

  private static void assertAllInOrderActiveHistory(
      PokerAuthoritativeSession session, PlayedCards played) {
    for (long recipient : List.of(10L, 11L)) {
      assertEquals(played.cards(), seat(session.viewFor(recipient), played.seat()).get("playedCards"));
    }
    long opponent = 10L + (1 - played.seat());
    List<?> hiddenHand = (List<?>) seat(session.viewFor(opponent), played.seat()).get("cards");
    assertTrue(hiddenHand.stream().allMatch(card -> Integer.valueOf(0).equals(card)));
  }

  private static void assertActiveSeatHistoryHidden(PokerAuthoritativeSession session) {
    for (long recipient : List.of(10L, 11L)) {
      assertTrue(((List<?>) seat(session.viewFor(recipient), 0).get("playedCards")).isEmpty());
      assertTrue(((List<?>) seat(session.viewFor(recipient), 1).get("playedCards")).isEmpty());
    }
  }

  @SuppressWarnings("unchecked")
  private static void assertFinishedHistoryMatchesAuthority(PokerAuthoritativeSession session) {
    assertTrue((Boolean) session.viewFor(10).get("finished"));
    Map<Integer, List<Integer>> expected =
        (Map<Integer, List<Integer>>) session.authoritativeState().get("playedCardsBySeat");
    for (long recipient : List.of(10L, 11L)) {
      for (int seat = 0; seat < 2; seat++) {
        assertEquals(expected.getOrDefault(seat, List.of()),
            seat(session.viewFor(recipient), seat).get("playedCards"));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> seat(Map<String, Object> view, int seat) {
    return (Map<String, Object>) ((Map<Integer, Object>) view.get("seats")).get(seat);
  }

  @SuppressWarnings("unchecked")
  private static void finishRound(PokerAuthoritativeSession session, long sequence) {
    int guard = 0;
    while (!Boolean.TRUE.equals(session.viewFor(10).get("finished"))) {
      assertTrue(guard++ < 50, "authoritative round did not converge");
      int seat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
      long player = 10L + seat;
      var hint = session.execute(command(session, "hint_req", sequence++, seat, player, Map.of()));
      List<CardCombination> hints = (List<CardCombination>) hint.body().get("hints");
      session.execute(command(
          session, hints.isEmpty() ? "pass_req" : "play_req", sequence++, seat, player,
          hints.isEmpty() ? Map.of() : Map.of("cards", hints.getFirst().cards())));
    }
  }

  private static GameCommandRequest command(
      PokerAuthoritativeSession session,
      String message,
      long sequence,
      int seat,
      long player,
      Map<String, Object> body) {
    Map<String, Object> state = session.authoritativeState();
    return new GameCommandRequest(
        message,
        "played-visibility-" + sequence,
        sequence,
        ((Number) state.get("roomId")).longValue(),
        ((Number) state.get("roundNo")).intValue(),
        String.valueOf(state.get("ruleVersion")),
        String.valueOf(player),
        seat,
        body);
  }

  private record PlayedCards(int seat, List<Integer> cards, long nextSequence) {}
}
