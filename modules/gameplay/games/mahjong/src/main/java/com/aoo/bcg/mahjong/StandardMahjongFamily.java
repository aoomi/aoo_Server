package com.aoo.bcg.mahjong;

/** Shared standard Mahjong engine used by core contracts and catalog profiles. */
public final class StandardMahjongFamily implements MahjongRuleFamily {
    private final StandardMahjongConfig config;
    private final StandardMahjongRuleSet rules;

    public StandardMahjongFamily(StandardMahjongConfig config) {
        this.config = config;
        this.rules = new StandardMahjongRuleSet(config);
    }

    @Override public String familyCode() { return "mahjong-standard"; }
    @Override public MahjongRuleSet<MahjongState> ruleSet() { return rules; }
    @Override public MahjongTileSet tileSet(String version) { return MahjongTileSet.standard(version); }
}
