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

    @Test void discardWithoutCandidateAdvancesToNextActiveSeat() {
        MahjongState initial = new MahjongState(List.of(8, 7), Map.of(0, List.of(1), 1, List.of(2)),
                0, false, 0, -1, MahjongOperationWindow.empty(), false, -1);
        MahjongCoreEngine engine = new MahjongCoreEngine();
        MahjongState drawn = engine.execute(initial, MahjongCommand.draw(0), rules);
        MahjongState discarded = engine.execute(drawn, MahjongCommand.discard(0, 8), rules);

        assertEquals(1, discarded.currentSeat());
        assertFalse(discarded.currentSeatHasDrawn());
        assertTrue(discarded.window().isEmpty());
        assertEquals(List.of(2, 7), engine.execute(discarded, MahjongCommand.draw(1), rules).hands().get(1));
    }

    @Test void chengdu1001AddedAndConcealedGangUseDifferentHandAndMeldTransitions() {
        MahjongCoreEngine engine = new MahjongCoreEngine();
        MahjongState peng = new MahjongState(List.of(31,32), Map.of(0,List.of(11,21),1,List.of(22)),
                0,true,11,1,MahjongOperationWindow.empty(),false,-1,
                Map.of(0,List.of(new MahjongState.Meld(MahjongOperation.PENG,List.of(11,11,11),1))),
                Set.of(),21,MahjongRegionalRules.OpeningSelections.empty());
        MahjongState added = engine.execute(peng,new MahjongCommand(0,MahjongOperation.GANG,11,List.of(),3),rules);
        assertEquals(List.of(21,32),added.hands().get(0));
        assertEquals(MahjongOperation.GANG,added.melds().get(0).getFirst().operation());
        assertEquals(1,added.melds().get(0).getFirst().sourceSeat());
        assertThrows(IllegalStateException.class,()->engine.execute(peng,new MahjongCommand(0,MahjongOperation.GANG,12,List.of(),3),rules));

        MahjongState four = new MahjongState(List.of(33,34),Map.of(0,List.of(15,15,15,15,21),1,List.of(22)),
                0,true,-1,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongState concealed = engine.execute(four,new MahjongCommand(0,MahjongOperation.GANG,15,List.of(),4),rules);
        assertEquals(List.of(21,34),concealed.hands().get(0));
        assertEquals(List.of(15,15,15,15),concealed.melds().get(0).getFirst().tiles());
        assertEquals(0,concealed.melds().get(0).getFirst().sourceSeat());
        assertThrows(IllegalStateException.class,()->engine.execute(four,new MahjongCommand(0,MahjongOperation.GANG,16,List.of(),4),rules));
    }

    @Test void discardGangRequiresWindowAndConsumesThreeMatchingTiles() {
        MahjongState state=new MahjongState(List.of(31,32),Map.of(0,List.of(11),1,List.of(11,11,11,22)),
                0,false,11,0,new MahjongOperationWindow(Map.of(1,Set.of(MahjongOperation.GANG,MahjongOperation.PASS))),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        MahjongState claimed=engine.execute(state,new MahjongCommand(1,MahjongOperation.GANG,11,List.of(11,11,11),2),rules);
        assertEquals(List.of(22,32),claimed.hands().get(1));
        assertEquals(List.of(11,11,11,11),claimed.melds().get(1).getFirst().tiles());
        assertThrows(IllegalStateException.class,()->engine.execute(state,
                new MahjongCommand(0,MahjongOperation.GANG,11,List.of(11,11,11),2),rules));
    }
}
