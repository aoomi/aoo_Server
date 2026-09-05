package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PdkXqpEquivalentRulesTest {
    @Test void advancedPublishedRulesRoundTripWithoutDisplayText() {
        Map<String,Object> published = new LinkedHashMap<>();
        published.put("baseScore", 2);
        published.put("requiredFirstCardRounds", 1);
        published.put("bankerSelectionCard", 103);
        published.put("selectBankerEveryRound", true);
        published.put("springMode", "FIXED");
        published.put("springValue", 20);
        published.put("reverseSpringMode", "HAND_TABLE");
        published.put("reverseSpringValue", 2);
        published.put("reverseSpringMaxPlayedCards", 1);
        published.put("handScoreTable", List.of(1, 0, 2, 1, 5, 2));
        published.put("bombScoreMode", "FIXED_POINTS");
        published.put("bombFixedPoints", 5);
        published.put("initialHandPatterns", List.of("FOUR_ACES", "ALL_RED"));
        published.put("initialHandPatternLimit", 2);
        published.put("initialHandPatternScoreUnit", 5);
        published.put("fourOfKindPatternRanks", List.of());
        published.put("directWinPatterns", List.of(List.of(103,203,303,403)));
        published.put("operationTimeoutSeconds", 30);
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        Map<String,Object> snapshot = PdkPublishedRuleOptions.snapshot(config);
        assertEquals(103, snapshot.get("bankerSelectionCard"));
        assertEquals(config, PdkPublishedRuleOptions.apply(snapshot,
                PaoDeKuaiConfig.defaults()));
        assertFalse(snapshot.values().contains("春天"));
    }

    @Test void detectsEveryProvenInitialPatternFromAuthoritativeHand() {
        Map<String,Object> published = Map.of(
                "initialHandPatterns", List.of("FOUR_ACES", "FOUR_CONFIGURED_RANK",
                        "ALL_RED", "ALL_BIG"),
                "fourOfKindPatternRanks", List.of(7));
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        assertTrue(PdkInitialHandEvaluator.patterns(
                List.of(114,214,314,414), config)
                .contains(PdkAdvancedRules.InitialPattern.FOUR_ACES));
        assertTrue(PdkInitialHandEvaluator.patterns(
                List.of(107,207,307,407), config)
                .contains(PdkAdvancedRules.InitialPattern.FOUR_CONFIGURED_RANK));
        assertTrue(PdkInitialHandEvaluator.patterns(
                List.of(210,411,212,414), config)
                .containsAll(List.of(PdkAdvancedRules.InitialPattern.ALL_RED,
                        PdkAdvancedRules.InitialPattern.ALL_BIG)));
    }

    @Test void xqpSpringReverseSpringAndHandTableAreServerAuthoritative() {
        Map<String,Object> published = Map.ofEntries(
                Map.entry("baseScore", 2), Map.entry("springMode", "FIXED"),
                Map.entry("springValue", 20), Map.entry("reverseSpringMode", "HAND_TABLE"),
                Map.entry("reverseSpringValue", 2),
                Map.entry("reverseSpringMaxPlayedCards", 1),
                Map.entry("handScoreTable", List.of(1, 1, 5, 2)),
                Map.entry("bombScoreMode", "DISABLED"));
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        PdkSettlementContext context = new PdkSettlementContext(9, 1, "xqp", 0, 2,
                Map.of(0,10L,1,11L,2,12L),
                Map.of(0,List.of(),1,List.of(103,104,105,106,107),2,List.of(108,109)),
                Map.of(0,3,1,0,2,1), Map.of(0,0,1,0,2,0),
                Map.of(0,5,1,0,2,1), Map.of(0,0,1,0,2,0), -1, -1,
                PdkRuleProfiles.flexibleTwoToFourPlayers("xqp", null), config);
        Map<Long,Long> score = PdkScoringPolicy.standard().settle(context).scoreDelta();
        assertEquals(44L, score.get(10L));
        assertEquals(-40L, score.get(11L));
        assertEquals(-4L, score.get(12L));
    }

    @Test void directWinAndCompeteDealerSurviveImmutableRestore() {
        PaoDeKuaiFamily directFamily = family("direct", Map.of(
                "cardsPerPlayer", 4,
                "directWinPatterns", List.of(List.of(103))));
        PokerAuthoritativeSession direct = twoPlayerSession(81, directFamily);
        assertEquals("DIRECT_WIN", direct.viewFor(10).get("phase"));
        assertEquals(true, direct.viewFor(10).get("finished"));
        Map<String,Object> saved = direct.authoritativeState();
        assertEquals(saved, PokerAuthoritativeSession.restore(saved, directFamily)
                .authoritativeState());

        PaoDeKuaiFamily dealerFamily = family("dealer", Map.of(
                "cardsPerPlayer", 4, "competeDealerEnabled", true,
                "competeDealerStartAfterBanker", true));
        PokerAuthoritativeSession dealer = twoPlayerSession(82, dealerFamily);
        assertEquals("COMPETE_DEALER", dealer.viewFor(10).get("phase"));
        int challenger = ((Number) dealer.viewFor(10).get("currentSeat")).intValue();
        int banker = ((Number) dealer.viewFor(10).get("bankerSeat")).intValue();
        dealer.execute(command(82, dealer, "compete_dealer_req", 5, challenger,
                10 + challenger, Map.of("compete", true)));
        dealer.execute(command(82, dealer, "compete_dealer_req", 6, banker,
                10 + banker, Map.of("compete", false)));
        assertEquals("PLAYING", dealer.viewFor(10).get("phase"));
        assertEquals(challenger, dealer.viewFor(10).get("competeDealerSeat"));
        assertEquals(challenger, dealer.viewFor(10).get("currentSeat"));
    }

    @Test void operationTimeoutAndHostingThresholdDriveServerAutoPlay() {
        PaoDeKuaiFamily family = family("timeout", Map.of(
                "cardsPerPlayer", 4, "operationTimeoutSeconds", 1,
                "hostingMissThreshold", 1));
        PokerAuthoritativeSession session = twoPlayerSession(83, family);
        int timedOutSeat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
        long before = session.stateVersion();
        assertTrue(session.tickLifecycle(session.operationDeadline().deadline()));
        assertEquals(before + 1, session.stateVersion());
        @SuppressWarnings("unchecked") Map<Integer,Object> seats =
                (Map<Integer,Object>) session.viewFor(10).get("seats");
        @SuppressWarnings("unchecked") Map<String,Object> timedOut =
                (Map<String,Object>) seats.get(timedOutSeat);
        assertEquals(true, timedOut.get("hosting"));
        assertEquals(session.authoritativeState(),
                PokerAuthoritativeSession.restore(session.authoritativeState(), family)
                        .authoritativeState());
    }

    @Test void genericRoomPresentationAndInteractionRulesRemainServerAuthoritative() {
        PaoDeKuaiFamily family = family("governance", Map.ofEntries(
                Map.entry("cardsPerPlayer", 4), Map.entry("autoReady", false),
                Map.entry("textChatEnabled", false),
                Map.entry("interactionEnabled", false),
                Map.entry("settlementPresentation", "FLOATING"),
                Map.entry("distanceWarningEnabled", true),
                Map.entry("declaredRoundTimeoutSeconds", 86400)));
        PokerAuthoritativeSession session = new PokerAuthoritativeSession(84, 10, 2, 4,
                family, 8);
        session.execute(command(84, session, "join_req", 1, 1, 11, Map.of()));
        @SuppressWarnings("unchecked") Map<Integer,Object> waitingSeats =
                (Map<Integer,Object>) session.viewFor(10).get("seats");
        assertEquals(false, ((Map<?,?>) waitingSeats.get(0)).get("ready"));
        assertEquals(false, ((Map<?,?>) waitingSeats.get(1)).get("ready"));
        session.execute(command(84, session, "ready_req", 2, 0, 10, Map.of()));
        session.execute(command(84, session, "ready_req", 3, 1, 11, Map.of()));
        assertEquals("FLOATING", session.viewFor(10).get("settlementPresentation"));
        assertEquals(true, session.viewFor(10).get("distanceWarningEnabled"));
        assertThrows(SecurityException.class, () -> session.execute(command(84, session,
                "text_chat_req", 5, 0, 10, Map.of("text", "ignored"))));
        assertThrows(SecurityException.class, () -> session.execute(command(84, session,
                "preset_interaction_req", 6, 0, 10, Map.of("code", 1))));
        assertEquals(true, session.operationDeadline().open());
        Map<String,Object> snapshot = PdkPublishedRuleOptions.snapshot(
                family.rules().config());
        assertEquals(86400, snapshot.get("declaredRoundTimeoutSeconds"));
        assertEquals(20, snapshot.get("operationTimeoutSeconds"));
        assertEquals(family.rules().config(), PdkPublishedRuleOptions.apply(snapshot,
                PaoDeKuaiConfig.defaults()));
    }

    @Test void trustedGatewayPresenceDrivesOfflineDissolveRule() {
        PaoDeKuaiFamily family = family("offline-dissolve", Map.of(
                "cardsPerPlayer", 4, "offlineDissolveSeconds", 60));
        PokerAuthoritativeSession session = twoPlayerSession(85, family);
        session.participantPresence(11, false, java.time.Instant.now().minusSeconds(61));
        session.execute(command(85, session, "dissolve_req", 5, 0, 10, Map.of()));
        assertTrue(session.isTerminal());
        assertEquals("OFFLINE_VOTE_APPROVED", session.terminalReason());
        assertEquals(session.authoritativeState(), PokerAuthoritativeSession.restore(
                session.authoritativeState(), family).authoritativeState());
    }

    @Test void trustedHallAdmissionEnforcesIpAndGpsForThreePlayerRooms() {
        PaoDeKuaiFamily family = family("admission", Map.ofEntries(
                Map.entry("cardsPerPlayer", 2),
                Map.entry("gpsAdmissionRequired", true),
                Map.entry("gpsMinimumDistanceMeters", 100),
                Map.entry("uniqueIpRequired", true)));
        PokerAuthoritativeSession session = new PokerAuthoritativeSession(86, 10, 3, 4,
                family, 8);
        session.admitParticipant(10, 0, new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                "10.0.0.1", 30.0000, 104.0000));
        session.admitParticipant(11, 1, new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                "10.0.0.2", 30.0100, 104.0100));
        session.execute(command(86, session, "join_req", 1, 1, 11, Map.of()));
        assertThrows(SecurityException.class, () -> session.admitParticipant(12, 2,
                new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                        "10.0.0.2", 31.0000, 105.0000)));
        assertThrows(SecurityException.class, () -> session.admitParticipant(12, 2,
                new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                        "10.0.0.3", 30.0101, 104.0101)));
        assertEquals(session.authoritativeState(), PokerAuthoritativeSession.restore(
                session.authoritativeState(), family).authoritativeState());
    }

    @Test void observerEntryRequiresExplicitAuthoritativeSeatClaim() {
        PaoDeKuaiFamily family = family("observer", Map.of(
                "cardsPerPlayer", 4, "entryMode", "OBSERVER"));
        PokerAuthoritativeSession session = new PokerAuthoritativeSession(87, 10, 2, 4,
                family, 8);
        session.admitParticipant(11, 1, new com.aoo.bcg.gamespi.RoomAdmissionAuthority.Admission(
                "10.0.0.2", null, null));
        session.execute(command(87, session, "join_req", 1, 1, 11, Map.of()));
        assertEquals(11L, ((Map<?,?>) session.authoritativeState().get("observers")).get(1));
        assertFalse(((Map<?,?>) session.authoritativeState().get("players")).containsValue(11L));
        session.execute(command(87, session, "take_seat_req", 2, 1, 11, Map.of()));
        assertEquals(11L, ((Map<?,?>) session.authoritativeState().get("players")).get(1));
        assertTrue(((Map<?,?>) session.authoritativeState().get("observers")).isEmpty());
        assertEquals(session.authoritativeState(), PokerAuthoritativeSession.restore(
                session.authoritativeState(), family).authoritativeState());
    }

    private static PaoDeKuaiFamily family(String version, Map<String,Object> published) {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        List<Integer> deck = List.of(103,203,303,403,104,204,304,404);
        Map<String,Object> withDeck = new LinkedHashMap<>(published);
        withDeck.put("deckCards", deck);
        PokerRuleProfile base = new PokerRuleProfile(version, deck.size(), 2, 2,
                PokerRuleProfile.FirstLead.RANDOM, null, 5, 2, false, false,
                true, true, 1, 16, 2, 8, deck);
        return new PaoDeKuaiFamily(config,
                PdkPublishedRuleOptions.profile(version, withDeck, config, base));
    }

    private static PokerAuthoritativeSession twoPlayerSession(long room,
            PaoDeKuaiFamily family) {
        PokerAuthoritativeSession session = new PokerAuthoritativeSession(room, 10, 2, 4,
                family, 8);
        session.execute(command(room, session, "join_req", 1, 1, 11, Map.of()));
        session.execute(command(room, session, "ready_req", 2, 0, 10, Map.of()));
        session.execute(command(room, session, "ready_req", 3, 1, 11, Map.of()));
        return session;
    }

    private static GameCommandRequest command(long room, PokerAuthoritativeSession session,
            String id, long sequence, int seat, long player, Map<String,Object> body) {
        int round = ((Number) session.authoritativeState().get("roundNo")).intValue();
        return new GameCommandRequest(id, "xqp-" + sequence, sequence, room,
                Math.max(1, round), session.authoritativeState().get("ruleVersion").toString(),
                String.valueOf(player), seat, body);
    }
}
