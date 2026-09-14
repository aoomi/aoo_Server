package com.aoo.bcg.mahjong;

public interface MahjongRuleFamily {
    String familyCode();
    MahjongRuleSet<MahjongState> ruleSet();
    default MahjongTileSet tileSet(String playVersion) { return MahjongTileSet.standard(playVersion); }
    /** Concealed opening size; regional word-tile variants deal twenty instead of thirteen. */
    default int openingHandSize(){return 13;}
}
