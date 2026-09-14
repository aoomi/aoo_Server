package com.aoo.bcg.mahjong;
public enum MahjongSuit { WAN(1), TIAO(2), TONG(3); final int prefix; MahjongSuit(int prefix){this.prefix=prefix;} public boolean contains(int tile){return tile/10==prefix;} }
