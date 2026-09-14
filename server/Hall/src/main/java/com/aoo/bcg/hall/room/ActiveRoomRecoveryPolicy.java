package com.aoo.bcg.hall.room;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Server-owned definition of a room that may be restored after authentication. */
final class ActiveRoomRecoveryPolicy {
    private static final Set<String> RECOVERABLE_PHASES = Set.of(
            "OPEN", "LOBBY", "WAITING", "WAITING_EX", "PREPARING", "DEALING",
            "PLAYING", "IN_GAME", "ROUND_PLAYING", "OPERATING", "RESOLVING",
            "PAUSED", "INTER_ROUND", "ROUND_SETTLEMENT");

    private ActiveRoomRecoveryPolicy() {}

    static boolean isRecoverable(String hallState, String authorityState, Map<String, Object> snapshot) {
        if (!Set.of("OPEN", "PLAYING").contains(normalize(hallState))
                || !"ACTIVE".equals(normalize(authorityState))) return false;
        if (snapshot == null || snapshot.isEmpty()) return "OPEN".equals(normalize(hallState));
        if (truthy(snapshot.get("roomTerminal")) || truthy(snapshot.get("dissolved"))
                || settlementPresent(snapshot.get("finalSettlement"))
                || settlementPresent(snapshot.get("bigSettlement"))) return false;
        if (isFinalRoundSettlement(snapshot)) return false;
        if (isRoundSettlement(snapshot)) return true;
        if (isStructuredAuthoritySnapshot(snapshot)) return true;
        return RECOVERABLE_PHASES.contains(normalize(snapshot.get("phase")))
                || RECOVERABLE_PHASES.contains(normalize(snapshot.get("state")));
    }

    private static boolean isStructuredAuthoritySnapshot(Map<String, Object> snapshot) {
        Object players = snapshot.get("players");
        Object state = snapshot.get("state");
        if (!(players instanceof Map<?, ?> playerMap) || playerMap.isEmpty()) return false;
        if (!(state instanceof Map<?, ?>)) return false;
        long roundNo = number(snapshot.get("roundNo"));
        long roundLimit = number(snapshot.get("roundLimit"));
        return roundNo >= 0 && (roundLimit <= 0 || roundNo <= roundLimit);
    }

    private static boolean isRoundSettlement(Map<String, Object> snapshot) {
        Object state = snapshot.get("state");
        if (!(state instanceof Map<?, ?> roundState) || !truthy(roundState.get("finished"))) return false;
        long roundNo = number(snapshot.get("roundNo"));
        long roundLimit = number(snapshot.get("roundLimit"));
        return roundNo > 0 && (roundLimit <= 0 || roundNo < roundLimit);
    }

    private static boolean isFinalRoundSettlement(Map<String, Object> snapshot) {
        Object state = snapshot.get("state");
        if (!(state instanceof Map<?, ?> roundState) || !truthy(roundState.get("finished"))) return false;
        long roundNo = number(snapshot.get("roundNo"));
        long roundLimit = number(snapshot.get("roundLimit"));
        return truthy(snapshot.get("roundScored")) && roundLimit > 0 && roundNo >= roundLimit;
    }

    private static long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(normalize(value)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).strip().toUpperCase(Locale.ROOT);
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value) || "TRUE".equals(normalize(value)) || "1".equals(normalize(value));
    }

    private static boolean settlementPresent(Object value) {
        return value != null && !Boolean.FALSE.equals(value) && !"FALSE".equals(normalize(value))
                && !"0".equals(normalize(value)) && !"".equals(normalize(value));
    }
}
