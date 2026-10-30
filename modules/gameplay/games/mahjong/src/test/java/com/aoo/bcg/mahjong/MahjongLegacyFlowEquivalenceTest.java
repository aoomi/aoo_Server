package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavior evidence for the standalone Mahjong flow candidates.
 *
 * <p>This test deliberately uses the catalog provider and the unified
 * authoritative session only. It does not import, reflectively load, or
 * mention any candidate implementation.</p>
 */
final class MahjongLegacyFlowEquivalenceTest {
    private static final String VERSION = "1.0.0";

    @Test
    void chengdu1001CatalogSessionCompletesOpeningAndMultipleWinnerSettlement() {
        MahjongRegionRuntimeProfile profile = chengdu1001();
        CatalogMahjongFamilyProvider provider = provider(profile);
        long roomId = 911001L;
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerNum", 3, "setCount", 2, "shuffleSeed", 9)));
        AuthoritativeGameSession session = room.requireAuthoritativeSession();

        session.execute(command(profile, roomId, 1, 0, 10, "join", Map.of()));
        session.execute(command(profile, roomId, 2, 1, 20, "join", Map.of()));
        session.execute(command(profile, roomId, 3, 2, 30, "join", Map.of()));
        session.execute(command(profile, roomId, 4, 0, 10, "start", Map.of()));

        MahjongState started = state(session);
        for (int seat = 0; seat < 3; seat++) {
            long player = playerFor(seat);
            session.execute(command(profile, roomId, 5 + seat, seat, player, "exchange",
                    Map.of("tiles", exchangeChoice(started.hands().get(seat)))));
        }
        for (int seat = 0; seat < 3; seat++) {
            session.execute(command(profile, roomId, 8 + seat, seat, playerFor(seat), "dingque",
                    Map.of("suit", MahjongSuit.WAN.name())));
        }

        MahjongState afterOpening = state(session);
        assertEquals(3, afterOpening.openingSelections().exchanges().size());
        assertEquals(3, afterOpening.openingSelections().missingSuits().size());
        assertEquals("PLAYING", session.viewFor(10).get("openingPhase"));

        MahjongState winningRound = multiWinnerState();
        Map<String, Object> prepared = withState(session.authoritativeState(), winningRound, "PLAYING");
        session = provider.restoreAuthoritativeSession(prepared).orElseThrow();

        session.execute(command(profile, roomId, 12, 0, 10, "hu", Map.of("tile", 34)));
        assertEquals(Set.of(0), state(session).winners());
        assertFalse(state(session).finished());
        session.execute(command(profile, roomId, 13, 1, 20, "draw", Map.of()));
        session.execute(command(profile, roomId, 14, 1, 20, "hu", Map.of("tile", 34)));
        assertTrue(state(session).finished());
        assertEquals(Set.of(0, 1), state(session).winners());
        assertEquals("SETTLED", session.authoritativeState().get("phase"));
        assertEquals(true, session.viewFor(10).get("roundScored"));
        SettlementPayload chengduSettlement = provider.settlementProvider().orElseThrow()
                .settle(new GameRoomHandle(roomId, profile.gameId(), VERSION, session), 1);
        assertEquals(0L, chengduSettlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum());

        MahjongRegionRuntimeProfile standard = standardProfile();
        CatalogMahjongFamilyProvider standardProvider = provider(standard);
        long standardRoomId = 911004L;
        GameRoomHandle standardRoom = standardProvider.roomFactory().create(new RoomCreationContext(standardRoomId, 10,
                Map.of("playerNum", 3, "shuffleSeed", 11)));
        AuthoritativeGameSession standardSession = standardRoom.requireAuthoritativeSession();
        standardSession.execute(command(standard, standardRoomId, 1, 0, 10, "join", Map.of()));
        standardSession.execute(command(standard, standardRoomId, 2, 1, 20, "join", Map.of()));
        standardSession.execute(command(standard, standardRoomId, 3, 2, 30, "join", Map.of()));
        standardSession = standardProvider.restoreAuthoritativeSession(
                withState(standardSession.authoritativeState(), multiWinnerDiscardState(), "PLAYING"))
                .orElseThrow();
        assertEquals(Set.of(0, 1), state(standardSession).winners());
        SettlementPayload settlement = standardProvider.settlementProvider().orElseThrow()
                .settle(new GameRoomHandle(standardRoomId, standard.gameId(), VERSION, standardSession), 1);
        assertEquals(Map.of(10L, 1L, 20L, 1L, 30L, -2L), settlement.scoreDelta());
        assertEquals(0L, settlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum());
        assertTrue(session.invariantViolations().isEmpty());
        assertTrue(standardSession.invariantViolations().isEmpty());
    }

