package business.global.pk.zjh;

import com.aoo.bcg.common.settlement.SettlementEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** XQP X01-X43 scoring-equivalence matrix under the published ZJH parameters. */
final class ZjhScoreEquivalenceTest {
    private static final ZjhRules RULES = ZjhRules.from(Map.ofEntries(
            Map.entry("seatLimit", 8), Map.entry("minimumPlayers", 2), Map.entry("totalRounds", 10),
            Map.entry("operationSeconds", 10), Map.entry("compareStartRound", 1), Map.entry("maximumBet", 50),
            Map.entry("mustBlindRounds", 0), Map.entry("baseBet", 1), Map.entry("aaaBonus", 20),
            Map.entry("leopardBonus", 10), Map.entry("straightFlushBonus", 5)));

    @Test void x01_singleTypeHierarchy() { // X01, D12
        assertEquals(ZjhGameLogic.ZJH_VALUE, type(0x02, 0x14, 0x27));
    }

    @Test void x02_pairDetected() { // X02
        assertEquals(ZjhGameLogic.ZJH_DUIZI, type(0x03, 0x13, 0x25));
    }

    @Test void x03_straightDetected_qkaHighest() { // X03
        assertEquals(ZjhGameLogic.ZJH_SHUNZI, type(0x0C, 0x1D, 0x2E));
        assertTrue(compare(cards(0x0C, 0x1D, 0x2E), cards(0x0B, 0x1C, 0x2D)));
    }

    @Test void x04_flushDetected() { // X04
        assertEquals(ZjhGameLogic.ZJH_JINHUA, type(0x03, 0x05, 0x07));
    }

    @Test void x05_straightFlushDetected() { // X05
        assertEquals(ZjhGameLogic.ZJH_SHUNJIN, type(0x03, 0x04, 0x05));
    }

    @Test void x06_leopardDetected() { // X06
        assertEquals(ZjhGameLogic.ZJH_PAOZI, type(0x03, 0x13, 0x23));
    }

    @Test void x07_a23_isLowestStraight() { // X07
        assertEquals(ZjhGameLogic.ZJH_SHUNZI, type(0x0E, 0x12, 0x23));
        assertFalse(compare(cards(0x0E, 0x12, 0x23), cards(0x02, 0x13, 0x24)));
    }

    @Test void x08_typeOrder_leopardSfFlushPairSingle() { // X08
        List<ArrayList<Integer>> ascending = List.of(cards(0x02, 0x14, 0x27), cards(0x03, 0x13, 0x25),
                cards(0x03, 0x14, 0x25), cards(0x03, 0x05, 0x07), cards(0x03, 0x04, 0x05),
                cards(0x03, 0x13, 0x23));
        for (int i = 1; i < ascending.size(); i++) assertTrue(compare(ascending.get(i), ascending.get(i - 1)));
    }

    @Test void x09_sameType_pointwiseCompare() { // X09
        assertTrue(compare(cards(0x0D, 0x19, 0x27), cards(0x0C, 0x1A, 0x27)));
    }

    @Test void x10_suitTieBreak_spadeToDiamond() { // X10
        assertTrue(compare(cards(0x32, 0x24, 0x16), cards(0x22, 0x14, 0x06)));
    }

    @Test void x11_identicalHands_returnTie() { // X11: boolean facade exposes tie as neither side winning
        ArrayList<Integer> hand = cards(0x02, 0x14, 0x27);
        assertFalse(compare(hand, new ArrayList<>(hand)));
    }

    @Test void x12_tie_compareInitiatorLoses() { // X12, D12
        ZjhTable table = playingWithHands(cards(0x02, 0x14, 0x27), cards(0x02, 0x14, 0x27));
        int initiator = table.operatorSeat();
        assertEquals(initiator, table.compare(initiator, otherSeat(table, initiator)));
    }

    @Test void x13_compareInitiatorCost() { // X13, D01: XQP cost = one current minimum bet
        ZjhTable table = started(RULES, 2);
        int initiator = table.operatorSeat();
        long before = table.committedBet(initiator);
        table.compare(initiator, otherSeat(table, initiator));
        assertEquals(1L, table.committedBet(initiator) - before);
    }

