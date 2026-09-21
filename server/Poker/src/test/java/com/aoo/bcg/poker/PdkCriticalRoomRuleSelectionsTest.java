package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies that workbook-backed room selections, not stale defaults, own critical PDK rules. */
final class PdkCriticalRoomRuleSelectionsTest {
    @Test void tripleAceBombSelectionEnablesAndDisablesTheActualCombination() {
        PaoDeKuaiRuleSet enabled = rules(Map.of(
                "playRule", List.of("triple_ace_bomb")));
        assertEquals("SPECIAL_TRIPLE_BOMB",
                enabled.recognize(List.of(114, 214, 314), null).type());

        PaoDeKuaiRuleSet disabled = rules(Map.of(
                "playRule", List.of(),
                "specialTripleBombRanks", List.of(14)));
        assertThrows(IllegalArgumentException.class,
                () -> disabled.recognize(List.of(114, 214, 314), null));
    }

    @Test void requireSpadeThreeSelectionEnablesAndDisablesTheActualFirstPlayConstraint() {
        PaoDeKuaiRuleSet enabled = rules(Map.of(
                "playRule", List.of("require_spade_three")));
        PaoDeKuaiContext firstTurn = new PaoDeKuaiContext(
                true, 2, List.of(103, 104), 103, true);
        assertThrows(IllegalStateException.class,
                () -> enabled.validatePlay(List.of(104), firstTurn));
        assertDoesNotThrow(() -> enabled.validatePlay(List.of(103), firstTurn));

        PaoDeKuaiConfig disabledConfig = config(Map.of(
                "playRule", List.of(),
                "requiredFirstCard", 103,
                "requiredFirstCardRounds", 99_999));
        assertNull(disabledConfig.requiredFirstCard());
        PaoDeKuaiRuleSet disabled = new PaoDeKuaiRuleSet(
                disabledConfig, ChengduPdkRules.profile("critical-rules", false));
        PaoDeKuaiContext unconstrainedFirstTurn = new PaoDeKuaiContext(
                true, 2, List.of(103, 104));
        assertDoesNotThrow(() -> disabled.validatePlay(List.of(104), unconstrainedFirstTurn));
    }

    @Test void removeThreeFourControlsTwoPlayerDeckButNeverCutsAThreePlayerRoom() {
        NeijiangPdkRules region = new NeijiangPdkRules();
        Map<String,Object> cut = region.authoritativeRules(Map.of(
                "playerCount", 2,
                "playRule", List.of("remove_three_four")), 2);
        assertEquals("CUT_40", cut.get("deckMode"));
        assertEquals(40, ((List<?>) cut.get("deckCards")).size());
        assertTrue(((List<?>) cut.get("deckCards")).stream()
                .map(Number.class::cast)
                .noneMatch(card -> StandardPokerRuleSet.rank(card.intValue()) == 3
                        || StandardPokerRuleSet.rank(card.intValue()) == 4));

        Map<String,Object> standard = region.authoritativeRules(Map.of(
                "playerCount", 2,
                "playRule", List.of()), 2);
        assertEquals("STANDARD_48", standard.get("deckMode"));
        assertEquals(48, ((List<?>) standard.get("deckCards")).size());

        Map<String,Object> threePlayers = region.authoritativeRules(Map.of(
                "playerCount", 3,
                "playRule", List.of("remove_three_four")), 3);
        assertEquals("STANDARD_48", threePlayers.get("deckMode"));
        assertEquals(48, ((List<?>) threePlayers.get("deckCards")).size());
    }

    @Test void liangshanDoesNotInheritChengduOrNeijiangCriticalSelections() {
        LiangshanPdkRules region = new LiangshanPdkRules();
        assertThrows(IllegalArgumentException.class, () -> region.authoritativeRules(Map.of(
                "playerCount", 2,
                "playRule", List.of("triple_ace_bomb")), 2));
        assertThrows(IllegalArgumentException.class, () -> region.authoritativeRules(Map.of(
                "playerCount", 2,
                "playRule", List.of("remove_three_four")), 2));
        assertThrows(IllegalArgumentException.class, () -> region.authoritativeRules(Map.of(
                "playerCount", 2,
                "playRule", List.of("require_spade_three")), 2));
    }

    private static PaoDeKuaiRuleSet rules(Map<String,Object> published) {
        return new PaoDeKuaiRuleSet(config(published),
                ChengduPdkRules.profile("critical-rules", false));
    }

    private static PaoDeKuaiConfig config(Map<String,Object> published) {
        return PdkPublishedRuleOptions.apply(published, ChengduPdkRules.defaults());
    }
}
