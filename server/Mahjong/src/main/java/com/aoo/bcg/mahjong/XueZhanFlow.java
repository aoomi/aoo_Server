package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Shared exchange-three, missing-suit and win-then-continue flow. */
public final class XueZhanFlow {
    public XueZhanState submitExchange(XueZhanState state, int seat, List<Integer> tiles) {
        requirePhase(state, XueZhanPhase.EXCHANGE_THREE); requireSeat(state, seat);
        if (tiles.size()!=3 || tiles.stream().map(tile -> tile/10).distinct().count()!=1) throw new IllegalArgumentException("exchange requires three same-suit tiles");
        List<Integer> hand = new ArrayList<>(state.hands().get(seat));
        for(int tile:tiles) if(!hand.remove(Integer.valueOf(tile))) throw new IllegalArgumentException("exchange tile not in hand");
        HashMap<Integer,List<Integer>> exchanges=new HashMap<>(state.exchanges()); if(exchanges.putIfAbsent(seat,List.copyOf(tiles))!=null) throw new IllegalStateException("exchange already submitted");
        boolean complete=exchanges.size()==state.hands().size();
        if(!complete)return new XueZhanState(state.phase(),state.hands(),exchanges,state.missingSuits(),state.winners());
        List<Integer> seats=state.hands().keySet().stream().sorted().toList(); HashMap<Integer,List<Integer>> exchangedHands=new HashMap<>();
        for(int index=0;index<seats.size();index++){int current=seats.get(index);int source=seats.get((index-1+seats.size())%seats.size());List<Integer> nextHand=new ArrayList<>(state.hands().get(current));for(int tile:exchanges.get(current))nextHand.remove(Integer.valueOf(tile));nextHand.addAll(exchanges.get(source));exchangedHands.put(current,nextHand);}
        return new XueZhanState(XueZhanPhase.DING_QUE,exchangedHands,exchanges,state.missingSuits(),state.winners());
    }
    public XueZhanState chooseMissingSuit(XueZhanState state,int seat,MahjongSuit suit){
        requirePhase(state,XueZhanPhase.DING_QUE); requireSeat(state,seat); HashMap<Integer,MahjongSuit> choices=new HashMap<>(state.missingSuits());
        if(choices.putIfAbsent(seat,suit)!=null) throw new IllegalStateException("missing suit already chosen");
        return new XueZhanState(choices.size()==state.hands().size()?XueZhanPhase.PLAYING:state.phase(),state.hands(),state.exchanges(),choices,state.winners());
    }
    public boolean mayDiscard(XueZhanState state,int seat,int tile){
        requirePhase(state,XueZhanPhase.PLAYING); MahjongSuit missing=state.missingSuits().get(seat); if(missing==null)return false;
        boolean stillHasMissing=state.hands().get(seat).stream().anyMatch(missing::contains); return !stillHasMissing||missing.contains(tile);
    }
    public boolean mayWin(XueZhanState state,int seat){ MahjongSuit missing=state.missingSuits().get(seat); return state.phase()==XueZhanPhase.PLAYING&&!state.winners().contains(seat)&&state.hands().get(seat).stream().noneMatch(missing::contains); }
    public XueZhanState recordWin(XueZhanState state,int seat){ if(!mayWin(state,seat))throw new IllegalStateException("seat cannot win"); HashSet<Integer>winners=new HashSet<>(state.winners()); winners.add(seat); XueZhanPhase phase=winners.size()>=state.hands().size()-1?XueZhanPhase.FINISHED:XueZhanPhase.PLAYING; return new XueZhanState(phase,state.hands(),state.exchanges(),state.missingSuits(),winners); }
    private static void requirePhase(XueZhanState s,XueZhanPhase p){if(s.phase()!=p)throw new IllegalStateException("invalid xue-zhan phase");} private static void requireSeat(XueZhanState s,int seat){if(!s.hands().containsKey(seat))throw new IllegalArgumentException("unknown seat");}
}