    @Test void x14_comparePotContribution() { // X14, D01
        ZjhTable table = started(RULES, 2);
        long before = table.pot();
        table.compare(table.operatorSeat(), otherSeat(table, table.operatorSeat()));
        assertEquals(1L, table.pot() - before);
    }

    @Test void x15_compareLoserDropped() { // X15
        ZjhTable table = started(RULES, 2);
        int loser = table.compare(table.operatorSeat(), otherSeat(table, table.operatorSeat()));
        assertEquals(false, seatState(table, loser).get("active"));
        assertEquals(true, seatState(table, loser).get("comparedOut"));
    }

    @Test void x16_lookedSeatCompareCost() { // X16, D04: MENRATE=200 => looked minimum is twice blind
        ZjhTable table = started(RULES, 2);
        int initiator = table.operatorSeat();
        table.look(initiator);
        long before = table.committedBet(initiator);
        table.compare(initiator, otherSeat(table, initiator));
        assertEquals(2L, table.committedBet(initiator) - before);
    }

    @Test void x17_compareStartRoundGate() { // X17, D05/D10
        ZjhRules rules = rules(Map.of("compareStartRound", 3));
        ZjhTable table = started(rules, 2);
        assertThrows(IllegalStateException.class,
                () -> table.compare(table.operatorSeat(), otherSeat(table, table.operatorSeat())));
    }

    @Test void x18_blindFollowCost() { // X18
        ZjhTable table = started(RULES, 3);
        int seat = table.operatorSeat();
        long before = table.committedBet(seat);
        table.bet(seat, 1);
        assertEquals(1L, table.committedBet(seat) - before);
    }

    @Test void x19_lookedFollowCostDoubles() { // X19, D04
        ZjhTable table = started(RULES, 3);
        int seat = table.operatorSeat();
        table.look(seat);
        long before = table.committedBet(seat);
        table.bet(seat, 2);
        assertEquals(2L, table.committedBet(seat) - before);
    }

    @Test void x20_blindBetNormalizationIsInverseOfCost() { // X20, D04
        ZjhTable table = started(RULES, 3);
        int looked = table.operatorSeat();
        table.look(looked);
        table.bet(looked, 4);
        assertEquals(2, table.viewFor(9999).get("minimumBet"));
    }

    @Test void x21_mustBlindRoundBlocksLook() { // X21, D10
        ZjhTable table = started(rules(Map.of("mustBlindRounds", 1)), 2);
        assertThrows(IllegalStateException.class, () -> table.look(table.operatorSeat()));
    }

    @Test void x22_maximumBetAbsoluteCap() { // X22, D03
        ZjhTable table = started(RULES, 2);
        assertThrows(IllegalArgumentException.class, () -> table.bet(table.operatorSeat(), 51));
    }

    @Test void x23_raiseAmountValidation() { // X23
        ZjhTable table = started(RULES, 3);
        assertEquals(6L, table.bet(table.operatorSeat(), 3));
    }

    @Test void x24_preBetRecorded() { // X24
        ZjhTable table = started(RULES, 3);
        int queued = otherSeat(table, table.operatorSeat());
        table.preBet(queued, 1);
        assertEquals(1, seatState(table, queued).get("preBet"));
    }

    @Test void x25_preBetExpiryKeepsSeat() { // X25, D06: state-machine difference deliberately retained
        ZjhTable table = started(RULES, 3);
        int queued = otherSeat(table, table.operatorSeat());
        Map<String, Object> snapshot = mutableSnapshot(table);
        mutableSeat(snapshot, queued).put("looked", true);
        mutableSeat(snapshot, queued).put("preBet", 1);
        table = ZjhTable.restore(table.roomId(), snapshot);
        table.bet(table.operatorSeat(), 1);
        assertEquals(true, seatState(table, queued).get("active"));
        assertEquals(0, seatState(table, queued).get("preBet"));
        assertEquals(queued, table.operatorSeat());
    }

