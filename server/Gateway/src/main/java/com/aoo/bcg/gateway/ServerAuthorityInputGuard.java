package com.aoo.bcg.gateway;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rejects client attempts to submit server-owned state through the canonical game transport. */
final class ServerAuthorityInputGuard {
    private static final Set<String> SERVER_OWNED = Set.of(
            "hands", "handcards", "privatecards", "holecards", "wall", "deck", "seed",
            "balance", "balances", "scoredelta", "settlement", "settlementresult",
            "winnerseat", "currentseat", "authoritativestate", "serverseq", "stateversion");

    private ServerAuthorityInputGuard() { }

    static void validate(String msgId, Map<String,Object> body, long currentStateVersion) {
        if (currentStateVersion < 0) throw new IllegalStateException("negative server stateVersion");
        Object expected = body.get("expectedStateVersion");
        if ("game.action".equals(msgId) && !(expected instanceof Number))
            throw new SecurityException("expectedStateVersion is required");
        if (expected instanceof Number number && number.longValue() != currentStateVersion)
            throw new SecurityException("stale stateVersion");
        rejectServerOwned(body);
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
