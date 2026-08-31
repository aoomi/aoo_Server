package com.aoo.bcg.mahjong;
import java.util.List;
import java.util.Set;
public interface MahjongRuleSet<T> { Set<MahjongOperation> allowedOperations(int seatId, T state); boolean canWin(int seatId, List<Integer> concealedTiles, int winningTile, T state); }
