package business.global.pk.zjh;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZJHGameLogicTest {
    private static ArrayList<Integer> cards(int... values) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int value : values) result.add(value);
        return result;
    }

    @Test void recognizesEveryLegacyHandType() {
        assertEquals(ZJHGameLogic.ZJH_VALUE, ZJHGameLogic.GetCardType(cards(0x02, 0x14, 0x27)));
        assertEquals(ZJHGameLogic.ZJH_DUIZI, ZJHGameLogic.GetCardType(cards(0x03, 0x13, 0x25)));
        assertEquals(ZJHGameLogic.ZJH_SHUNZI, ZJHGameLogic.GetCardType(cards(0x03, 0x14, 0x25)));
        assertEquals(ZJHGameLogic.ZJH_JINHUA, ZJHGameLogic.GetCardType(cards(0x03, 0x05, 0x07)));
        assertEquals(ZJHGameLogic.ZJH_SHUNJIN, ZJHGameLogic.GetCardType(cards(0x03, 0x04, 0x05)));
        assertEquals(ZJHGameLogic.ZJH_PAOZI, ZJHGameLogic.GetCardType(cards(0x03, 0x13, 0x23)));
        assertEquals(ZJHGameLogic.ZJH_TESHU, ZJHGameLogic.GetCardType(cards(0x02, 0x13, 0x25)));
    }

    @Test void comparesByLegacyRank() {
        assertTrue(ZJHGameLogic.CompareCard(cards(0x03, 0x13, 0x23), cards(0x03, 0x14, 0x25)));
        assertFalse(ZJHGameLogic.CompareCard(cards(0x03, 0x14, 0x25), cards(0x03, 0x13, 0x23)));
    }

    @Test void dealsWithoutDuplicates() {
        ZJHSetCard deck = new ZJHSetCard(null);
        List<Integer> dealt = deck.popList(3);
        assertEquals(3, dealt.size());
        assertEquals(3, dealt.stream().distinct().count());
        assertEquals(49, deck.leftCards.size());
    }
}