    @Test void x26_bonusTriggerTypeSet() { // X26, D02b
        assertEquals(5, finishedWithWinner(cards(0x03, 0x04, 0x05), cards(0x02, 0x14, 0x27)).winnerBonusPerOpponent());
        assertEquals(10, finishedWithWinner(cards(0x03, 0x13, 0x23), cards(0x02, 0x14, 0x27)).winnerBonusPerOpponent());
        assertEquals(0, finishedWithWinner(cards(0x03, 0x05, 0x07), cards(0x02, 0x14, 0x27)).winnerBonusPerOpponent());
    }

    @Test void x27_bonusUsesTopHandOnly() { // X27, D02b
        ZjhTable table = finishedWithWinner(cards(0x03, 0x04, 0x05), cards(0x0E, 0x1E, 0x2E));
        assertEquals(5, table.winnerBonusPerOpponent());
    }

    @Test void x28_bonusAmountEqualsConfiguredPoints() { // X28, D02
        assertEquals(20, RULES.aaaBonus());
        assertEquals(10, RULES.leopardBonus());
        assertEquals(5, RULES.straightFlushBonus());
    }

    @Test void x29_bonusPayersIncludeFoldedSeats() { // X29, D09
        ZjhTable table = finishedWithWinner(cards(0x0E, 0x1E, 0x2E), cards(0x02, 0x14, 0x27));
        List<SettlementEntry> entries = entries(new ZjhSettlementService().settle(table, 1));
        SettlementEntry loser = entries.stream().filter(entry -> entry.playerId() != winnerPlayer(table)).findFirst().orElseThrow();
        assertEquals(-20L, loser.components().get("xi"));
    }

    @Test void x30_perPayerBonusDeltaSymmetric() { // X30, D09; no carry-balance model exists, so no clamp
        ZjhTable table = finishedWithWinner(cards(0x03, 0x13, 0x23), cards(0x02, 0x14, 0x27));
        List<SettlementEntry> entries = entries(new ZjhSettlementService().settle(table, 1));
        long xi = entries.stream().mapToLong(entry -> entry.components().get("xi")).sum();
        assertEquals(0L, xi);
    }

    @Test void x31_winnerTakesWholePot() { // X31
        ZjhTable table = started(RULES, 2);
        table.fold(table.operatorSeat());
        SettlementEntry winner = entries(new ZjhSettlementService().settle(table, 1)).stream()
                .filter(entry -> entry.playerId() == winnerPlayer(table)).findFirst().orElseThrow();
        assertEquals(table.pot() - table.committedBet(table.winnerSeat()) + table.winnerBonusPerOpponent(),
                winner.scoreDelta());
    }

    @Test void x32_settlementIsZeroSum() { // X32
        ZjhTable table = started(RULES, 3);
        while (table.state() == ZjhTable.State.PLAYING) table.fold(table.operatorSeat());
        assertEquals(0L, entries(new ZjhSettlementService().settle(table, 1)).stream()
                .mapToLong(SettlementEntry::scoreDelta).sum());
    }

    @Test void x33_participationSetAndPotIn() { // X33
        ZjhTable table = started(RULES, 3);
        assertEquals(3L, table.pot());
        assertEquals(3, table.committedBets().size());
    }

    @Test void x34_foldExcludesFromFurtherBets() { // X34
        ZjhTable table = started(RULES, 3);
        int folded = table.operatorSeat();
        table.fold(folded);
        assertThrows(IllegalStateException.class, () -> table.bet(folded, 1));
    }

    @Test void x35_timeoutFoldsSeat() { // X35, D07
        ZjhTable table = started(RULES, 3);
        int timedOut = table.operatorSeat();
        Map<String, Object> snapshot = mutableSnapshot(table);
        snapshot.put("operationDeadlineEpochMillis", 0L);
        table = ZjhTable.restore(table.roomId(), snapshot);
        table.timeout(timedOut, 0L);
        assertEquals(false, seatState(table, timedOut).get("active"));
    }

