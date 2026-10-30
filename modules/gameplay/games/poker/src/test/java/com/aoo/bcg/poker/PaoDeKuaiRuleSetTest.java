package com.aoo.bcg.poker;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaoDeKuaiRuleSetTest {
    @Test void recognizesRegionalCombinationsAndGuardsFirstAndReportedSingle() {
        var rules = new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5, true, true, true, 103, true));
        assertEquals("CONSECUTIVE_PAIRS", rules.recognize(List.of(103, 203, 104, 204), null).type());
        assertEquals("AIRPLANE_WITH_SINGLES", rules.recognize(List.of(103, 203, 303, 104, 204, 304, 105, 106), null).type());
        assertEquals("FOUR_WITH_TWO", rules.recognize(List.of(107, 207, 307, 407, 108, 109), null).type());
        assertThrows(IllegalStateException.class,
                () -> rules.validatePlay(List.of(104), new PaoDeKuaiContext(true, 3, List.of(103, 104))));
        assertThrows(IllegalStateException.class,
                () -> rules.validatePlay(List.of(103), new PaoDeKuaiContext(false, 1, List.of(103, 114))));
        assertDoesNotThrow(
                () -> rules.validatePlay(List.of(114), new PaoDeKuaiContext(false, 1, List.of(103, 114))));
    }
}
