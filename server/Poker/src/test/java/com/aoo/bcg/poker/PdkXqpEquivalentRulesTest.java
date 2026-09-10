package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
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

    @Test void liangshanRobberMustMakeSpringAndEachOpponentSharesTheLoss() {
        Map<String,Object> published = Map.ofEntries(
                Map.entry("baseScore", 2), Map.entry("competeDealerEnabled", true),
                Map.entry("competeDealerMustSpringToWin", true),
                Map.entry("jinHuaScoreUnit", 1),
                Map.entry("bombScoreMode", "MULTIPLIER"), Map.entry("bombScoreCap", 2),
                Map.entry("handScoreTable", List.of(1, 0, 2, 1, 5, 2)));
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        Map<Integer,Long> players = Map.of(0, 10L, 1, 11L, 2, 12L);
        Map<Integer,List<Integer>> hands = Map.of(0, List.of(), 1, List.of(103),
                2, List.of(104));
        Map<Integer,Integer> bombs = Map.of(0, 0, 1, 1, 2, 0);
        PokerRuleProfile profile = PdkRuleProfiles.flexibleTwoToFourPlayers("ls-xqp", null);

        PdkSettlementContext failedSpring = new PdkSettlementContext(91, 1, "ls-xqp",
                0, 0, players, hands, Map.of(0, 2, 1, 1, 2, 0), bombs,
                Map.of(0, 4, 1, 1, 2, 0), Map.of(0, 0, 1, 0, 2, 0), 1, 0,
                profile, config);
        Map<Long,Long> failed = PdkScoringPolicy.standard().settle(failedSpring).scoreDelta();
        // 3 × 底分 × 1 个炸弹 = 12；抢庄者未春天时每位闲家各得 12。
        assertEquals(-26L, failed.get(10L));
        assertEquals(16L, failed.get(11L));
        assertEquals(10L, failed.get(12L));

        PdkSettlementContext madeSpring = new PdkSettlementContext(91, 1, "ls-xqp",
                0, 0, players, hands, Map.of(0, 2, 1, 0, 2, 0), bombs,
                Map.of(0, 4, 1, 0, 2, 0), Map.of(0, 0, 1, 0, 2, 0), 1, 0,
                profile, config);
        Map<Long,Long> made = PdkScoringPolicy.standard().settle(madeSpring).scoreDelta();
        assertEquals(22L, made.get(10L));
        assertEquals(-8L, made.get(11L));
        assertEquals(-14L, made.get(12L));

        PdkSettlementContext noCompete = new PdkSettlementContext(91, 1, "ls-xqp",
                0, 0, players, Map.of(0, List.of(), 1, List.of(103, 104, 105, 106, 107),
                        2, List.of(108, 109)), Map.of(0, 1, 1, 1, 2, 1),
                Map.of(0, 0, 1, 0, 2, 0), Map.of(0, 1, 1, 5, 2, 2),
                Map.of(0, 0, 1, 0, 2, 0), -1, -1, profile, config);
        Map<Long,Long> ordinary = PdkScoringPolicy.standard().settle(noCompete).scoreDelta();
        assertEquals(6L, ordinary.get(10L));
        assertEquals(-4L, ordinary.get(11L));
        assertEquals(-2L, ordinary.get(12L));
    }

    @Test void liangshanOpponentPlayImmediatelyEndsRobberRoundAndWins() {
        PaoDeKuaiFamily family = family("ls-robber-immediate", Map.of(
                "cardsPerPlayer", 4, "competeDealerEnabled", true,
                "competeDealerMustSpringToWin", true));
        for (long room = 940; room < 1040; room++) {
            PokerAuthoritativeSession session = twoPlayerSession(room, family);
            Map<String,Object> compete = session.viewFor(10);
            int dealer = ((Number) compete.get("currentSeat")).intValue();
            session.execute(command(room, session, "compete_dealer_req", 4, dealer,
                    10 + dealer, Map.of("compete", true)));
            if (!"PLAYING".equals(session.viewFor(10).get("phase"))) continue;

            long sequence = 5;
            List<CardCombination> dealerHints = hints(session, sequence++, dealer);
            int dealerCards = cardCount(session, dealer);
            CardCombination dealerPlay = dealerHints.stream()
                    .filter(candidate -> candidate.cards().size() < dealerCards)
                    .findFirst().orElse(null);
            if (dealerPlay == null) continue;
            session.execute(command(room, session, "play_req", sequence++, dealer,
                    10 + dealer, Map.of("cards", dealerPlay.cards())));
            Map<String,Object> afterDealer = session.viewFor(10);
            if (Boolean.TRUE.equals(afterDealer.get("finished"))) continue;

            int opponent = ((Number) afterDealer.get("currentSeat")).intValue();
            if (opponent == dealer) continue;
            int opponentCards = cardCount(session, opponent);
            CardCombination opponentPlay = hints(session, sequence++, opponent).stream()
                    .filter(candidate -> candidate.cards().size() < opponentCards)
                    .findFirst().orElse(null);
            if (opponentPlay == null) continue;

            Map<String,Object> result = session.execute(command(room, session, "play_req",
                    sequence, opponent, 10 + opponent,
                    Map.of("cards", opponentPlay.cards()))).body();
            assertEquals(true, result.get("finished"));
            assertEquals(opponent, ((Number) result.get("winnerSeat")).intValue());
            assertEquals(dealer, ((Number) result.get("competeDealerSeat")).intValue());
            assertTrue(cardCount(result, opponent) > 0,
                    "the opponent wins when the first hand is played, not after running out");
            @SuppressWarnings("unchecked") Map<Integer,Object> seats =
                    (Map<Integer,Object>) result.get("seats");
            assertTrue(((Number) ((Map<?,?>) seats.get(dealer)).get("roundScore")).longValue() < 0);
            assertTrue(((Number) ((Map<?,?>) seats.get(opponent)).get("roundScore")).longValue() > 0);
            assertEquals(session.authoritativeState(),
                    PokerAuthoritativeSession.restore(session.authoritativeState(), family)
                            .authoritativeState());
            return;
        }
        fail("no deterministic deal produced a non-terminal opponent response");
    }

    @Test void liangshanRobberSpringRequirementIsInTheImmutableRuleSnapshot() {
        LiangshanPdkRules regional = new LiangshanPdkRules();
        PaoDeKuaiConfig compete = PdkPublishedRuleOptions.apply(
                regional.authoritativeRules(Map.of("robDealerRule", "dealer_first"), 2),
                regional.defaults());
        PaoDeKuaiConfig noCompete = PdkPublishedRuleOptions.apply(
                regional.authoritativeRules(Map.of("robDealerRule", "no_compete"), 2),
                regional.defaults());
        assertTrue(compete.advancedRules().dealerRule().mustSpringToWin());
        assertFalse(noCompete.advancedRules().dealerRule().mustSpringToWin());
        Map<String,Object> snapshot = PdkPublishedRuleOptions.snapshot(compete);
        assertEquals(true, snapshot.get("competeDealerMustSpringToWin"));
        assertEquals(compete, PdkPublishedRuleOptions.apply(snapshot, regional.defaults()));
    }

    @Test void persistedPreSpringRuleSnapshotRestoresToTheCurrentLiangshanRule() {
        PaoDeKuaiFamily family = family("ls-legacy-snapshot", Map.of(
                "cardsPerPlayer", 4, "competeDealerEnabled", true,
                "competeDealerMustSpringToWin", true));
        PokerAuthoritativeSession session = twoPlayerSession(93, family);
        Map<String,Object> legacy = new LinkedHashMap<>(session.authoritativeState());
        legacy.put("ruleSnapshotKey", beforeRobberSpringSnapshotKey(family));
        assertTrue(family.matchesLegacyRuleSnapshotKey(String.valueOf(
                legacy.get("ruleSnapshotKey"))));
        Map<String,Object> migrated = PdkPublishedRuleOptions.migrateVerifiedLegacySnapshotIdentity(
                legacy, family);
        PokerAuthoritativeSession restored = PokerAuthoritativeSession.restore(migrated, family);
        assertEquals(family.ruleSnapshotKey(), restored.authoritativeState().get("ruleSnapshotKey"));
        assertEquals(session.viewFor(10).get("competeDealerSeat"),
                restored.viewFor(10).get("competeDealerSeat"));
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

    @Test void readyPlayerReallyLeavesBeforeCardsArePresented() {
        PaoDeKuaiFamily family = family("leave-before-deal", Map.of(
                "cardsPerPlayer", 4, "competeDealerEnabled", true,
                "competeDealerStartAfterBanker", true));
        PokerAuthoritativeSession session = twoPlayerSession(94, family);
        assertEquals("COMPETE_DEALER", session.viewFor(11).get("phase"));
        assertEquals(false, session.viewFor(11).get("cardsDealt"));

        session.execute(command(94, session, "leave_req", 4, 1, 11, Map.of()));

        assertEquals("WAITING", session.viewFor(10).get("phase"));
        assertEquals(false, session.viewFor(10).get("cardsDealt"));
        assertFalse(((Map<?,?>) session.authoritativeState().get("players")).containsValue(11L));
        assertEquals(0, session.authoritativeState().get("roundNo"));
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

    @Test void workbookSmallSettlementCheckboxMapsToStablePresentationContract() {
        PaoDeKuaiConfig popup = PdkPublishedRuleOptions.apply(Map.of(
                "rule_cd201_0001", List.of("option_0001")), PaoDeKuaiConfig.defaults());
        PaoDeKuaiConfig floating = PdkPublishedRuleOptions.apply(Map.of(
                "rule_nj201_0004", List.of()), PaoDeKuaiConfig.defaults());
        PaoDeKuaiConfig explicit = PdkPublishedRuleOptions.apply(Map.of(
                "rule_ls201_0001", List.of("option_0001"),
                "settlementPresentation", "FLOATING"), PaoDeKuaiConfig.defaults());
        assertEquals(PdkAdvancedRules.SettlementPresentation.POPUP,
                popup.advancedRules().governance().settlementPresentation());
        assertEquals(PdkAdvancedRules.SettlementPresentation.FLOATING,
                floating.advancedRules().governance().settlementPresentation());
        assertEquals(PdkAdvancedRules.SettlementPresentation.FLOATING,
                explicit.advancedRules().governance().settlementPresentation());
    }

    @Test void floatingSettlementStartsExactlyOneNextRoundAfterAuthoritativeDelayAndRestore() {
        PaoDeKuaiFamily family = family("floating-next", Map.of(
                "cardsPerPlayer", 4, "settlementPresentation", "FLOATING"));
        PokerAuthoritativeSession session = twoPlayerSession(88, family);
        finishRound(session, 10);
        long settledVersion = session.stateVersion();
        long dueAt = ((Number) nextRoundDeadline(session).get("deadlineEpochMillis")).longValue();
        assertFalse(session.tickLifecycle(Instant.ofEpochMilli(dueAt - 1)));
        session.participantPresence(11, false, Instant.ofEpochMilli(dueAt - 1));
        PokerAuthoritativeSession restored = PokerAuthoritativeSession.restore(
                session.authoritativeState(), family);
        restored.participantPresence(11, true, Instant.ofEpochMilli(dueAt));
        assertTrue(restored.tickLifecycle(Instant.ofEpochMilli(dueAt)));
        assertEquals(settledVersion + 1, restored.stateVersion());
        assertEquals(2, restored.viewFor(10).get("roundNo"));
        assertEquals("PLAYING", restored.viewFor(10).get("phase"));
        assertEquals("", nextRoundDeadline(restored).get("operationId"));
        assertFalse(restored.tickLifecycle(Instant.ofEpochMilli(dueAt + 1)));
        assertEquals(restored.authoritativeState(), PokerAuthoritativeSession.restore(
                restored.authoritativeState(), family).authoritativeState());
    }

    @Test void popupAndFinalSettlementNeverScheduleAutomaticNextRound() {
        PaoDeKuaiFamily popup = family("popup-next", Map.of("cardsPerPlayer", 4));
        PokerAuthoritativeSession popupSession = twoPlayerSession(89, popup);
        finishRound(popupSession, 10);
        assertEquals("", nextRoundDeadline(popupSession).get("operationId"));
        assertFalse(popupSession.tickLifecycle(Instant.now().plusSeconds(3)));

        PaoDeKuaiFamily floating = family("final-next", Map.of(
                "cardsPerPlayer", 4, "settlementPresentation", "FLOATING"));
        PokerAuthoritativeSession finalSession = new PokerAuthoritativeSession(90, 10, 2, 4,
                floating, 1);
        finalSession.execute(command(90, finalSession, "join_req", 1, 1, 11, Map.of()));
        finalSession.execute(command(90, finalSession, "ready_req", 2, 0, 10, Map.of()));
        finalSession.execute(command(90, finalSession, "ready_req", 3, 1, 11, Map.of()));
        finishRound(finalSession, 10);
        assertEquals(true, finalSession.viewFor(10).get("matchFinished"));
        assertEquals("", nextRoundDeadline(finalSession).get("operationId"));
    }

    @Test void popupSettlementContinueStartsTheNextRoundWithAPlayableOperationIdAfterRestore() {
        PaoDeKuaiFamily family = family("popup-continue-operation", Map.of("cardsPerPlayer", 4));
        PokerAuthoritativeSession session = twoPlayerSession(92, family);
        long sequence = finishRound(session, 10);
        session.execute(command(92, session, "continue_req", sequence++, 0, 10, Map.of()));
        session.execute(command(92, session, "continue_req", sequence, 1, 11, Map.of()));

        assertEquals(2, session.viewFor(10).get("roundNo"));
        assertEquals("PLAYING", session.viewFor(10).get("phase"));
        @SuppressWarnings("unchecked") Map<String,Object> operationDeadline =
                (Map<String,Object>) session.viewFor(10).get("operationDeadline");
        assertTrue(String.valueOf(operationDeadline.get("operationId")).matches("2-\\d+-play"));
        PokerAuthoritativeSession restored = PokerAuthoritativeSession.restore(
                session.authoritativeState(), family);
        assertEquals(operationDeadline, restored.viewFor(10).get("operationDeadline"));

        Map<String,Object> legacySnapshot = new LinkedHashMap<>(session.authoritativeState());
        Map<String,Object> legacyDeadline = new LinkedHashMap<>(operationDeadline);
        legacyDeadline.put("operationId", String.valueOf(operationDeadline.get("operationId"))
                .replace("-play", "-continue"));
        legacySnapshot.put("operationDeadline", legacyDeadline);
        PokerAuthoritativeSession migrated = PokerAuthoritativeSession.restore(legacySnapshot, family);
        assertEquals(operationDeadline, migrated.viewFor(10).get("operationDeadline"));
    }

    @Test void dissolveDuringFloatingSettlementCancelsAutomaticNextRound() {
        PaoDeKuaiFamily family = family("dissolve-next", Map.of(
                "cardsPerPlayer", 4, "settlementPresentation", "FLOATING"));
        PokerAuthoritativeSession session = twoPlayerSession(91, family);
        long sequence = finishRound(session, 10);
        long dueAt = ((Number) nextRoundDeadline(session).get("deadlineEpochMillis")).longValue();
        session.execute(command(91, session, "dissolve_req", sequence, 0, 10, Map.of()));
        assertEquals("", nextRoundDeadline(session).get("operationId"));
        assertFalse(session.tickLifecycle(Instant.ofEpochMilli(dueAt + 1)));
        assertEquals(1, session.viewFor(10).get("roundNo"));
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

    private static long finishRound(PokerAuthoritativeSession session, long sequence) {
        int guard = 0;
        while (!Boolean.TRUE.equals(session.viewFor(10).get("finished"))) {
            assertTrue(guard++ < 50, "authoritative round did not converge");
            int seat = ((Number) session.viewFor(10).get("currentSeat")).intValue();
            long player = 10L + seat;
            var hint = session.execute(command(sessionRoom(session), session, "hint_req",
                    sequence++, seat, player, Map.of()));
            @SuppressWarnings("unchecked") List<CardCombination> hints =
                    (List<CardCombination>) hint.body().get("hints");
            session.execute(command(sessionRoom(session), session,
                    hints.isEmpty() ? "pass_req" : "play_req", sequence++, seat, player,
                    hints.isEmpty() ? Map.of() : Map.of("cards", hints.getFirst().cards())));
        }
        return sequence;
    }

    private static List<CardCombination> hints(
            PokerAuthoritativeSession session, long sequence, int seat) {
        @SuppressWarnings("unchecked") List<CardCombination> hints =
                (List<CardCombination>) session.execute(command(sessionRoom(session), session,
                        "hint_req", sequence, seat, 10L + seat, Map.of())).body().get("hints");
        return hints;
    }

    private static int cardCount(PokerAuthoritativeSession session, int seat) {
        return cardCount(session.viewFor(10), seat);
    }

    @SuppressWarnings("unchecked")
    private static int cardCount(Map<String,Object> view, int seat) {
        return ((Number) ((Map<String,Object>) ((Map<Integer,Object>) view.get("seats"))
                .get(seat)).get("cardCount")).intValue();
    }

    private static String beforeRobberSpringSnapshotKey(PaoDeKuaiFamily family) {
        String current = family.rules().config().toString();
        int field = current.indexOf(", mustSpringToWin=");
        assertTrue(field >= 0);
        int end = current.indexOf(']', field);
        assertTrue(end >= 0);
        String prior = current.substring(0, field) + current.substring(end);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    (family.profile() + "|" + prior).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static long sessionRoom(PokerAuthoritativeSession session) {
        return ((Number) session.authoritativeState().get("roomId")).longValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> nextRoundDeadline(PokerAuthoritativeSession session) {
        return (Map<String,Object>) session.viewFor(10).get("nextRoundDeadline");
    }

    private static GameCommandRequest command(long room, PokerAuthoritativeSession session,
            String id, long sequence, int seat, long player, Map<String,Object> body) {
        int round = ((Number) session.authoritativeState().get("roundNo")).intValue();
        return new GameCommandRequest(id, "xqp-" + sequence, sequence, room,
                Math.max(1, round), session.authoritativeState().get("ruleVersion").toString(),
                String.valueOf(player), seat, body);
    }
}