    @Test void x36_reconnectViewDeltaUnchanged() { // X36
        ZjhTable table = started(RULES, 3);
        table.bet(table.operatorSeat(), 2);
        ZjhTable restored = ZjhTable.restore(table.roomId(), table.authoritativeSnapshot());
        assertEquals(table.committedBets(), restored.committedBets());
        assertEquals(table.pot(), restored.pot());
    }

    @Test void x37_noHouseTaxInSettlementPayload() { // X37
        ZjhTable table = started(RULES, 2);
        table.fold(table.operatorSeat());
        Map<String, Object> result = new ZjhSettlementService().settle(table, 1);
        assertEquals(0L, entries(result).stream().mapToLong(SettlementEntry::scoreDelta).sum());
        assertFalse(result.containsKey("tax"));
    }

    @Test void x38_baseBetIsRoomUnit() { // X38
        ZjhTable table = started(rules(Map.of("baseBet", 5)), 2);
        assertEquals(10L, table.pot());
        assertEquals(5L, table.committedBet(table.operatorSeat()));
    }

    @Test void x39_cumulativeDeltaEqualsPerRoundSum() { // X39
        ZjhTable table = started(RULES, 3);
        while (table.state() == ZjhTable.State.PLAYING) table.fold(table.operatorSeat());
        Map<Long, Long> round = new LinkedHashMap<>();
        for (SettlementEntry entry : entries(new ZjhSettlementService().settle(table, 1))) round.put(entry.playerId(), entry.scoreDelta());
        assertEquals(round, table.cumulativeScoreDeltas());
    }

    @Test void x40_totalRoundsBoundary() { // X40
        ZjhTable table = started(RULES, 2);
        for (int round = 1; round <= RULES.totalRounds(); round++) {
            table.fold(table.operatorSeat());
            if (round < RULES.totalRounds()) table.continueRound();
        }
        assertEquals(ZjhTable.State.FINISHED, table.state());
        assertEquals(RULES.totalRounds(), table.roundNo());
    }

    @Test void x41_defaultFallbacksMatchRuleTable() { // X41
        ZjhRules defaults = ZjhRules.from(Map.of());
        assertEquals(8, defaults.seatLimit());
        assertEquals(2, defaults.minimumPlayers());
        assertEquals(10, defaults.totalRounds());
        assertEquals(10, defaults.operationSeconds());
        assertEquals(5, defaults.compareStartRound());
        assertEquals(50, defaults.maximumBet());
        assertEquals(1, defaults.mustBlindRounds());
        assertEquals(1, defaults.baseBet());
        assertEquals(20, defaults.aaaBonus());
        assertEquals(10, defaults.leopardBonus());
        assertEquals(5, defaults.straightFlushBonus());
    }

    @Test void x42_seededDealDeterministic() { // X42
        ZjhTable first = new ZjhTable(1L, 1L, RULES, 20260929L);
        first.sit(1001L);
        first.sit(1002L);
        ZjhTable second = ZjhTable.restore(1L, first.authoritativeSnapshot());

        first.start();
        second.start();

        assertEquals(handsBySeat(first), handsBySeat(second));
    }

    @Test void x43_weightDistributionSanity() { // X43
        ZjhTable table = new ZjhTable(1L, 1L, rules(Map.of("totalRounds", 30)), 20260929L);
        table.sit(1001L);
        table.sit(1002L);
        table.start();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        int dealtHands = 0;
        for (int round = 1; round <= 30; round++) {
            for (List<Integer> hand : handsBySeat(table).values()) {
                counts.merge(ZjhGameLogic.GetCardType(new ArrayList<>(hand)), 1, Integer::sum);
                dealtHands++;
            }
            table.fold(table.operatorSeat());
            if (round < 30) table.continueRound();
        }
        int mixed = counts.getOrDefault(ZjhGameLogic.ZJH_VALUE, 0);
        assertEquals(mixed, counts.values().stream().mapToInt(Integer::intValue).max().orElseThrow());
        int rare = counts.getOrDefault(ZjhGameLogic.ZJH_SHUNJIN, 0)
                + counts.getOrDefault(ZjhGameLogic.ZJH_PAOZI, 0);
        assertTrue(rare * 100 < dealtHands * 15);
    }

