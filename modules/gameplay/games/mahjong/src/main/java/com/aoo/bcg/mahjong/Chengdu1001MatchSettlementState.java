package com.aoo.bcg.mahjong;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-authoritative, replay-safe match totals for the Chengdu1001 room lifecycle. */
final class Chengdu1001MatchSettlementState {
    private final int totalRounds;
    private final Map<Integer, RoundSettlement> rounds = new LinkedHashMap<>();
    private final Map<Long, Long> totalScore = new LinkedHashMap<>();

    Chengdu1001MatchSettlementState(int totalRounds) {
        if (totalRounds <= 0) throw new IllegalArgumentException("chengdu1001 totalRounds must be positive");
        this.totalRounds = totalRounds;
    }

    boolean record(long roomId, int roundNo, String operationId, Map<Long, Long> scoreDelta) {
        if (roomId <= 0 || roundNo <= 0 || operationId == null || operationId.isBlank()
                || scoreDelta == null) {
            throw new IllegalArgumentException("invalid chengdu1001 round settlement identity");
        }
        RoundSettlement requested = new RoundSettlement(roomId, roundNo, operationId, scoreDelta);
        RoundSettlement existing = rounds.get(roundNo);
        if (existing != null) {
            if (!existing.equals(requested)) throw new IllegalStateException("conflicting Chengdu1001 round settlement");
            return false;
        }
        if (roundNo != rounds.size() + 1 || roundNo > totalRounds) {
            throw new IllegalStateException("non-sequential Chengdu1001 round settlement");
        }
        long balance = scoreDelta.values().stream().mapToLong(Long::longValue).sum();
        if (balance != 0) throw new IllegalArgumentException("chengdu1001 settlement must be zero-sum");
        rounds.put(roundNo, requested);
        scoreDelta.forEach((playerId, delta) -> totalScore.merge(playerId, delta, Math::addExact));
        return true;
    }

    int roundNo() { return rounds.size(); }
    int totalRounds() { return totalRounds; }
    boolean roundScored() { return !rounds.isEmpty(); }
    boolean roomFinished() { return rounds.size() == totalRounds; }
    Map<Long, Long> totalScore() { return Map.copyOf(totalScore); }

    Map<Long, Long> roundScoreDelta(int roundNo) {
        RoundSettlement round = rounds.get(roundNo);
        if (round == null) throw new IllegalStateException("chengdu1001 round settlement unavailable");
        return round.scoreDelta();
    }

    Map<String, Object> snapshot() {
        Map<String, Object> encodedRounds = new LinkedHashMap<>();
        rounds.forEach((roundNo, round) -> encodedRounds.put(String.valueOf(roundNo), Map.of(
                "roomId", round.roomId(),
                "roundNo", round.roundNo(),
                "operationId", round.operationId(),
                "scoreDelta", round.scoreDelta())));
        return Map.of(
                "totalRounds", totalRounds,
                "rounds", Map.copyOf(encodedRounds),
                "totalScore", Map.copyOf(totalScore));
    }

    static Chengdu1001MatchSettlementState restore(Object raw) {
        if (!(raw instanceof Map<?, ?> source)) throw new IllegalArgumentException("chengdu1001 match settlement state required");
        Chengdu1001MatchSettlementState restored = new Chengdu1001MatchSettlementState(integer(source.get("totalRounds")));
        Object rawRounds = source.get("rounds");
        if (!(rawRounds instanceof Map<?, ?> encodedRounds)) throw new IllegalArgumentException("chengdu1001 rounds required");
        encodedRounds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((left, right) -> Integer.compare(integer(left), integer(right))))
                .forEach(entry -> {
                    if (!(entry.getValue() instanceof Map<?, ?> round)) throw new IllegalArgumentException("invalid chengdu1001 round");
                    restored.record(longValue(round.get("roomId")), integer(round.get("roundNo")),
                            String.valueOf(round.get("operationId")), longMap(round.get("scoreDelta")));
                });
        if (!restored.totalScore.equals(longMap(source.get("totalScore")))) {
            throw new IllegalArgumentException("chengdu1001 cumulative score mismatch");
        }
        return restored;
    }

    private record RoundSettlement(long roomId, int roundNo, String operationId, Map<Long, Long> scoreDelta) {
        private RoundSettlement {
            scoreDelta = Map.copyOf(scoreDelta);
        }
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private static long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private static Map<Long, Long> longMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) throw new IllegalArgumentException("chengdu1001 score map required");
        Map<Long, Long> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(longValue(key), longValue(item)));
        return Map.copyOf(result);
    }
}
