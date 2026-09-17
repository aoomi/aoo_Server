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
        assertDoesNotThrow(() -> settlement.settle(table, 1));
    }
}
