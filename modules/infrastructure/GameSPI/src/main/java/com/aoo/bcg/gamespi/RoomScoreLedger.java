package com.aoo.bcg.gamespi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Server-owned score boundary for game sessions. The adapter resolves the actual
 * account scope from {@code roomId}; neither clients nor games select a currency.
 */
public interface RoomScoreLedger {
    Snapshot snapshot(long roomId, Collection<Long> playerIds);

    Receipt commit(Command command);

    record Snapshot(long roomId, long revision, Map<Long, Long> balances) {
        public Snapshot {
            if (roomId <= 0 || revision < 0) throw new IllegalArgumentException("invalid score snapshot identity");
            balances = immutableBalances(balances);
        }
    }

    record Command(String operationId, long roomId, int roundNo, long expectedRevision,
                   Map<Long, Long> expectedBalances, Map<Long, Long> deltas) {
        public Command {
            if (operationId == null || operationId.isBlank() || operationId.length() > 128
                    || roomId <= 0 || roundNo <= 0 || expectedRevision < 0) {
                throw new IllegalArgumentException("invalid score settlement identity");
            }
            expectedBalances = immutableBalances(expectedBalances);
            deltas = immutableDeltas(deltas);
            if (!expectedBalances.keySet().equals(deltas.keySet())) {
                throw new IllegalArgumentException("score settlement participants differ from snapshot");
            }
            long total = deltas.values().stream().reduce(0L, Math::addExact);
            if (total != 0) throw new IllegalArgumentException("score settlement must be zero-sum");
        }
    }

    record Receipt(String operationId, long roomId, int roundNo, long revision,
                   Map<Long, Long> balances, Map<Long, Long> deltas, boolean replayed) {
        public Receipt {
            if (operationId == null || operationId.isBlank() || roomId <= 0 || roundNo <= 0 || revision <= 0) {
                throw new IllegalArgumentException("invalid score settlement receipt");
            }
            balances = immutableBalances(balances);
            deltas = immutableDeltas(deltas);
        }
    }

    private static Map<Long, Long> immutableBalances(Map<Long, Long> source) {
        Objects.requireNonNull(source, "balances");
        Map<Long, Long> result = new LinkedHashMap<>();
        source.forEach((playerId, balance) -> {
            if (playerId == null || playerId <= 0 || balance == null || balance < 0) {
                throw new IllegalArgumentException("invalid player score balance");
            }
            if (result.putIfAbsent(playerId, balance) != null) throw new IllegalArgumentException("duplicate score player");
        });
        if (result.isEmpty()) throw new IllegalArgumentException("score participants are required");
        return Map.copyOf(result);
    }

    private static Map<Long, Long> immutableDeltas(Map<Long, Long> source) {
        Objects.requireNonNull(source, "deltas");
        Map<Long, Long> result = new LinkedHashMap<>();
        source.forEach((playerId, delta) -> {
            if (playerId == null || playerId <= 0 || delta == null) throw new IllegalArgumentException("invalid score delta");
            result.put(playerId, delta);
        });
        if (result.isEmpty()) throw new IllegalArgumentException("score deltas are required");
        return Map.copyOf(result);
    }
}
