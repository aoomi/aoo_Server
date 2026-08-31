package com.aoo.bcg.mahjong;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MahjongCoreEngineTest {
    private final MahjongRuleSet<MahjongState> rules = new MahjongRuleSet<>() {
        public Set<MahjongOperation> allowedOperations(int seat, MahjongState state) {
            return seat == 1 && state.lastDiscard() == 9 ? Set.of(MahjongOperation.PENG, MahjongOperation.PASS) : Set.of();
        }
        public boolean canWin(int seat, List<Integer> hand, int tile, MahjongState state) { return tile == 9; }
    };

    @Test void drawDiscardCandidateAndPassAdvanceAreAuthoritative() {
        MahjongState initial = new MahjongState(List.of(9, 8), Map.of(0, List.of(1), 1, List.of(9, 9)),
                0, false, 0, -1, MahjongOperationWindow.empty(), false, -1);
        MahjongCoreEngine engine = new MahjongCoreEngine();
        MahjongState drawn = engine.execute(initial, MahjongCommand.draw(0), rules);
        assertEquals(List.of(1, 9), drawn.hands().get(0));
        MahjongState discarded = engine.execute(drawn, MahjongCommand.discard(0, 9), rules);
        assertTrue(discarded.window().allows(1, MahjongOperation.PENG));
        MahjongState passed = engine.execute(discarded,
                new MahjongCommand(1, MahjongOperation.PASS, 0, List.of()), rules);
        assertEquals(1, passed.currentSeat());
        assertTrue(passed.window().isEmpty());
        assertThrows(IllegalStateException.class, () -> engine.execute(initial, MahjongCommand.draw(1), rules));
    }
}
