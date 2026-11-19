package com.aoo.bcg.mahjong;

public record StandardMahjongConfig(boolean allowChi, boolean allowPeng, boolean allowExposedGang,
        boolean continueAfterWin, boolean requireOpeningSelections) {
    public StandardMahjongConfig(boolean allowChi, boolean allowPeng, boolean allowExposedGang) {
        this(allowChi, allowPeng, allowExposedGang, false, false);
    }
    public static StandardMahjongConfig defaults() { return new StandardMahjongConfig(true, true, true); }
}
