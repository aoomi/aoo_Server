package com.aoo.bcg.mahjong;

import java.util.List;

public record MahjongCommand(int seatId, MahjongOperation operation, int tile, List<Integer> consumedTiles) {
    public MahjongCommand {
        if (seatId < 0 || operation == null) throw new IllegalArgumentException("invalid mahjong command");
        consumedTiles = List.copyOf(consumedTiles == null ? List.of() : consumedTiles);
    }
    public static MahjongCommand draw(int seatId) { return new MahjongCommand(seatId, MahjongOperation.DRAW, 0, List.of()); }
    public static MahjongCommand discard(int seatId, int tile) { return new MahjongCommand(seatId, MahjongOperation.DISCARD, tile, List.of()); }
}
