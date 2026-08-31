package com.aoo.bcg.mahjong;

public record LaiZiMahjongConfig(int wildcardTile, StandardMahjongConfig operations) {
    public LaiZiMahjongConfig {
        if (wildcardTile <= 0 || operations == null) throw new IllegalArgumentException("invalid lai-zi config");
    }
}
