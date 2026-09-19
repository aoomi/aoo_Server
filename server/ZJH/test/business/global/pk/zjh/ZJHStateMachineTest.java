package business.global.pk.zjh;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ZJHStateMachineTest {
    @Test void blindLookBetCompareAndPotSettlementAreAuthoritative() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 1, "compareStartRound", 1, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(77, 1001, rules, 8);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 1001 + seat);
        table.start();
        assertEquals(4, table.pot());
        assertThrows(IllegalStateException.class, () -> table.look(0));
        table.bet(0, 1); table.bet(1, 1); table.bet(2, 1); table.bet(3, 1);
        assertEquals(2, table.bettingRound());
        table.look(0);
        assertEquals(3, table.handView(1001, 0).size());
        assertEquals(java.util.List.of(0, 0, 0), table.handView(1002, 0));
        table.compare(0, 1);
        while (table.state() == ZJHTable.State.PLAYING) table.fold(table.operatorSeat());
        Map<String, Object> settlement = new ZJHSettlementService().settle(table, 1);
        assertEquals(table.winnerSeat(), settlement.get("winnerSeat"));
    }

    @Test void reconnectSnapshotRestoresRulesPotVisibilityAndVersion() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "mustBlindRounds", 0));
        ZJHTable table = new ZJHTable(88, 2001, rules, 9);
        table.sit(0, 2001); table.sit(1, 2002); table.start();
        table.look(0); table.preBet(1, 2); table.bet(0, 2);
        ZJHTable restored = ZJHTable.restore(88, table.authoritativeSnapshot());
        assertEquals(table.authoritativeSnapshot(), restored.authoritativeSnapshot());
        assertEquals(table.viewFor(2001), restored.viewFor(2001));
    }

    @Test void queuedPreBetExecutesOnTurnAndSettlementRejectsForgedRoundNumber() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(99, 3001, rules, 10);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 3001 + seat);
        table.start();
        table.preBet(1, 3);
        long versionBeforeBet = table.stateVersion();
        table.bet(0, 2);
        assertEquals(9, table.pot());
        assertEquals(4, table.committedBet(1));
        assertEquals(2, table.operatorSeat());
        assertEquals(versionBeforeBet + 2, table.stateVersion());

        table.fold(2);
        table.fold(3);
        table.fold(0);
        ZJHSettlementService settlement = new ZJHSettlementService();
        assertThrows(IllegalArgumentException.class, () -> settlement.settle(table, 2));
        Map<String, Object> first = settlement.settle(table, 1);
        Map<String, Object> duplicate = settlement.settle(table, 1);
        assertEquals(first, duplicate, "duplicate settle_req must be a pure idempotent read");
        assertEquals(1, first.get("roundNo"));
        assertEquals(false, first.get("final"));
    }

    @Test void middleSeatFoldAdvancesClockwiseInsteadOfJumpingToFirstSeat() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(100, 4001, rules, 11);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 4001 + seat);
        table.start();
        table.bet(0, 1);
        table.fold(1);
        assertEquals(2, table.operatorSeat());
    }

    @Test void currentCallRaisesBlindAndLookedMinimumsAndSurvivesReconnect() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(101, 5001, rules, 12);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 5001 + seat);
        table.start();
        table.bet(0, 5);
        assertThrows(IllegalArgumentException.class, () -> table.bet(1, 4));
        table.bet(1, 5); table.bet(2, 5); table.bet(3, 5);
        table.look(0);
        assertThrows(IllegalArgumentException.class, () -> table.bet(0, 9));
        table.bet(0, 10);
        ZJHTable restored = ZJHTable.restore(101, table.authoritativeSnapshot());
        assertEquals(table.authoritativeSnapshot(), restored.authoritativeSnapshot());
        assertEquals(5, restored.viewFor(5002).get("minimumBet"));
        assertEquals(10, restored.viewFor(5001).get("minimumBet"));
    }

    @Test void rejectsStaleBetCompareAndInactiveSeatOperations() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "compareStartRound", 3, "baseBet", 1, "maximumBet", 10));
        ZJHTable table = new ZJHTable(102, 6001, rules, 13);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 6001 + seat);
        table.start();
        assertThrows(IllegalStateException.class, () -> table.bet(1, 1));
        assertThrows(IllegalArgumentException.class, () -> table.bet(0, 11));
        assertThrows(IllegalStateException.class, () -> table.compare(0, 1));
        table.fold(0);
        assertThrows(IllegalStateException.class, () -> table.preBet(0, 1));
        assertThrows(IllegalStateException.class, () -> table.compare(1, 1));
    }

    @Test void clockwiseTurnWrapsAcrossLastSeatAndReconnectKeepsOperator() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(103, 7001, rules, 14);
        table.sit(0, 7001); table.sit(2, 7002); table.sit(5, 7003); table.sit(7, 7004);
        table.start();
        table.bet(0, 1); table.bet(2, 1); table.bet(5, 1); table.bet(7, 1);
        assertEquals(0, table.operatorSeat());
        assertEquals(2, table.bettingRound());
        ZJHTable restored = ZJHTable.restore(103, table.authoritativeSnapshot());
        assertEquals(0, restored.operatorSeat());
        assertEquals(table.viewFor(7001), restored.viewFor(7001));
    }

    @Test void timeoutRejectsEarlyRequestThenFoldsAtAuthoritativeDeadline() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "mustBlindRounds", 0, "operationSeconds", 10, "baseBet", 1, "maximumBet", 50));
        ZJHTable table = new ZJHTable(104, 8001, rules, 15);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 8001 + seat);
        table.start();
        long deadline = ((Number) table.viewFor(8001).get("operationDeadlineEpochMillis")).longValue();
        assertThrows(IllegalStateException.class, () -> table.timeout(0, deadline - 1));
        table.timeout(0, deadline);
        assertEquals(1, table.operatorSeat());
        @SuppressWarnings("unchecked") Map<Integer, Object> seats =
                (Map<Integer, Object>) table.viewFor(8001).get("seats");
        @SuppressWarnings("unchecked") Map<String, Object> folded = (Map<String, Object>) seats.get(0);
        assertEquals(false, folded.get("active"));
    }

    @Test void finalRoundSettlementIsZeroSumAndMarkedFinal() {
        ZJHRules rules = ZJHRules.from(Map.of("seatLimit", 8, "minimumPlayers", 4,
                "totalRounds", 10, "mustBlindRounds", 0, "baseBet", 2, "maximumBet", 50));
        ZJHTable table = new ZJHTable(105, 9001, rules, 16);
        for (int seat = 0; seat < 4; seat++) table.sit(seat, 9001 + seat);
        table.start();
        for (int round = 1; round <= 10; round++) {
            table.fold(0); table.fold(1); table.fold(2);
            if (round < 10) table.continueRound();
        }
        assertEquals(ZJHTable.State.FINISHED, table.state());
        Map<String, Object> settlement = new ZJHSettlementService().settle(table, 10);
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
        assertEquals(Map.of(9001L, -20L, 9002L, -20L, 9003L, -20L, 9004L, 60L),
                cumulativeEntries.stream().collect(java.util.stream.Collectors.toMap(
                        com.aoo.bcg.common.settlement.SettlementEntry::playerId,
                        com.aoo.bcg.common.settlement.SettlementEntry::scoreDelta)));
        assertEquals(settlement, new ZJHSettlementService().settle(table, 10),
                "duplicate final settle_req must not accumulate the round twice");
        ZJHTable restored = ZJHTable.restore(105, table.authoritativeSnapshot());
        assertEquals(table.cumulativeScoreDeltas(), restored.cumulativeScoreDeltas());
        assertEquals(settlement, new ZJHSettlementService().settle(restored, 10));
    }
}
