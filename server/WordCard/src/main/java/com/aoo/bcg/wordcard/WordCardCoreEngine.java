package com.aoo.bcg.wordcard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Authoritative draw/discard/chi/peng/wei/pao/ti/hu/pass flow shared by all word-card families. */
public final class WordCardCoreEngine<T> {
    private final WordCardRuleSet<T> rules;
    public WordCardCoreEngine(WordCardRuleSet<T> rules) { this.rules=java.util.Objects.requireNonNull(rules); }
    public WordCardState apply(WordCardState state, WordCardCommand command, T context) {
        if (state.phase()==WordCardPhase.FINISHED) throw new IllegalStateException("game finished");
        if (!state.hands().containsKey(command.seatId())) throw new IllegalArgumentException("seat not found");
        if (!rules.allowedOperations(command.seatId(), context).contains(command.operation())) throw new IllegalStateException("operation disabled by family");
        return switch(command.operation()) {
            case DRAW -> draw(state,command.seatId());
            case DISCARD -> discard(state,command);
            case CHI,PENG,WEI,PAO,TI -> meld(state,command);
            case HU -> win(state,command.seatId(),context);
            case PASS -> pass(state,command.seatId());
        };
    }
    private WordCardState draw(WordCardState s,int seat){requireTurn(s,seat);if(s.deck().isEmpty())throw new IllegalStateException("deck exhausted");var d=new ArrayList<>(s.deck());int card=d.remove(0);var h=hands(s);h.get(seat).add(card);return copy(s,h,d,s.discards(),seat,WordCardPhase.PLAYING,card,null,s.huXi());}
    private WordCardState discard(WordCardState s,WordCardCommand c){requireTurn(s,c.seatId());if(c.cards().size()!=1)throw new IllegalArgumentException("discard requires one card");var h=hands(s);if(!h.get(c.seatId()).remove(c.cards().get(0)))throw new IllegalArgumentException("card not owned");var out=new ArrayList<>(s.discards());out.add(c.cards().get(0));return copy(s,h,s.deck(),out,next(s,c.seatId()),WordCardPhase.RESPONDING,c.cards().get(0),null,s.huXi());}
    private WordCardState meld(WordCardState s,WordCardCommand c){if(s.exposedCard()==null)throw new IllegalStateException("no exposed card");if(c.cards().isEmpty())throw new IllegalArgumentException("meld cards required");var h=hands(s);for(Integer card:c.cards())if(!h.get(c.seatId()).remove(card))throw new IllegalArgumentException("meld card not owned");var points=new LinkedHashMap<>(s.huXi());points.merge(c.seatId(),meldPoints(c.operation()),Integer::sum);return copy(s,h,s.deck(),s.discards(),c.seatId(),WordCardPhase.PLAYING,null,null,points);}
    private WordCardState win(WordCardState s,int seat,T context){int incoming=s.exposedCard()==null?-1:s.exposedCard();if(!rules.canWin(seat,s.hands().get(seat),incoming,context))throw new IllegalStateException("winning condition not met");return copy(s,s.hands(),s.deck(),s.discards(),seat,WordCardPhase.FINISHED,s.exposedCard(),seat,s.huXi());}
    private WordCardState pass(WordCardState s,int seat){if(s.phase()!=WordCardPhase.RESPONDING)throw new IllegalStateException("nothing to pass");return copy(s,s.hands(),s.deck(),s.discards(),next(s,seat),WordCardPhase.PLAYING,null,null,s.huXi());}
    private static int meldPoints(WordCardOperation op){return switch(op){case CHI->0;case PENG->1;case WEI->3;case PAO->6;case TI->9;default->throw new IllegalArgumentException("not meld");};}
    private static void requireTurn(WordCardState s,int seat){if(s.currentSeat()!=seat||s.phase()==WordCardPhase.RESPONDING)throw new IllegalStateException("not current seat");}
    private static int next(WordCardState s,int seat){var ids=s.hands().keySet().stream().sorted().toList();int i=ids.indexOf(seat);if(i<0)throw new IllegalArgumentException("seat not found");return ids.get((i+1)%ids.size());}
    private static Map<Integer,List<Integer>> hands(WordCardState s){Map<Integer,List<Integer>> h=new LinkedHashMap<>();s.hands().forEach((k,v)->h.put(k,new ArrayList<>(v)));return h;}
    private static WordCardState copy(WordCardState old,Map<Integer,List<Integer>> h,List<Integer>d,List<Integer>out,int seat,WordCardPhase phase,Integer exposed,Integer winner,Map<Integer,Integer>points){return new WordCardState(h,d,out,points,seat,phase,exposed,winner);}
}
