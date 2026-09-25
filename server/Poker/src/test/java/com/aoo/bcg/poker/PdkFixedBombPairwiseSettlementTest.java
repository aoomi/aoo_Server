package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PdkFixedBombPairwiseSettlementTest {
    @Test void nonWinnerBombIsPaidByWinnerAndEveryOtherOpponent() {
        assertEquals(Map.of(10L, -10L, 11L, 20L, 12L, -10L),
                score(3, 0, Map.of(1, 1), "FIXED_POINTS"));
    }

    @Test void winnerBombIsPaidOnceByEachOpponent() {
        assertEquals(Map.of(10L, 20L, 11L, -10L, 12L, -10L),
                score(3, 0, Map.of(0, 1), "FIXED_POINTS"));
    }

    @Test void noBombsAndMultipleBombsRemainZeroSum() {
        assertEquals(Map.of(10L, 0L, 11L, 0L, 12L, 0L),
                score(3, 0, Map.of(), "FIXED_POINTS"));
        assertEquals(Map.of(10L, 40L, 11L, 0L, 12L, -40L, 13L, 0L),
                score(4, 0, Map.of(0, 2, 1, 1, 3, 1), "FIXED_POINTS"));
    }

    @Test void twoPlayerFixedScoreKeepsExistingPayment() {
        assertEquals(Map.of(10L, -10L, 11L, 10L),
                score(2, 0, Map.of(1, 1), "FIXED_POINTS"));
    }

    @Test void fixedBombPaymentAlsoAppliesDuringDealerSettlement() {
        assertEquals(Map.of(10L, 2L, 11L, 14L, 12L, -16L),
                score(3, 0, Map.of(1, 1), "FIXED_POINTS", true));
    }

    @Test void disabledMultiplierAndLegacyPairwiseModesRemainIsolated() {
        assertEquals(Map.of(10L, 0L, 11L, 0L, 12L, 0L),
                score(3, 0, Map.of(1, 1), "DISABLED"));
        assertEquals(Map.of(10L, 0L, 11L, 0L, 12L, 0L),
                score(3, 0, Map.of(1, 1), "MULTIPLIER"));
        assertEquals(Map.of(10L, -5L, 11L, 10L, 12L, -5L),
                score(3, 0, Map.of(1, 1), "PAIRWISE"));
    }

    private static Map<Long,Long> score(int playerCount, int winner,
            Map<Integer,Integer> bombCounts, String bombMode) {
        return score(playerCount, winner, bombCounts, bombMode, false);
    }

    private static Map<Long,Long> score(int playerCount, int winner,
            Map<Integer,Integer> bombCounts, String bombMode, boolean competeDealer) {
        Map<String,Object> published = new LinkedHashMap<>(Map.of(
                "baseScore", 2, "bombScoreMode", bombMode, "bombFixedPoints", 5,
                "bombScoreCap", 2, "springMode", "DISABLED",
                "reverseSpringMode", "DISABLED", "handScoreTable", List.of(0, 0)));
        published.put("competeDealerEnabled", competeDealer);
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        Map<Integer,Long> players = new LinkedHashMap<>();
        Map<Integer,List<Integer>> hands = new LinkedHashMap<>();
        Map<Integer,Integer> plays = new LinkedHashMap<>();
        for (int seat = 0; seat < playerCount; seat++) {
            players.put(seat, 10L + seat);
            hands.put(seat, seat == winner ? List.of() : List.of(103));
            plays.put(seat, 2);
        }
        PdkSettlementContext context = new PdkSettlementContext(36L, 1, "pdk-036",
                winner, winner, players, hands, plays, bombCounts,
                Map.of(), Map.of(), -1, competeDealer ? winner : -1,
                PdkRuleProfiles.flexibleTwoToFourPlayers("pdk-036", null), config);
        return PdkScoringPolicy.standard().settle(context).scoreDelta();
    }
}
