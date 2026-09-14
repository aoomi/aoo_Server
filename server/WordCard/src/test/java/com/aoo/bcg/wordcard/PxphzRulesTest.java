package com.aoo.bcg.wordcard;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class PxphzRulesTest{
 @Test void exactEightyCardDeckAndRegionalColors(){assertEquals(80,PxphzRules.deck().size());assertEquals(20,PxphzRules.deck().stream().map(PxphzRules::type).distinct().count());assertTrue(PxphzRules.red(21));assertEquals(PxphzRules.ColorWin.BLACK,PxphzRules.colorWin(List.of(11,31,41)));assertEquals(PxphzRules.ColorWin.RED,PxphzRules.colorWin(List.of(21,22,23,24,71,72,73,74,101,102)));}
 @Test void chiPassAndHuXiScoring(){List<Integer>hand=List.of(11,21,31,71,101);assertTrue(PxphzRules.legalChi(hand,21,Set.of(),false,false));assertFalse(PxphzRules.legalChi(hand,21,Set.of(2),false,false));assertFalse(PxphzRules.legalChi(hand,21,Set.of(),true,true));assertEquals(3,PxphzRules.meldHuXi(WordCardOperation.WEI,21));assertEquals(12,PxphzRules.meldHuXi(WordCardOperation.TI,1021));assertEquals(4,PxphzRules.winnerScore(18,PxphzRules.ColorWin.NONE,1,1,1));}
 @Test void validatesRoomOptions(){assertDoesNotThrow(()->new PxphzRules.Config(3,20,4,true,true,true,true));assertThrows(IllegalArgumentException.class,()->new PxphzRules.Config(4,0,0,false,false,false,false));}
}
