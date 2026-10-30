package com.aoo.bcg.longcard;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LongCardCoreEngineTest {
    private static final LongCardRuleSet<Void> RULES = new LongCardRuleSet<>() {
        public Set<LongCardOperation> allowedOperations(int seat, Void ignored) { return Set.of(LongCardOperation.values()); }
        public boolean canWin(int seat, List<Integer> cards, int incoming, Void ignored) { return !cards.isEmpty() || incoming >= 0; }
    };

    @Test void executesRevealStealDiscardClaimCallAndWinWithoutMutatingSource() {
        LongCardState initial = new LongCardState(Map.of(0, List.of(1, 2), 1, List.of(3, 4)),
                List.of(5, 6), List.of(), 0, LongCardPhase.PLAYING, null, Set.of(), null);
        LongCardCoreEngine<Void> engine = new LongCardCoreEngine<>(RULES);
        LongCardState revealed = engine.apply(initial, LongCardCommand.of(LongCardOperation.REVEAL, 0), null);
        LongCardState stolen = engine.apply(revealed, LongCardCommand.of(LongCardOperation.STEAL, 0), null);
        LongCardState called = engine.apply(stolen, LongCardCommand.of(LongCardOperation.CALL, 0), null);
        LongCardState discarded = engine.apply(called, LongCardCommand.of(LongCardOperation.DISCARD, 0, 5), null);
        LongCardState claimed = engine.apply(discarded, LongCardCommand.of(LongCardOperation.CHI, 1, 3), null);
        LongCardState won = engine.apply(claimed, LongCardCommand.of(LongCardOperation.HU, 1), null);
        assertEquals(List.of(5, 6), initial.deck());
        assertTrue(called.calledSeats().contains(0));
        assertEquals(1, claimed.currentSeat());
        assertEquals(LongCardPhase.FINISHED, won.phase());
        assertEquals(1, won.winnerSeat());
    }

    @Test void rejectsWrongTurnAndUnownedDiscard() {
        LongCardState state = new LongCardState(Map.of(0, List.of(1), 1, List.of(2)), List.of(3),
                List.of(), 0, LongCardPhase.PLAYING, null, Set.of(), null);
        LongCardCoreEngine<Void> engine = new LongCardCoreEngine<>(RULES);
        assertThrows(IllegalStateException.class,
                () -> engine.apply(state, LongCardCommand.of(LongCardOperation.REVEAL, 1), null));
        assertThrows(IllegalArgumentException.class,
                () -> engine.apply(state, LongCardCommand.of(LongCardOperation.DISCARD, 0, 99), null));
    }

    @Test void resolvesSimultaneousResponsesByActionThenSeatDistance() {
        var winner=LongCardResponseResolver.resolve(List.of(
                new LongCardResponseResolver.Response(1,LongCardOperation.PENG),
                new LongCardResponseResolver.Response(3,LongCardOperation.HU),
                new LongCardResponseResolver.Response(2,LongCardOperation.HU)),0,4);
        assertEquals(2,winner.seatId());
        assertThrows(IllegalStateException.class,()->LongCardResponseResolver.resolve(
                List.of(new LongCardResponseResolver.Response(1,LongCardOperation.PASS)),0,4));
    }
}
