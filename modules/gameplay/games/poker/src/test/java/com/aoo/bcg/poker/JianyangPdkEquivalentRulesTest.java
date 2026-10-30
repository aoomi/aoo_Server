package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class JianyangPdkEquivalentRulesTest {
    private static final List<Integer> TRIPLE_PAIR = List.of(105, 205, 305, 114, 214);
    private static final List<Integer> HIGHER_TRIPLE_SINGLES = List.of(106, 206, 306, 103, 104);
    private static final List<Integer> HIGHER_TRIPLE_PAIR = List.of(106, 206, 306, 103, 203);
    private static final List<Integer> AIRPLANE_PAIRS = List.of(
            105, 205, 305, 106, 206, 306, 112, 212, 113, 213);
    private static final List<Integer> HIGHER_AIRPLANE_SINGLES = List.of(
            107, 207, 307, 108, 208, 308, 103, 104, 109, 110);
    private static final List<Integer> HIGHER_AIRPLANE_PAIRS = List.of(
            107, 207, 307, 108, 208, 308, 103, 203, 104, 204);

    @Test void publishedAttachmentModeAllowsBothShapesButRejectsCrossShapeTripleResponses() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "tripleAttachmentMode", "EITHER_MATCH_SHAPE"), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        CardCombination lead = rules.recognize(TRIPLE_PAIR, null);
        CardCombination singles = rules.recognize(HIGHER_TRIPLE_SINGLES, null);
        CardCombination pair = rules.recognize(HIGHER_TRIPLE_PAIR, null);

        assertTrue(config.allowTripleWithPair());
        assertEquals("TRIPLE_WITH_PAIR", lead.type());
        assertEquals("TRIPLE_WITH_TWO", singles.type());
        assertFalse(rules.canBeat(singles, lead, null));
        assertTrue(rules.canBeat(pair, lead, null),
                "a higher body beats despite its lower-ranked pair attachment");
        assertEquals(config, PdkPublishedRuleOptions.apply(
                PdkPublishedRuleOptions.snapshot(config), PaoDeKuaiConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> PdkPublishedRuleOptions.apply(
                Map.of("tripleAttachmentMode", "EITHER_MATCH_SHAPE",
                        "compareTripleAttachments", true), PaoDeKuaiConfig.defaults()));
    }

    @Test void airplaneResponseKeepsAttachmentShapeWithoutComparingWingRanks() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "airplaneAttachmentMode", "EITHER_MATCH_SHAPE",
                "allowAirplaneWithTwo", true), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        CardCombination lead = rules.recognize(AIRPLANE_PAIRS, null);
        CardCombination singles = rules.recognize(HIGHER_AIRPLANE_SINGLES, null);
        CardCombination pairs = rules.recognize(HIGHER_AIRPLANE_PAIRS, null);

        assertEquals("AIRPLANE_WITH_PAIRS", lead.type());
        assertEquals("AIRPLANE_WITH_TWO", singles.type());
        assertFalse(rules.canBeat(singles, lead, null));
        assertTrue(rules.canBeat(pairs, lead, null),
                "higher airplane body beats even with smaller pair attachments");
    }

    @Test void existingFreeAttachmentAndRankComparisonStayIndependent() {
        PaoDeKuaiRuleSet free = new PaoDeKuaiRuleSet(PaoDeKuaiConfig.defaults());
        assertTrue(free.canBeat(free.recognize(HIGHER_TRIPLE_SINGLES, null),
                free.recognize(TRIPLE_PAIR, null), null));
        assertTrue(free.canBeat(free.recognize(HIGHER_AIRPLANE_SINGLES, null),
                free.recognize(AIRPLANE_PAIRS, null), null));

        PaoDeKuaiConfig compare = PdkPublishedRuleOptions.apply(Map.of(
                "compareTripleAttachments", true), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rankComparison = new PaoDeKuaiRuleSet(compare);
        assertFalse(rankComparison.canBeat(rankComparison.recognize(HIGHER_TRIPLE_PAIR, null),
                rankComparison.recognize(TRIPLE_PAIR, null), null));

        assertEquals(PaoDeKuaiConfig.AttachmentMode.EITHER,
                ChengduPdkRules.defaults().tripleAttachmentMode());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.EITHER,
                new NeijiangPdkRules().defaults().tripleAttachmentMode());
        LiangshanPdkRules liangshan = new LiangshanPdkRules();
        assertEquals(PaoDeKuaiConfig.AttachmentMode.DISABLED,
                liangshan.defaults().tripleAttachmentMode());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.SINGLE_OR_PAIR,
                PdkPublishedRuleOptions.apply(liangshan.authoritativeRules(Map.of(
                        "playRule", List.of("triple_with_one")), 2), liangshan.defaults())
                        .tripleAttachmentMode());
    }
}
