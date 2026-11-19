package com.aoo.bcg.common.settlement;

import java.util.HashSet;
import java.util.Set;

public final class SettlementValidator {
    private SettlementValidator() {}
    public static void validate(SettlementResult result, long expectedRoomId, int expectedRoundNo,
                                String expectedPlayVersion, boolean zeroSum) {
        validate(result, expectedRoomId, expectedRoundNo, expectedPlayVersion,
                zeroSum ? SettlementBalancePolicy.ZERO_SUM : SettlementBalancePolicy.NON_ZERO_SUM);
    }

    public static void validate(SettlementResult result, long expectedRoomId, int expectedRoundNo,
            String expectedPlayVersion, SettlementBalancePolicy balancePolicy) {
        if (result == null || expectedPlayVersion == null || balancePolicy == null)
            throw new IllegalArgumentException("settlement validation arguments are required");
        if (result.roomId() != expectedRoomId || result.roundNo() != expectedRoundNo || !result.playVersion().equals(expectedPlayVersion)) throw new IllegalStateException("settlement identity mismatch");
        Set<Long> players = new HashSet<>();
        long total = 0;
        for (SettlementEntry entry : result.entries()) {
            if (!players.add(entry.playerId())) throw new IllegalStateException("duplicate settlement player");
            total = Math.addExact(total, entry.scoreDelta());
            long componentTotal = entry.components().values().stream().reduce(0L, Math::addExact);
            if (componentTotal != entry.scoreDelta()) throw new IllegalStateException("settlement component mismatch");
        }
        if (balancePolicy.requiresZeroSum() && total != 0) throw new IllegalStateException("settlement is not zero-sum");
    }
}
