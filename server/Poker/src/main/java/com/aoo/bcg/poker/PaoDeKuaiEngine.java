package com.aoo.bcg.poker;
import java.util.List;
/** Mandatory Paodekuai guard followed by the shared authoritative poker engine. */
public final class PaoDeKuaiEngine { private final PokerCoreEngine<PaoDeKuaiContext> core=new PokerCoreEngine<>(); public PokerTurnState play(PokerTurnState state,int seat,List<Integer> cards,PaoDeKuaiRuleSet rules,PaoDeKuaiContext context){rules.validatePlay(cards,context);return core.play(state,seat,cards,rules,context);} public PokerTurnState pass(PokerTurnState state,int seat){return core.pass(state,seat);} }
