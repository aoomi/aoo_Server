package com.aoo.bcg.mahjong;

/** Shared Sichuan tile/rule family retained for the standalone Chengdu1001 module. */
public final class XueZhanMahjongFamily implements MahjongRuleFamily {
    private final MahjongRuleSet<MahjongState> rules =
            new StandardMahjongRuleSet(new StandardMahjongConfig(false, true, true, true, true));

    @Override public String familyCode() { return "mahjong-xue-zhan"; }
    @Override public MahjongRuleSet<MahjongState> ruleSet() { return rules; }
    @Override public MahjongTileSet tileSet(String version) { return MahjongTileSet.sichuan(version); }
}
