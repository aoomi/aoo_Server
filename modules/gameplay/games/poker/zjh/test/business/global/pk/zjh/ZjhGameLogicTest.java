package business.global.pk.zjh;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZjhGameLogicTest {
    private static ArrayList<Integer> cards(int... values) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int value : values) result.add(value);
        return result;
    }

    @Test void recognizesEveryPublishedZjhHandType() {
        assertEquals(ZjhGameLogic.ZJH_VALUE, ZjhGameLogic.GetCardType(cards(0x02, 0x14, 0x27)));
        assertEquals(ZjhGameLogic.ZJH_DUIZI, ZjhGameLogic.GetCardType(cards(0x03, 0x13, 0x25)));
        assertEquals(ZjhGameLogic.ZJH_SHUNZI, ZjhGameLogic.GetCardType(cards(0x03, 0x14, 0x25)));
        assertEquals(ZjhGameLogic.ZJH_JINHUA, ZjhGameLogic.GetCardType(cards(0x03, 0x05, 0x07)));
        assertEquals(ZjhGameLogic.ZJH_SHUNJIN, ZjhGameLogic.GetCardType(cards(0x03, 0x04, 0x05)));
        assertEquals(ZjhGameLogic.ZJH_PAOZI, ZjhGameLogic.GetCardType(cards(0x03, 0x13, 0x23)));
        assertEquals(ZjhGameLogic.ZJH_VALUE, ZjhGameLogic.GetCardType(cards(0x02, 0x13, 0x25)),
                "235 is an XQP optional rule that ZJH does not publish");
    }

    @Test void comparesByLegacyRank() {
        assertTrue(ZjhGameLogic.CompareCard(cards(0x03, 0x13, 0x23), cards(0x03, 0x14, 0x25)));
        assertFalse(ZjhGameLogic.CompareCard(cards(0x03, 0x14, 0x25), cards(0x03, 0x13, 0x23)));
    }

    @Test void sameTypeAndRanksUseXqpSuitOrder() {
        assertTrue(ZjhGameLogic.CompareCard(cards(0x32, 0x24, 0x16), cards(0x22, 0x14, 0x06)));
        assertFalse(ZjhGameLogic.CompareCard(cards(0x22, 0x14, 0x06), cards(0x32, 0x24, 0x16)));
        assertTrue(ZjhGameLogic.CompareCard(cards(0x32, 0x22, 0x14), cards(0x12, 0x02, 0x14)),
                "equal pair and kicker compare by XQP suit order");
    }

    @Test void aceTwoThreeIsTheSmallestPublishedStraight() {
        assertTrue(ZjhGameLogic.CompareCard(cards(0x02, 0x13, 0x24), cards(0x0E, 0x12, 0x23)));
        assertFalse(ZjhGameLogic.CompareCard(cards(0x0E, 0x12, 0x23), cards(0x02, 0x13, 0x24)));
    }

    @Test void publishedHandHierarchyIsStrict() {
        ArrayList<Integer> high = cards(0x02, 0x14, 0x27);
        ArrayList<Integer> pair = cards(0x03, 0x13, 0x25);
        ArrayList<Integer> straight = cards(0x03, 0x14, 0x25);
        ArrayList<Integer> flush = cards(0x03, 0x05, 0x07);
        ArrayList<Integer> straightFlush = cards(0x03, 0x04, 0x05);
        ArrayList<Integer> leopard = cards(0x03, 0x13, 0x23);
        List<ArrayList<Integer>> ascending = List.of(high, pair, straight, flush, straightFlush, leopard);
        for (int i = 1; i < ascending.size(); i++) {
            assertTrue(ZjhGameLogic.CompareCard(ascending.get(i), ascending.get(i - 1)));
            assertFalse(ZjhGameLogic.CompareCard(ascending.get(i - 1), ascending.get(i)));
        }
    }

    @Test void dealsWithoutDuplicates() {
        ZjhSetCard deck = new ZjhSetCard(null);
        List<Integer> dealt = deck.popList(3);
        assertEquals(3, dealt.size());
        assertEquals(3, dealt.stream().distinct().count());
        assertEquals(49, deck.leftCards.size());
    }
}
