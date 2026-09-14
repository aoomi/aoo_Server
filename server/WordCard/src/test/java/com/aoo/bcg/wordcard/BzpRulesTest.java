package com.aoo.bcg.wordcard;
import static org.junit.jupiter.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
class BzpRulesTest{
 @Test void portsLegacyCardTypesAndTwoCannotEnterStraight(){assertEquals(BzpRules.Type.SINGLE,BzpRules.classify(BzpRules.Type.SINGLE,List.of(0x35)).type());assertEquals(BzpRules.Type.PAIR,BzpRules.classify(BzpRules.Type.PAIR,List.of(0x35,0x45)).type());assertEquals(BzpRules.Type.TRIPLE_BOMB,BzpRules.classify(BzpRules.Type.TRIPLE_BOMB,List.of(0x35,0x45,0x25)).type());assertThrows(IllegalArgumentException.class,()->BzpRules.classify(BzpRules.Type.STRAIGHT,List.of(0x3d,0x31,0x32)));}
 @Test void portsChainsAndBombPrecedence(){var pairs=BzpRules.classify(BzpRules.Type.LINKED_PAIRS,List.of(0x35,0x45,0x36,0x46));var triple=BzpRules.classify(BzpRules.Type.TRIPLE_BOMB,List.of(0x37,0x47,0x27));assertFalse(BzpRules.beats(triple,pairs));var dou=BzpRules.classify(BzpRules.Type.DOU,List.of(0x35,0x45,0x25,0x15));assertTrue(BzpRules.beats(dou,pairs));}
 @Test void portsLegacyPrizeTables(){assertEquals(16,BzpRules.fourBombPrize(9));assertEquals(24,BzpRules.eightBombPrize(10));assertEquals(32,BzpRules.kingBombPrize(7));assertEquals(16,BzpRules.prize510k(5));assertEquals(25,List.of(0x35,0x3a,0x3d).stream().mapToInt(BzpRules::point510k).sum());}
}
