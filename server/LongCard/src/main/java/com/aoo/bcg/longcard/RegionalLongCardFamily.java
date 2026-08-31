package com.aoo.bcg.longcard;

import java.util.List;
import java.util.Set;

public final class RegionalLongCardFamily implements LongCardRuleFamily {
    private final RegionalLongCardProfile profile;
    private final LongCardRuleSet<LongCardRuleContext> rules = new LongCardRuleSet<>() {
        @Override public Set<LongCardOperation> allowedOperations(int seatId, LongCardRuleContext context) {
            if (seatId < 0) throw new IllegalArgumentException("seatId must not be negative");
            return profile.allowedOperations();
        }
        @Override public boolean canWin(int seatId, List<Integer> cards, int incomingCard,
                LongCardRuleContext context) {
            return context != null && context.huPoints() >= profile.minimumHuPoints();
        }
    };
    public RegionalLongCardFamily(RegionalLongCardProfile profile) {
        this.profile = java.util.Objects.requireNonNull(profile);
    }
    @Override public String familyCode() { return profile.familyCode(); }
    public RegionalLongCardProfile profile() { return profile; }
    public LongCardRuleSet<LongCardRuleContext> ruleSet() { return rules; }
    public LongCardCoreEngine<LongCardRuleContext> engine() { return new LongCardCoreEngine<>(rules); }
}
