package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PdkUnlimitedOperationTest {
    @Test void publishedUnlimitedRuleOpensReconnectSafeUntimedTurn() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
                Map.of("operationTimeoutSeconds", 0), PaoDeKuaiConfig.defaults());
        assertTrue(config.advancedRules().unlimitedOperationTime());
        assertEquals(0, PdkPublishedRuleOptions.snapshot(config).get("operationTimeoutSeconds"));
        PaoDeKuaiFamily family = new PaoDeKuaiFamily(config);
        PokerAuthoritativeSession room = new PokerAuthoritativeSession(2, 20, 2, 9, family);
        room.execute(request("join_req", "join", 1, "21", 1));
        room.execute(request("ready_req", "ready-owner", 2, "20", 0));
        room.execute(request("ready_req", "ready-guest", 3, "21", 1));

        OperationDeadline turn = room.operationDeadline();
        assertTrue(turn.open());
        assertTrue(turn.unlimited());
        assertFalse(room.tickLifecycle(Instant.parse("2099-01-01T00:00:00Z")));
        assertEquals(turn, room.operationDeadline());
        assertEquals(room.authoritativeState(),
                PokerAuthoritativeSession.restore(room.authoritativeState(), family).authoritativeState());
        assertEquals(turn,
                PokerAuthoritativeSession.restore(room.authoritativeState(), family).operationDeadline());

        int seat = ((Number) room.viewFor(20).get("currentSeat")).intValue();
        long player = 20L + seat;
        List<CardCombination> hints = PdkLegalMoves.hints(room);
        assertFalse(hints.isEmpty());
        assertDoesNotThrow(() -> room.execute(request("play_req", "untimed-play", 4,
                String.valueOf(player), seat, Map.of("cards", hints.getFirst().cards()))));
    }

    @Test void onlyExplicitZeroEnablesUnlimitedRule() {
        assertEquals(20, PaoDeKuaiConfig.defaults().advancedRules().operationTimeoutSeconds());
        assertFalse(PaoDeKuaiConfig.defaults().advancedRules().unlimitedOperationTime());
        assertThrows(IllegalArgumentException.class, () -> PdkPublishedRuleOptions.apply(
                Map.of("operationTimeoutSeconds", -1), PaoDeKuaiConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> PdkPublishedRuleOptions.apply(
                Map.of("operationTimeoutSeconds", 86401), PaoDeKuaiConfig.defaults()));
    }

    private static GameCommandRequest request(String command, String requestId, long sequence,
            String playerId, int seat) {
        return request(command, requestId, sequence, playerId, seat, Map.of());
    }

    private static GameCommandRequest request(String command, String requestId, long sequence,
            String playerId, int seat, Map<String,Object> payload) {
        return new GameCommandRequest(command, requestId, sequence, 2, 1, "pdk-v1",
                playerId, seat, payload);
    }
}
