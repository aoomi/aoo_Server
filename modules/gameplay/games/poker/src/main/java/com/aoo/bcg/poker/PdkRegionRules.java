package com.aoo.bcg.poker;

import java.util.Map;

/** Region publication profile. It may normalize immutable room options, never run a game loop. */
interface PdkRegionRules {
    String gameCode();
    String providerKey();
    String defaultDeckMode();
    int defaultPlayers();
    int defaultCardsPerPlayer();
    PaoDeKuaiConfig defaults();
    Map<String,Object> authoritativeRules(Map<String,Object> publishedRules, int players);
    PokerRuleProfile profile(String playVersion, Map<String,Object> authoritativeRules,
            PaoDeKuaiConfig config);
}