    @Test
    void wallExhaustionSettlementSurvivesRecoveryAndStartsExactlyOneNextRound() throws Exception {
        MahjongRegionRuntimeProfile profile = chengdu1001();
        CatalogMahjongFamilyProvider provider = provider(profile);
        long roomId = 911002L;
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerNum", 2, "setCount", 2, "shuffleSeed", 17)));
        AuthoritativeGameSession session = room.requireAuthoritativeSession();
        session.execute(command(profile, roomId, 1, 0, 10, "join", Map.of()));
        session.execute(command(profile, roomId, 2, 1, 20, "join", Map.of()));
        session.execute(command(profile, roomId, 3, 0, 10, "start", Map.of()));

        List<Integer> existingWinner = List.of(21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 32, 33, 34);
        List<Integer> existingPayer = List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 11, 12, 13, 14);
        MahjongState drawRound = new MahjongState(
                List.of(),
                Map.of(0, existingWinner, 1, existingPayer),
                0,
                false,
                34,
                1,
                MahjongOperationWindow.empty(),
                false,
                0,
                Map.of(),
                Set.of(0),
                0,
                completeOpening(2));
        session = provider.restoreAuthoritativeSession(withState(session.authoritativeState(), drawRound, "PLAYING"))
                .orElseThrow();

        session.execute(command(profile, roomId, 4, 0, 10, "draw", Map.of()));
        assertTrue(state(session).finished());
        assertEquals("SETTLED", session.authoritativeState().get("phase"));
        assertEquals(true, session.viewFor(10).get("roundScored"));
        @SuppressWarnings("unchecked")
        Map<Long, Long> roundScore = (Map<Long, Long>) session.viewFor(10).get("roundScoreDelta");
        assertEquals(Set.of(10L, 20L), roundScore.keySet());
        assertTrue(roundScore.get(10L) > 0);
        assertTrue(roundScore.get(20L) < 0);
        assertEquals(0L, roundScore.values().stream().mapToLong(Long::longValue).sum());

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(mapper.writeValueAsString(session.authoritativeState()), Map.class);
        AuthoritativeGameSession recovered = provider.restoreAuthoritativeSession(json).orElseThrow();
        assertEquals(session.authoritativeState(), recovered.authoritativeState());
        long settledVersion = recovered.stateVersion();

        recovered.execute(command(profile, roomId, 5, 0, 10, "continue", Map.of()));
        assertEquals(List.of(0), recovered.viewFor(10).get("continueSeats"));
        assertEquals("SETTLED", recovered.authoritativeState().get("phase"));

