package com.aoo.bcg.longcard;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BuiltinLongCardFamiliesTest {
    @Test void registersRegionalModulesAndExcludesSharedCpBase() {
        assertEquals(Set.of("AYCP", "AYDSS", "PCDSS", "ZGCP", "ZGDSS"),
                BuiltinLongCardFamilies.all().keySet());
        assertThrows(IllegalArgumentException.class,()->BuiltinLongCardFamilies.require("CP"));
    }

    @Test void isolatesChePaiAndDaSiShiActionsAndWinningThresholds() {
        RegionalLongCardFamily che = BuiltinLongCardFamilies.require("AYCP");
        RegionalLongCardFamily dss = BuiltinLongCardFamilies.require("AYDSS");
        assertTrue(che.ruleSet().allowedOperations(0, new LongCardRuleContext(0)).contains(LongCardOperation.STEAL));
        assertFalse(dss.ruleSet().allowedOperations(0, new LongCardRuleContext(0)).contains(LongCardOperation.STEAL));
        assertTrue(che.ruleSet().canWin(0, java.util.List.of(), -1, new LongCardRuleContext(10)));
        assertFalse(dss.ruleSet().canWin(0, java.util.List.of(), -1, new LongCardRuleContext(10)));
        assertTrue(dss.ruleSet().canWin(0, java.util.List.of(), -1, new LongCardRuleContext(15)));
        assertFalse(BuiltinLongCardFamilies.require("ZGCP").ruleSet().canWin(0,java.util.List.of(),-1,new LongCardRuleContext(13)));
        assertTrue(BuiltinLongCardFamilies.require("ZGDSS").ruleSet().allowedOperations(0,new LongCardRuleContext(1)).contains(LongCardOperation.PENG));
    }
}
