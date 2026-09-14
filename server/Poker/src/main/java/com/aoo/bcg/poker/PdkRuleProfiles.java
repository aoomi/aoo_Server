package com.aoo.bcg.poker;

/** 公共 2/3/4 人房间使用 48 张牌，保证每种人数都能整除发牌且不产生客户端决定的余牌。 */
public final class PdkRuleProfiles {
    private PdkRuleProfiles() { }

    public static PokerRuleProfile flexibleTwoToFourPlayers(String playVersion,
            Integer requiredFirstCard) {
        return new PokerRuleProfile(playVersion, 48, 2, 4,
                requiredFirstCard == null ? PokerRuleProfile.FirstLead.RANDOM
                        : PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER,
                requiredFirstCard, 5, 2, false, false, true, true,
                1, 16, 2, 24, PokerCardCodec.deck(48));
    }
}
