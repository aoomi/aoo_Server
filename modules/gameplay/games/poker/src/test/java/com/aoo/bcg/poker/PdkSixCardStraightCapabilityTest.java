package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PdkSixCardStraightCapabilityTest {
    private static final String VERSION = "six-card-straight-capability";

    @Test
    void publishedSixCardMinimumAcceptsSixAndLongerStraights() {
        PaoDeKuaiRuleSet rules = rules(Map.<String, Object>of("minimumStraightLength", 6));

        assertEquals("STRAIGHT", rules.recognize(List.of(108, 109, 110, 111, 112, 113), null).type());
        assertEquals("STRAIGHT", rules.recognize(List.of(107, 108, 109, 110, 111, 112, 113), null).type());
    }

    @Test
    void publishedSixCardMinimumRejectsFiveGapsAndRankTwo() {
        PaoDeKuaiRuleSet rules = rules(Map.<String, Object>of("minimumStraightLength", 6));

        assertThrows(IllegalArgumentException.class,
                () -> rules.recognize(List.of(108, 109, 110, 111, 112), null));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recognize(List.of(108, 109, 110, 112, 113, 114), null));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recognize(List.of(110, 111, 112, 113, 114, 115), null));
    }

    @Test
    void snapshotPreservesPublishedSixCardMinimum() {
        Map<String, Object> published = Map.of("minimumStraightLength", 6);
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published, PaoDeKuaiConfig.defaults());
        PokerRuleProfile profile = profile(published, config);
        Map<String, Object> snapshot = PdkPublishedRuleOptions.snapshot(config, profile);

        PaoDeKuaiConfig restored = PdkPublishedRuleOptions.apply(snapshot, PaoDeKuaiConfig.defaults());
        PokerRuleProfile restoredProfile = profile(snapshot, restored);
        assertEquals(6, snapshot.get("minimumStraightLength"));
        assertEquals(6, restored.minimumStraightLength());
        assertEquals(6, restoredProfile.minimumStraightLength());
        assertEquals("STRAIGHT", new PaoDeKuaiRuleSet(restored, restoredProfile)
                .recognize(List.of(108, 109, 110, 111, 112, 113), null).type());
    }

    @Test
    void missingPublishedMinimumKeepsExistingFiveCardDefault() {
        PaoDeKuaiRuleSet rules = rules(Map.of());

        assertEquals(5, rules.config().minimumStraightLength());
        assertEquals("STRAIGHT", rules.recognize(List.of(108, 109, 110, 111, 112), null).type());
        assertThrows(IllegalArgumentException.class,
                () -> rules.recognize(List.of(108, 109, 110, 111), null));
    }

    private static PaoDeKuaiRuleSet rules(Map<String, Object> published) {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published, PaoDeKuaiConfig.defaults());
        return new PaoDeKuaiRuleSet(config, profile(published, config));
    }

    private static PokerRuleProfile profile(Map<String, Object> published, PaoDeKuaiConfig config) {
        return PdkPublishedRuleOptions.profile(VERSION, published, config,
                PdkRuleProfiles.flexibleTwoToFourPlayers(VERSION, null));
    }
}
