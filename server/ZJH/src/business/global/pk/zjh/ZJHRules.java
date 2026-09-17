package business.global.pk.zjh;

import java.util.Map;

/** Immutable CN297 room rules. Standing is peripheral room management and is intentionally absent. */
public record ZJHRules(
        int seatLimit,
        int minimumPlayers,
        int totalRounds,
        int operationSeconds,
        int compareStartRound,
        int maximumBet,
        int mustBlindRounds,
        int baseBet,
        int aaaBonus,
        int leopardBonus,
        int straightFlushBonus) {

    public ZJHRules {
        if (seatLimit != 8 && seatLimit != 10) throw new IllegalArgumentException("seatLimit must be 8 or 10");
        if (!oneOf(minimumPlayers, 2, 4, 6) || minimumPlayers > seatLimit) throw new IllegalArgumentException("minimumPlayers must be 2, 4 or 6");
        if (!oneOf(totalRounds, 10, 20, 30)) throw new IllegalArgumentException("totalRounds must be 10, 20 or 30");
        if (!oneOf(operationSeconds, 10, 15, 20)) throw new IllegalArgumentException("operationSeconds must be 10, 15 or 20");
        if (!oneOf(compareStartRound, 1, 3, 5)) throw new IllegalArgumentException("compareStartRound must be 1, 3 or 5");
        if (!oneOf(maximumBet, 10, 20, 50)) throw new IllegalArgumentException("maximumBet must be 10, 20 or 50");
        if (!oneOf(mustBlindRounds, 0, 1, 2)) throw new IllegalArgumentException("mustBlindRounds must be 0, 1 or 2");
        if (!oneOf(baseBet, 1, 2, 5, 10) || maximumBet < baseBet) throw new IllegalArgumentException("invalid bet bounds");
        if (aaaBonus < 0 || leopardBonus < 0 || straightFlushBonus < 0) throw new IllegalArgumentException("bonuses must not be negative");
    }

    public static ZJHRules from(Map<String, Object> source) {
        return new ZJHRules(integer(source, "seatLimit", 8), integer(source, "minimumPlayers", 2),
                integer(source, "totalRounds", 10), integer(source, "operationSeconds", 10),
                integer(source, "compareStartRound", 5), integer(source, "maximumBet", 50),
                integer(source, "mustBlindRounds", 1), integer(source, "baseBet", 1),
                integer(source, "aaaBonus", 20), integer(source, "leopardBonus", 10),
                integer(source, "straightFlushBonus", 5));
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("seatLimit", seatLimit), Map.entry("minimumPlayers", minimumPlayers),
                Map.entry("totalRounds", totalRounds), Map.entry("operationSeconds", operationSeconds),
                Map.entry("compareStartRound", compareStartRound), Map.entry("maximumBet", maximumBet),
                Map.entry("mustBlindRounds", mustBlindRounds), Map.entry("baseBet", baseBet),
                Map.entry("aaaBonus", aaaBonus), Map.entry("leopardBonus", leopardBonus),
                Map.entry("straightFlushBonus", straightFlushBonus));
    }

    private static int integer(Map<String, Object> source, String key, int fallback) {
        Object value = source.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be numeric");
        double exact = number.doubleValue();
        int parsed = number.intValue();
        if (!Double.isFinite(exact) || exact != parsed) throw new IllegalArgumentException(key + " must be an integer");
        return parsed;
    }

    private static boolean oneOf(int value, int... allowed) {
        for (int candidate : allowed) if (value == candidate) return true;
        return false;
    }
}
