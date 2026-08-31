package com.aoo.bcg.mahjong;
import java.util.HashMap;
/** In blood-flow Mahjong winning records a settlement event but never removes the player. */
public final class XueLiuFlow {
 public XueLiuState start(Iterable<Integer> seats){HashMap<Integer,Integer> counts=new HashMap<>();seats.forEach(seat->counts.put(seat,0));if(counts.size()<2)throw new IllegalArgumentException("at least two seats required");return new XueLiuState(counts,false);}
 public XueLiuState recordWin(XueLiuState state,int seat){if(state.finished()||!state.winCounts().containsKey(seat))throw new IllegalStateException("win is not allowed");HashMap<Integer,Integer> counts=new HashMap<>(state.winCounts());counts.merge(seat,1,Integer::sum);return new XueLiuState(counts,false);}
 public XueLiuState finishWhenWallEmpty(XueLiuState state,int wallSize){if(wallSize<0)throw new IllegalArgumentException("invalid wall size");return wallSize==0?new XueLiuState(state.winCounts(),true):state;}
}
