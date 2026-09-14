package com.aoo.bcg.common.reconnect;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CardPerspectiveTest {
    @Test void masksOpponentCardsAndDetachesOwnerView() {
        List<Integer> cards = new ArrayList<>(List.of(17, 18, 19));
        List<Integer> opponent = CardPerspective.hand(cards, false);
        List<Integer> owner = CardPerspective.hand(cards, true);
        assertEquals(List.of(0, 0, 0), opponent);
        owner.clear();
        assertEquals(List.of(17, 18, 19), cards);
    }
}