    private static ZjhRules rules(Map<String, Object> overrides) {
        Map<String, Object> values = new LinkedHashMap<>(RULES.toMap());
        values.putAll(overrides);
        return ZjhRules.from(values);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, List<Integer>> handsBySeat(ZjhTable table) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        ((Map<Integer, Object>) table.authoritativeSnapshot().get("seats")).forEach((seat, value) ->
                result.put(seat, (List<Integer>) ((Map<String, Object>) value).get("cards")));
        return result;
    }

    private static ZjhTable started(ZjhRules rules, int players) {
        ZjhTable table = new ZjhTable(3600 + players, 1001, rules, 36, 360036L + players);
        for (int index = 0; index < players; index++) table.sit(1001L + index);
        table.start();
        return table;
    }

    private static ZjhTable playingWithHands(ArrayList<Integer> first, ArrayList<Integer> second) {
        ZjhTable table = started(RULES, 2);
        int operator = table.operatorSeat();
        int other = otherSeat(table, operator);
        Map<String, Object> snapshot = mutableSnapshot(table);
        mutableSeat(snapshot, operator).put("cards", List.copyOf(first));
        mutableSeat(snapshot, other).put("cards", List.copyOf(second));
        return ZjhTable.restore(table.roomId(), snapshot);
    }

    private static ZjhTable finishedWithWinner(ArrayList<Integer> winnerHand, ArrayList<Integer> foldedHand) {
        ZjhTable table = started(RULES, 2);
        int winner = table.operatorSeat();
        int folded = otherSeat(table, winner);
        Map<String, Object> snapshot = mutableSnapshot(table);
        snapshot.put("state", ZjhTable.State.ROUND_FINISHED.name());
        snapshot.put("operatorSeat", winner);
        mutableSeat(snapshot, winner).put("cards", List.copyOf(winnerHand));
        mutableSeat(snapshot, winner).put("active", true);
        mutableSeat(snapshot, folded).put("cards", List.copyOf(foldedHand));
        mutableSeat(snapshot, folded).put("active", false);
        return ZjhTable.restore(table.roomId(), snapshot);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableSnapshot(ZjhTable table) {
        Map<String, Object> snapshot = new LinkedHashMap<>(table.authoritativeSnapshot());
        Map<Integer, Object> seats = new LinkedHashMap<>();
        ((Map<Integer, Object>) snapshot.get("seats")).forEach((seat, value) ->
                seats.put(seat, new LinkedHashMap<>((Map<String, Object>) value)));
        snapshot.put("seats", seats);
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableSeat(Map<String, Object> snapshot, int seat) {
        return (Map<String, Object>) ((Map<Integer, Object>) snapshot.get("seats")).get(seat);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> seatState(ZjhTable table, int seat) {
        return (Map<String, Object>) ((Map<Integer, Object>) table.authoritativeSnapshot().get("seats")).get(seat);
    }

    @SuppressWarnings("unchecked")
    private static List<SettlementEntry> entries(Map<String, Object> result) {
        return (List<SettlementEntry>) result.get("entries");
    }

    private static long winnerPlayer(ZjhTable table) {
        return ((Number) seatState(table, table.winnerSeat()).get("playerId")).longValue();
    }

    private static int otherSeat(ZjhTable table, int seat) {
        return table.committedBets().keySet().stream().filter(candidate -> candidate != seat).findFirst().orElseThrow();
    }

    private static int type(int... values) { return ZjhGameLogic.GetCardType(cards(values)); }
    private static boolean compare(ArrayList<Integer> left, ArrayList<Integer> right) {
        return ZjhGameLogic.CompareCard(left, right);
    }
    private static ArrayList<Integer> cards(int... values) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int value : values) result.add(value);
        return result;
    }
}
