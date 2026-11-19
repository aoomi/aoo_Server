package business.global.pk.zjh;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ZjhStateMachineTest {
    @Test void blindLookBetCompareAndPotSettlementAreAuthoritative() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 1, "compareStartRound", 1, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(77, 1001, rules, 8);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 1001 + seat);
        table.start();
        int ownerSeat = table.seatOf(1001);
        assertEquals(4, table.pot());
        assertThrows(IllegalStateException.class, () -> table.look(ownerSeat));
        for (int turn = 0; turn < 4; turn++) table.bet(table.operatorSeat(), 1);
        assertEquals(2, table.bettingRound());
        table.look(ownerSeat);
        assertEquals(3, table.handView(1001, ownerSeat).size());
        assertEquals(java.util.List.of(0, 0, 0), table.handView(1002, ownerSeat));
        table.compare(ownerSeat, table.seatOf(1002));
        while (table.state() == ZjhTable.State.PLAYING) table.fold(table.operatorSeat());
        Map<String, Object> settlement = new ZjhSettlementService().settle(table, 1);
        assertEquals(table.winnerSeat(), settlement.get("winnerSeat"));
    }

    @Test void reconnectSnapshotRestoresRulesPotVisibilityAndVersion() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "mustBlindRounds", 0));
        ZjhTable table = new ZjhTable(88, 2001, rules, 9);
        table.sit(0, 2001); table.sit(1, 2002); table.start();
        int ownerSeat = table.seatOf(2001);
        table.look(ownerSeat); table.preBet(table.seatOf(2002), 2); table.bet(ownerSeat, 2);
        ZjhTable restored = ZjhTable.restore(88, table.authoritativeSnapshot());
        assertEquals(table.authoritativeSnapshot(), restored.authoritativeSnapshot());
        assertEquals(table.viewFor(2001), restored.viewFor(2001));
    }

    @Test void queuedPreBetExecutesOnTurnAndSettlementRejectsForgedRoundNumber() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(99, 3001, rules, 10);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 3001 + seat);
        table.start();
        int ownerSeat = table.seatOf(3001);
        int queuedSeat = nextSeat(table, ownerSeat);
        table.preBet(queuedSeat, 3);
        long versionBeforeBet = table.stateVersion();
        table.bet(ownerSeat, 2);
        assertEquals(9, table.pot());
        assertEquals(4, table.committedBet(queuedSeat));
        assertEquals(nextSeat(table, queuedSeat), table.operatorSeat());
        assertEquals(versionBeforeBet + 2, table.stateVersion());

        while (table.state() == ZjhTable.State.PLAYING) table.fold(table.operatorSeat());
        ZjhSettlementService settlement = new ZjhSettlementService();
        assertThrows(IllegalArgumentException.class, () -> settlement.settle(table, 2));
        Map<String, Object> first = settlement.settle(table, 1);
        Map<String, Object> duplicate = settlement.settle(table, 1);
        assertEquals(first, duplicate, "duplicate settle_req must be a pure idempotent read");
        assertEquals(1, first.get("roundNo"));
        assertEquals(false, first.get("final"));
    }

    @Test void middleSeatFoldAdvancesClockwiseInsteadOfJumpingToFirstSeat() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(100, 4001, rules, 11);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 4001 + seat);
        table.start();
        table.bet(table.seatOf(4001), 1);
        int foldedSeat = table.operatorSeat();
        int expectedNext = nextSeat(table, foldedSeat);
        table.fold(foldedSeat);
        assertEquals(expectedNext, table.operatorSeat());
    }

    @Test void currentCallRaisesBlindAndLookedMinimumsAndSurvivesReconnect() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(101, 5001, rules, 12);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 5001 + seat);
        table.start();
        int ownerSeat = table.seatOf(5001);
        table.bet(ownerSeat, 5);
        assertThrows(IllegalArgumentException.class, () -> table.bet(table.operatorSeat(), 4));
        for (int turn = 0; turn < 3; turn++) table.bet(table.operatorSeat(), 5);
        table.look(ownerSeat);
        assertThrows(IllegalArgumentException.class, () -> table.bet(ownerSeat, 9));
        table.bet(ownerSeat, 10);
        ZjhTable restored = ZjhTable.restore(101, table.authoritativeSnapshot());
        assertEquals(table.authoritativeSnapshot(), restored.authoritativeSnapshot());
        assertEquals(5, restored.viewFor(5002).get("minimumBet"));
        assertEquals(10, restored.viewFor(5001).get("minimumBet"));
    }

    @Test void rejectsStaleBetCompareAndInactiveSeatOperations() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "compareStartRound", 3, "baseBet", 1, "maximumBet", 10));
        ZjhTable table = new ZjhTable(102, 6001, rules, 13);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 6001 + seat);
        table.start();
        int ownerSeat = table.seatOf(6001);
        int memberSeat = table.seatOf(6002);
        assertThrows(IllegalStateException.class, () -> table.bet(memberSeat, 1));
        assertThrows(IllegalArgumentException.class, () -> table.bet(ownerSeat, 11));
        assertThrows(IllegalStateException.class, () -> table.compare(ownerSeat, memberSeat));
        table.fold(ownerSeat);
        assertThrows(IllegalStateException.class, () -> table.preBet(ownerSeat, 1));
        assertThrows(IllegalStateException.class, () -> table.compare(memberSeat, memberSeat));
    }

    @Test void clockwiseTurnWrapsAcrossLastSeatAndReconnectKeepsOperator() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(103, 7001, rules, 14);
        table.sit(0, 7001); table.sit(2, 7002); table.sit(5, 7003); table.sit(7, 7004);
        table.start();
        int firstSeat = table.seatOf(7001);
        for (int turn = 0; turn < 4; turn++) table.bet(table.operatorSeat(), 1);
        assertEquals(firstSeat, table.operatorSeat());
        assertEquals(2, table.bettingRound());
        ZjhTable restored = ZjhTable.restore(103, table.authoritativeSnapshot());
        assertEquals(firstSeat, restored.operatorSeat());
        assertEquals(table.viewFor(7001), restored.viewFor(7001));
    }

    @Test void nonSequentialRandomJoinsStillAdvanceByClockwiseSeatNumber() {
        boolean exercisedNonSequentialJoin = false;
        for (long seatSeed = 0; seatSeed < 64 && !exercisedNonSequentialJoin; seatSeed++) {
            ZjhTable table = new ZjhTable(106, 10001,
                    ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4)), 23, seatSeed);
            int[] joinedSeats = new int[4];
            for (int index = 0; index < joinedSeats.length; index++) {
                joinedSeats[index] = table.sit(10001L + index);
            }
            boolean nonSequential = false;
            for (int index = 1; index < joinedSeats.length; index++) {
                if (joinedSeats[index] < joinedSeats[index - 1]) nonSequential = true;
            }
            if (!nonSequential) continue;
            exercisedNonSequentialJoin = true;
            table.start();
            for (int turn = 0; turn < joinedSeats.length; turn++) {
                int actingSeat = table.operatorSeat();
                int expectedNext = nextSeat(table, actingSeat);
                table.bet(actingSeat, 1);
                assertEquals(expectedNext, table.operatorSeat());
            }
            assertEquals(2, table.bettingRound());
        }
        assertTrue(exercisedNonSequentialJoin, "test must exercise non-sequential random seating");
    }

    @Test void waitingSnapshotKeepsFirstTurnAndDealOrderWhenFirstJoinHasHigherSeat() {
        boolean exercisedHigherFirstJoin = false;
        for (long seatSeed = 0; seatSeed < 64 && !exercisedHigherFirstJoin; seatSeed++) {
            ZjhTable table = new ZjhTable(107, 11001,
                    ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4)), 29, seatSeed);
            int firstJoinSeat = table.sit(11001);
            for (long playerId = 11002; playerId <= 11004; playerId++) table.sit(playerId);
            int firstClockwiseSeat = ((Map<?, ?>) table.authoritativeState().get("seats"))
                    .keySet().stream().mapToInt(key -> ((Number) key).intValue()).min().orElseThrow();
            if (firstJoinSeat == firstClockwiseSeat) continue;
            exercisedHigherFirstJoin = true;

            ZjhTable restored = ZjhTable.restore(107, table.authoritativeSnapshot());
            table.start();
            restored.start();
            assertEquals(firstJoinSeat, table.operatorSeat());
            assertNotEquals(firstClockwiseSeat, table.operatorSeat());
            assertEquals(table.operatorSeat(), restored.operatorSeat());
            assertEquals(table.authoritativeState().get("seats"), restored.authoritativeState().get("seats"));

            for (int turn = 0; turn < 4; turn++) {
                int actingSeat = table.operatorSeat();
                int expectedNext = nextSeat(table, actingSeat);
                assertEquals(actingSeat, restored.operatorSeat());
                table.bet(actingSeat, 1);
                restored.bet(actingSeat, 1);
                assertEquals(expectedNext, table.operatorSeat());
                assertEquals(table.operatorSeat(), restored.operatorSeat());
            }
            assertEquals(2, table.bettingRound());
            assertEquals(table.bettingRound(), restored.bettingRound());
        }
        assertTrue(exercisedHigherFirstJoin, "test must exercise a first join above the lowest seat");
    }

    @Test void timeoutRejectsEarlyRequestThenFoldsAtAuthoritativeDeadline() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "operationSeconds", 10, "baseBet", 1, "maximumBet", 50));
        ZjhTable table = new ZjhTable(104, 8001, rules, 15);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 8001 + seat);
        table.start();
        int ownerSeat = table.seatOf(8001);
        int expectedNext = nextSeat(table, ownerSeat);
        long deadline = ((Number) table.viewFor(8001).get("operationDeadlineEpochMillis")).longValue();
        assertThrows(IllegalStateException.class, () -> table.timeout(ownerSeat, deadline - 1));
        table.timeout(ownerSeat, deadline);
        assertEquals(expectedNext, table.operatorSeat());
        @SuppressWarnings("unchecked") Map<Integer, Object> seats =
                (Map<Integer, Object>) table.viewFor(8001).get("seats");
        @SuppressWarnings("unchecked") Map<String, Object> folded = (Map<String, Object>) seats.get(ownerSeat);
        assertEquals(false, folded.get("active"));
    }

    @Test void finalRoundSettlementIsZeroSumAndMarkedFinal() {
        ZjhRules rules = ZjhRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "totalRounds", 10, "mustBlindRounds", 0, "baseBet", 2, "maximumBet", 50));
        ZjhTable table = new ZjhTable(105, 9001, rules, 16);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 9001 + seat);
        table.start();
        Map<Long, Long> expectedTotals = new java.util.LinkedHashMap<>();
        for (long playerId = 9001; playerId <= 9004; playerId++) expectedTotals.put(playerId, 0L);
        for (int round = 1; round <= 10; round++) {
            while (table.state() == ZjhTable.State.PLAYING) table.fold(table.operatorSeat());
            int winnerSeat = table.winnerSeat();
            long lossPerOpponent = Math.addExact((long) rules.baseBet(), table.winnerBonusPerOpponent());
            for (long playerId = 9001; playerId <= 9004; playerId++) {
                long delta = table.seatOf(playerId) == winnerSeat
                        ? Math.multiplyExact(3L, lossPerOpponent) : Math.negateExact(lossPerOpponent);
                expectedTotals.put(playerId, Math.addExact(expectedTotals.get(playerId), delta));
            }
            if (round < 10) table.continueRound();
        }
        assertEquals(ZjhTable.State.FINISHED, table.state());
        Map<String, Object> settlement = new ZjhSettlementService().settle(table, 10);
        assertEquals(true, settlement.get("final"));
        @SuppressWarnings("unchecked") java.util.List<com.aoo.bcg.common.settlement.SettlementEntry> entries =
                (java.util.List<com.aoo.bcg.common.settlement.SettlementEntry>) settlement.get("entries");
        assertEquals(4, entries.size());
        assertEquals(0L, entries.stream().mapToLong(
                com.aoo.bcg.common.settlement.SettlementEntry::scoreDelta).sum());
        @SuppressWarnings("unchecked") java.util.List<com.aoo.bcg.common.settlement.SettlementEntry> cumulativeEntries =
                (java.util.List<com.aoo.bcg.common.settlement.SettlementEntry>) settlement.get("cumulativeEntries");
        assertEquals(4, cumulativeEntries.size());
        assertEquals(0L, cumulativeEntries.stream().mapToLong(
                com.aoo.bcg.common.settlement.SettlementEntry::scoreDelta).sum());
        assertNotEquals(entries, cumulativeEntries, "final settlement must contain all-round totals");
        assertEquals(expectedTotals,
                cumulativeEntries.stream().collect(java.util.stream.Collectors.toMap(
                        com.aoo.bcg.common.settlement.SettlementEntry::playerId,
                        com.aoo.bcg.common.settlement.SettlementEntry::scoreDelta)));
        assertEquals(settlement, new ZjhSettlementService().settle(table, 10),
                "duplicate final settle_req must not accumulate the round twice");
        ZjhTable restored = ZjhTable.restore(105, table.authoritativeSnapshot());
        assertEquals(table.cumulativeScoreDeltas(), restored.cumulativeScoreDeltas());
        assertEquals(settlement, new ZjhSettlementService().settle(restored, 10));
    }

    private static int nextSeat(ZjhTable table, int previousSeat) {
        Map<?, ?> seats = (Map<?, ?>) table.authoritativeState().get("seats");
        int first = Integer.MAX_VALUE;
        int next = Integer.MAX_VALUE;
        for (Object key : seats.keySet()) {
            int seat = ((Number) key).intValue();
            first = Math.min(first, seat);
            if (seat > previousSeat) next = Math.min(next, seat);
        }
        return next == Integer.MAX_VALUE ? first : next;
    }
}
