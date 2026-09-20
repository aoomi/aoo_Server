package com.aoo.bcg.gateway;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rejects client attempts to submit server-owned state through the canonical game transport. */
final class ServerAuthorityInputGuard {
    static final class StateVersionConflictException extends SecurityException {
        StateVersionConflictException(String message) { super(message); }
    }
    private static final Set<String> SERVER_OWNED = Set.of(
            "hands", "handcards", "privatecards", "holecards", "wall", "deck", "seed",
            "balance", "balances", "scoredelta", "settlement", "settlementresult",
            "winnerseat", "currentseat", "authoritativestate", "serverseq", "stateversion");

    private ServerAuthorityInputGuard() { }

    static void validate(String msgId, Map<String,Object> body, long currentStateVersion) {
        if (currentStateVersion < 0) throw new IllegalStateException("negative server stateVersion");
        Object expected = expectedStateVersion(body);
        if (("game.action".equals(msgId) || requiresTurnVersion(msgId)) && !(expected instanceof Number))
            throw new StateVersionConflictException("expectedStateVersion is required");
        // A newly loaded client has no authoritative version yet. State requests are authenticated,
        // read-only synchronization operations and must return the current snapshot instead of being
        // rejected by the optimistic write guard. Every mutating action remains version-gated.
        if (!isStateRequest(msgId, body)
                && expected instanceof Number number && number.longValue() != currentStateVersion)
            throw new StateVersionConflictException("stale stateVersion");
        rejectServerOwned(body);
    }

    private static Object expectedStateVersion(Map<String,Object> body) {
        Object direct = body.get("expectedStateVersion");
        if (direct != null) return direct;
        Object payload = body.get("payload");
        return payload instanceof Map<?,?> nested ? nested.get("expectedStateVersion") : null;
    }

    private static boolean requiresTurnVersion(String msgId) {
        String normalized = msgId.toLowerCase(Locale.ROOT);
        return normalized.equals("common.room.play_req") || normalized.equals("common.room.pass_req");
    }

    private static boolean isStateRequest(String msgId, Map<String,Object> body) {
        if (msgId.toLowerCase(Locale.ROOT).endsWith(".state_req")) return true;
        Object action = body.get("action");
        return action instanceof String text && text.toLowerCase(Locale.ROOT).endsWith(".state_req");
    }

    private static void rejectServerOwned(Object value) {
        if (value instanceof Map<?,?> map) {
            for (Map.Entry<?,?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
                if (SERVER_OWNED.contains(normalized))
                    throw new SecurityException("client supplied server-owned field: " + key);
                rejectServerOwned(entry.getValue());
            }
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(ServerAuthorityInputGuard::rejectServerOwned);
        }
    }
}
