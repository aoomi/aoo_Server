package com.aoo.bcg.families;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OtherGameFoundationsTest {
    @Test void canonicalCardsRoundTripAndRejectCrossFamilyRanges() {
        for (CanonicalCard card:List.of(new CanonicalCard(CanonicalCard.Family.MAHJONG,3,7,2),new CanonicalCard(CanonicalCard.Family.POKER,4,14,0),new CanonicalCard(CanonicalCard.Family.LONG_CARD,2,10,3),new CanonicalCard(CanonicalCard.Family.WORD_CARD,1,10,3)))
            assertEquals(card,CanonicalCard.parse(card.value()));
        assertThrows(IllegalArgumentException.class,()->new CanonicalCard(CanonicalCard.Family.WORD_CARD,2,1,0));
    }
    @Test void scoreUnitsConvertExactlyAndRejectPrecisionLoss() {
        assertEquals(new ScoreAmount(45,ScoreAmount.Unit.TUN),new ScoreAmount(15,ScoreAmount.Unit.HU_XI).convertTo(ScoreAmount.Unit.TUN));
        assertEquals(new ScoreAmount(18,ScoreAmount.Unit.HU_XI),new ScoreAmount(15,ScoreAmount.Unit.HU_XI).add(new ScoreAmount(9,ScoreAmount.Unit.TUN)));
        assertThrows(ArithmeticException.class,()->new ScoreAmount(1,ScoreAmount.Unit.TUN).convertTo(ScoreAmount.Unit.HU_XI));
    }
    @Test void diceReplayAndBoardLegalityAreDeterministic() {
        var first=new DeterministicBoardRandom(42).roll(3,6);var replay=new DeterministicBoardRandom(first.before().seed(),first.before().cursor()).roll(3,6);
        assertEquals(first.faces(),replay.faces()); assertEquals(3,first.after().cursor());
        var board=new BoardPosition(10,java.util.Set.of(5),Map.of(7,1));assertEquals(4,board.move(7,3).pieces().get(7));
        assertThrows(IllegalStateException.class,()->board.move(7,4));
    }
    @Test void catalogsIndependentFamiliesAndReportsActionGaps() {
        assertEquals(GameFamilyCatalog.Category.DICE,GameFamilyCatalog.classify("dice-generic"));
        assertTrue(GameFamilyCatalog.independentFamilies().contains("party-generic"));
        var report=ActionCapabilityAudit.compare(List.of("draw","old-kong","hu"),List.of("DRAW","HU","TI"));
        assertEquals(java.util.Set.of("OLD_KONG"),report.unsupportedLegacy());assertEquals(java.util.Set.of("TI"),report.newOnly());
    }
}