        @SuppressWarnings("unchecked")
        Map<String, Object> partialJson = mapper.readValue(
                mapper.writeValueAsString(recovered.authoritativeState()), Map.class);
        AuthoritativeGameSession recoveredAgain = provider.restoreAuthoritativeSession(partialJson).orElseThrow();
        recoveredAgain.execute(command(profile, roomId, 6, 1, 20, "continue", Map.of()));
        assertEquals("PLAYING", recoveredAgain.authoritativeState().get("phase"));
        assertEquals(2, recoveredAgain.viewFor(10).get("roundNo"));
        assertEquals(List.of(), recoveredAgain.viewFor(10).get("continueSeats"));
        assertFalse((Boolean) recoveredAgain.viewFor(10).get("roomFinished"));
        assertTrue(recoveredAgain.stateVersion() > settledVersion);
    }

    @Test
    void catalogIdentityUsesOneUnifiedProviderAndReplayKeepsStateVersion() {
        MahjongRegionRuntimeProfile profile = chengdu1001();
        CatalogMahjongFamilyProvider provider = provider(profile);
        long roomId = 911003L;
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(roomId, 10,
                Map.of("playerNum", 2, "shuffleSeed", 23)));
        assertEquals("chengdu1001", provider.descriptor().code());
        assertEquals("com.aoo.bcg.mahjong.CatalogMahjongFamilyProvider", provider.getClass().getName());

        CatalogMahjongFamilySession session = (CatalogMahjongFamilySession) room.requireAuthoritativeSession();
        assertEquals("com.aoo.bcg.mahjong.CatalogMahjongFamilySession", session.getClass().getName());
        StatePayload base = StatePayload.copyOf(session.authoritativeState());
        session.execute(command(profile, roomId, 1, 0, 10, "join", Map.of()));
        assertTrue(session.stateVersion() > ((Number) base.asMap().get("stateVersion")).longValue());
        StatePayload replayed = provider.eventReplayProvider().orElseThrow()
                .replay(base, session.recordedEvents());
        assertEquals(session.authoritativeState(), replayed.asMap());

        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(session.authoritativeState()).orElseThrow();
        assertEquals(session.stateVersion(), restored.stateVersion());
        assertEquals(session.authoritativeState(), restored.authoritativeState());
    }

    private static MahjongRegionRuntimeProfile chengdu1001() {
        return MahjongRegionRuntimeProfiles.all().stream()
                .filter(profile -> profile.gameId() == 1001 && profile.code().equals("chengdu1001"))
                .findFirst().orElseThrow();
    }

    private static MahjongRegionRuntimeProfile standardProfile() {
        return MahjongRegionRuntimeProfiles.all().stream()
                .filter(profile -> profile.sourceAvailable() && profile.family().equals("mahjong:standard"))
                .findFirst().orElseThrow();
    }

    private static CatalogMahjongFamilyProvider provider(MahjongRegionRuntimeProfile profile) {
        String[] region = profile.region().split("/", 2);
        RegionScope scope = region[0].equals("NATIONAL") ? RegionScope.NATIONAL
                : region[0].equals("CITY") ? RegionScope.CITY : RegionScope.PROVINCE;
        String province = scope == RegionScope.NATIONAL ? "" : region.length > 1 ? region[1] : "";
        GameDescriptor descriptor = new GameDescriptor(profile.gameId(), profile.code(), profile.code(),
                GameCategory.MAHJONG, profile.family().replace(':', '-'), scope, province, "", VERSION);
        return (CatalogMahjongFamilyProvider) MahjongCatalogRuntimeRegistry.providerFor(descriptor).orElseThrow();
    }

    private static GameCommandRequest command(MahjongRegionRuntimeProfile profile, long roomId, long sequence,
            int seat, long player, String action, Map<String, Object> payload) {
        return new GameCommandRequest("mahjong." + profile.code() + ".dispatch", profile.code() + sequence,
                sequence, roomId, 1, VERSION, String.valueOf(player), seat,
                Map.of("action", action, "payload", payload));
    }

    private static long playerFor(int seat) {
        return 10L + seat * 10L;
    }

    private static List<Integer> exchangeChoice(List<Integer> hand) {
        for (MahjongSuit suit : MahjongSuit.values()) {
            List<Integer> choice = hand.stream().filter(suit::contains).limit(3).toList();
            if (choice.size() == 3) return choice;
        }
        throw new AssertionError("no legal exchange choice");
    }

    private static MahjongState state(AuthoritativeGameSession session) {
        return (MahjongState) map(session.authoritativeState().get("mahjongAuthority")).get("state");
    }

    private static MahjongState multiWinnerState() {
        List<Integer> waiting = List.of(21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 32, 33, 34);
        List<Integer> third = List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 11, 12, 13, 14);
        MahjongOperationWindow window = new MahjongOperationWindow(Map.of(
                0, Set.of(MahjongOperation.HU, MahjongOperation.PASS),
                1, Set.of(MahjongOperation.HU, MahjongOperation.PASS)));
        return new MahjongState(List.of(34), Map.of(0, waiting, 1, waiting, 2, third), 2, false,
                34, 2, window, false, -1, Map.of(), Set.of(), 0,
                completeOpening(3));
    }

    private static MahjongState multiWinnerDiscardState() {
        List<Integer> winner = List.of(21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 32, 33, 34, 34);
        List<Integer> secondWinner = winner;
        List<Integer> payer = List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 11, 12, 13, 14);
        return new MahjongState(List.of(), Map.of(0, winner, 1, secondWinner, 2, payer), 2, false,
                34, 2, MahjongOperationWindow.empty(), true, 1, Map.of(), Set.of(0, 1), 0,
                MahjongRegionalRules.OpeningSelections.empty());
    }

    private static MahjongRegionalRules.OpeningSelections completeOpening(int seats) {
        Map<Integer, List<Integer>> exchanges = new LinkedHashMap<>();
        Map<Integer, MahjongSuit> missingSuits = new LinkedHashMap<>();
        for (int seat = 0; seat < seats; seat++) {
            exchanges.put(seat, List.of());
            missingSuits.put(seat, MahjongSuit.WAN);
        }
        return new MahjongRegionalRules.OpeningSelections(exchanges, missingSuits);
    }

    private static Map<String, Object> withState(Map<String, Object> source, MahjongState state, String phase) {
        Map<String, Object> out = new LinkedHashMap<>(source);
        Map<String, Object> authority = new LinkedHashMap<>(map(source.get("mahjongAuthority")));
        authority.put("state", state);
        out.put("mahjongAuthority", authority);
        out.put("phase", phase);
        out.put("wrapperVersion", 0L);
        return out;
    }

    private static Map<String, Object> map(Object value) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) source.forEach((key, item) -> out.put(String.valueOf(key), item));
        return out;
    }
}
