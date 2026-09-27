package com.aoo.bcg.mahjong;

import java.util.HashSet;
import java.util.List;

/** Shared exchange-three, missing-suit and win-then-continue flow. */
public final class XueZhanFlow {
    public XueZhanState submitExchange(XueZhanState state, int seat, List<Integer> tiles) {
        requirePhase(state, XueZhanPhase.EXCHANGE_THREE); requireSeat(state, seat);
        MahjongRegionalRules.OpeningSelections selections=state.openingSelections().submitExchange(seat,state.hands().get(seat),tiles);
        if(selections.exchanges().size()<state.hands().size())return new XueZhanState(state.phase(),state.hands(),selections,state.winners());
        List<Integer>seats=state.hands().keySet().stream().sorted().toList();
        return new XueZhanState(XueZhanPhase.DING_QUE,selections.applyExchange(seats,state.hands(),1),selections,state.winners());
    }
    public XueZhanState chooseMissingSuit(XueZhanState state,int seat,MahjongSuit suit){
        requirePhase(state,XueZhanPhase.DING_QUE);requireSeat(state,seat);MahjongRegionalRules.OpeningSelections selections=state.openingSelections().submitMissingSuit(seat,suit);
        return new XueZhanState(selections.missingSuits().size()==state.hands().size()?XueZhanPhase.PLAYING:state.phase(),state.hands(),selections,state.winners());
    }
    public boolean mayDiscard(XueZhanState state,int seat,int tile){
        requirePhase(state,XueZhanPhase.PLAYING); MahjongSuit missing=state.openingSelections().missingSuits().get(seat); if(missing==null)return false;
        boolean stillHasMissing=state.hands().get(seat).stream().anyMatch(missing::contains); return !stillHasMissing||missing.contains(tile);
    }
    public boolean mayWin(XueZhanState state,int seat){ MahjongSuit missing=state.openingSelections().missingSuits().get(seat); return state.phase()==XueZhanPhase.PLAYING&&!state.winners().contains(seat)&&missing!=null&&state.hands().get(seat).stream().noneMatch(missing::contains); }
    public XueZhanState recordWin(XueZhanState state,int seat){ if(!mayWin(state,seat))throw new IllegalStateException("seat cannot win"); HashSet<Integer>winners=new HashSet<>(state.winners()); winners.add(seat); XueZhanPhase phase=winners.size()>=state.hands().size()-1?XueZhanPhase.FINISHED:XueZhanPhase.PLAYING; return new XueZhanState(phase,state.hands(),state.openingSelections(),winners); }
    private static void requirePhase(XueZhanState s,XueZhanPhase p){if(s.phase()!=p)throw new IllegalStateException("invalid xue-zhan phase");} private static void requireSeat(XueZhanState s,int seat){if(!s.hands().containsKey(seat))throw new IllegalArgumentException("unknown seat");}
}
