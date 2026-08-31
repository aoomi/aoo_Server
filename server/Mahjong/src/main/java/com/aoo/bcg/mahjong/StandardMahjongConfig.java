package com.aoo.bcg.mahjong;

public record StandardMahjongConfig(boolean allowChi, boolean allowPeng, boolean allowExposedGang) {
    public static StandardMahjongConfig defaults() { return new StandardMahjongConfig(true, true, true); }
}
