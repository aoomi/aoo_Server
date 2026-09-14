package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PdkCommonArchitectureTest {
    private static final List<String> COMMON_FILES = List.of(
            "PokerAuthoritativeSession.java", "PaoDeKuaiConfig.java", "PaoDeKuaiFamily.java",
            "PaoDeKuaiRuleSet.java", "PaoDeKuaiEngine.java", "PdkCardPatternPolicy.java",
            "PdkScoringPolicy.java", "PdkSettlementContext.java", "PdkVariantPolicy.java",
            "PdkRuleProfiles.java", "PdkPublishedRuleOptions.java");

    @Test void commonFoundationContainsNoRegionalIdentityOrGameIdSwitch() throws Exception {
        Path root = Path.of("src/main/java/com/aoo/bcg/poker");
        for (String file : COMMON_FILES) {
            String source = Files.readString(root.resolve(file)).toLowerCase();
            assertFalse(source.contains("njpdk") || source.contains("xcpdk")
                    || source.contains("aypdk") || source.contains("hbpdk")
                    || source.contains("sichuan") || source.contains("hubei")
                    || source.contains("neijiang") || source.contains("xuancheng"), file);
            assertFalse(source.matches("(?s).*switch\\s*\\([^)]*(gameid|region).*"), file);
        }
    }

    @Test void twoIndependentVariantsAssembleThroughTheSamePolicyContract() {
        PdkVariantPolicy first = AypdkRules.policy(AypdkOptions.from(Map.of()));
        PdkVariantPolicy second = HbpdkRules.policy(HbpdkOptions.from(Map.of()));
        assertNotEquals(first.policyId(), second.policyId());
        assertEquals(PaoDeKuaiFamily.CODE, first.family().familyCode());
        assertEquals(PaoDeKuaiFamily.CODE, second.family().familyCode());
        assertNotSame(first.scoringPolicy(), second.scoringPolicy());
    }

    @Test void commonAuthorityCompletesTwoThreeAndFourPlayerGames() {
        for (int playerCount : List.of(2, 3, 4)) {
            PokerRuleProfile base = PdkRuleProfiles.flexibleTwoToFourPlayers(
                    "common-pdk-" + playerCount, null);
            PokerRuleProfile profile = new PokerRuleProfile(base.version(), base.deckSize(), 2, 4,
                    base.firstLead(), base.requiredFirstCard(), base.minimumStraightLength(),
                    base.minimumPairRunLength(), base.allowTwoInRuns(), base.allowJokersInRuns(),
                    false, false, base.bombUnit(),
                    base.multiplierCap(), base.minimumPlaneLength(), base.maximumHandSize(), base.deck());
            PaoDeKuaiFamily family = new PaoDeKuaiFamily(
                    new PaoDeKuaiConfig(5, true, true, false, null, false), profile);
            PokerAuthoritativeSession session = new PokerAuthoritativeSession(
                    9000 + playerCount, 100, playerCount, 71, family);
            long sequence = 1;
            for (int seat = 1; seat < playerCount; seat++)
                session.execute(command(session, "join_req", sequence++, seat, 100 + seat, Map.of()));
            for (int seat = 0; seat < playerCount; seat++)
                session.execute(command(session, "ready_req", sequence++, seat, 100 + seat, Map.of()));
            int guard = 0;
            while (!Boolean.TRUE.equals(session.viewFor(100).get("finished"))) {
                assertTrue(guard++ < 500, "authority did not converge for " + playerCount);
                int seat = ((Number) session.viewFor(100).get("currentSeat")).intValue();
                long player = 100L + seat;
                PokerTurnState state = (PokerTurnState) session.authoritativeState().get("state");
                Integer playable = state.hands().get(seat).stream().filter(card -> {
                    CardCombination single = family.rules().recognize(List.of(card),
                            new PaoDeKuaiContext(false, 0, state.hands().get(seat)));
                    return family.rules().canBeat(single, state.previous(),
                            new PaoDeKuaiContext(false, 0, state.hands().get(seat)));
                }).findFirst().orElse(null);
                session.execute(playable == null
                        ? command(session, "pass_req", sequence++, seat, player, Map.of())
                        : command(session, "play_req", sequence++, seat, player,
                                Map.of("cards", List.of(playable))));
            }
            assertTrue(session.invariantViolations().isEmpty());
            assertEquals(0L, session.settlement(1, family.profile().version()).scoreDelta()
                    .values().stream().mapToLong(Long::longValue).sum());
        }
    }

    @Test void productionPdkProviderUsesTheCommonTwoToFourPlayerProfile() {
        PdkGameProvider provider = new PdkGameProvider();
        for (int playerCount : List.of(2, 3, 4)) {
            PokerAuthoritativeSession session = (PokerAuthoritativeSession) provider.roomFactory().create(
                    new com.aoo.bcg.gamespi.RoomCreationContext(9100 + playerCount, 100,
                            Map.of("playerCount", playerCount, "roundCount", 8))).requireAuthoritativeSession();
            long sequence = 1;
            for (int seat = 1; seat < playerCount; seat++)
                session.execute(command(session, "join_req", sequence++, seat, 100 + seat, Map.of()));
            for (int seat = 0; seat < playerCount; seat++)
                session.execute(command(session, "ready_req", sequence++, seat, 100 + seat, Map.of()));
            assertEquals("PLAYING", session.viewFor(100).get("phase"));
            assertEquals(playerCount, session.viewFor(100).get("playerCount"));
        }
    }

    @Test void productionPdkProviderAutoStartsWhenAllSeatsAreReadyWithoutOwnerStart() {
        PdkGameProvider provider = new PdkGameProvider();
        PokerAuthoritativeSession session = (PokerAuthoritativeSession) provider.roomFactory().create(
                new com.aoo.bcg.gamespi.RoomCreationContext(9200, 100,
                        Map.of("playerCount", 2, "roundCount", 8))).requireAuthoritativeSession();
        session.execute(command(session, "join_req", 1, 1, 101, Map.of()));
        session.execute(command(session, "ready_req", 2, 0, 100, Map.of()));
        session.execute(command(session, "ready_req", 3, 1, 101, Map.of()));
        assertEquals("PLAYING", session.viewFor(100).get("phase"));
        @SuppressWarnings("unchecked")
        Map<Integer,Object> seats = (Map<Integer,Object>) session.viewFor(100).get("seats");
        for (Object rawSeat : seats.values()) {
            @SuppressWarnings("unchecked")
            Map<String,Object> seat = (Map<String,Object>) rawSeat;
            assertTrue(((List<?>) seat.get("cards")).size() > 0);
        }
    }

    @Test void productionProviderMigratesOnlyTheKnownWaitingSnapshotIdentity() {
        PdkGameProvider provider = new PdkGameProvider();
        Map<String,Object> waiting = new java.util.LinkedHashMap<>(provider.roomFactory().create(
                new com.aoo.bcg.gamespi.RoomCreationContext(9199, 100,
                        Map.of("playerCount", 2, "roundCount", 8)))
                .requireAuthoritativeSession().authoritativeState());
        PaoDeKuaiFamily family = provider.pokerFamily();
        waiting.put("ruleSnapshotKey", legacyRecordIdentity(family));
        assertDoesNotThrow(() -> provider.restoreAuthoritativeSession(waiting).orElseThrow());
        waiting.put("ruleSnapshotKey", "unknown");
        assertThrows(IllegalStateException.class,
                () -> provider.restoreAuthoritativeSession(waiting).orElseThrow());
    }

    private static String legacyRecordIdentity(PaoDeKuaiFamily family) {
        try {
            String input = family.profile() + "|" + family.rules().config();
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest
                    .getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static GameCommandRequest command(PokerAuthoritativeSession session, String msgId,
            long sequence, int seat, long player, Map<String,Object> body) {
        Map<String,Object> state = session.authoritativeState();
        return new GameCommandRequest(msgId, "common-" + sequence, sequence,
                ((Number) state.get("roomId")).longValue(),
                ((Number) state.get("roundNo")).intValue(), String.valueOf(state.get("ruleVersion")),
                String.valueOf(player), seat, body);
    }
}
