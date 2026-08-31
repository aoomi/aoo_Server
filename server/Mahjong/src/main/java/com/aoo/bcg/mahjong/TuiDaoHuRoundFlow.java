package com.aoo.bcg.mahjong;
import java.util.Comparator; import java.util.List;
/** Immediate round-end and dealer progression shared by Tui-Dao-Hu games. */
public final class TuiDaoHuRoundFlow {
 public List<Integer> resolveWinners(int discarder,List<Integer> candidates,int seats,boolean multiple){
  List<Integer> ordered=candidates.stream().distinct().sorted(Comparator.comparingInt(seat->Math.floorMod(seat-discarder,seats))).toList();
  return multiple||ordered.isEmpty()?ordered:List.of(ordered.get(0));
 }
 public int nextDealer(int dealer,int seats,List<Integer>winners,boolean draw,boolean dealerContinuesOnDraw){return (draw&&dealerContinuesOnDraw)||winners.contains(dealer)?dealer:(dealer+1)%seats;}
}
