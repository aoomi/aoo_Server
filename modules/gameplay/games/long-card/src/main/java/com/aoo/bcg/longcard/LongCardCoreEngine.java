package com.aoo.bcg.longcard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed authoritative flow. Regional families may validate, never mutate state directly. */
public final class LongCardCoreEngine<T> {
    private final LongCardRuleSet<T> rules;
    public LongCardCoreEngine(LongCardRuleSet<T> rules) { this.rules = java.util.Objects.requireNonNull(rules); }

    public LongCardState apply(LongCardState state, LongCardCommand command, T context) {
        if (!rules.allowedOperations(command.seatId(), context).contains(command.operation()))
            throw new IllegalStateException("operation disabled by long-card family");
        if (state.phase() == LongCardPhase.FINISHED) throw new IllegalStateException("game already finished");
        if (!state.hands().containsKey(command.seatId())) throw new IllegalArgumentException("seat not found");
        return switch (command.operation()) {
            case REVEAL, DRAW -> reveal(state, command.seatId());
            case STEAL -> steal(state, command.seatId());
            case DISCARD -> discard(state, command);
            case CHI, PENG -> claim(state, command);
            case CALL -> call(state, command.seatId());
            case HU -> win(state, command.seatId(), context);
            case PASS -> pass(state, command.seatId());
        };
    }

    private LongCardState reveal(LongCardState state, int seat) {
        requireTurn(state, seat);
        if (state.deck().isEmpty()) throw new IllegalStateException("deck exhausted");
        List<Integer> deck = new ArrayList<>(state.deck());
        int card = deck.remove(0);
        return copy(state, state.hands(), deck, state.discards(), seat, LongCardPhase.REVEALED,
                card, state.calledSeats(), null);
    }
    private LongCardState steal(LongCardState state, int seat) {
        requireExposed(state, seat);
        Map<Integer, List<Integer>> hands = mutableHands(state);
        hands.get(seat).add(state.exposedCard());
        return copy(state, hands, state.deck(), state.discards(), seat, LongCardPhase.PLAYING,
                null, state.calledSeats(), null);
    }
    private LongCardState discard(LongCardState state, LongCardCommand command) {
        requireTurn(state, command.seatId());
        if (command.cards().size() != 1) throw new IllegalArgumentException("discard requires one card");
        Map<Integer, List<Integer>> hands = mutableHands(state);
        if (!hands.get(command.seatId()).remove(command.cards().get(0)))
            throw new IllegalArgumentException("card is not owned by seat");
        List<Integer> discards = new ArrayList<>(state.discards());
        discards.add(command.cards().get(0));
        return copy(state, hands, state.deck(), discards, nextSeat(state, command.seatId()),
                LongCardPhase.RESPONDING, command.cards().get(0), state.calledSeats(), null);
    }
    private LongCardState claim(LongCardState state, LongCardCommand command) {
        if (state.phase() != LongCardPhase.RESPONDING || state.exposedCard() == null)
            throw new IllegalStateException("no card can be claimed");
        if (command.cards().isEmpty()) throw new IllegalArgumentException("claim cards required");
        Map<Integer, List<Integer>> hands = mutableHands(state);
        List<Integer> hand = hands.get(command.seatId());
        for (Integer card : command.cards()) if (!hand.remove(card))
            throw new IllegalArgumentException("claim card is not owned by seat");
        return copy(state, hands, state.deck(), state.discards(), command.seatId(), LongCardPhase.PLAYING,
                null, state.calledSeats(), null);
    }
    private LongCardState call(LongCardState state, int seat) {
        requireTurn(state, seat);
        Set<Integer> called = new LinkedHashSet<>(state.calledSeats());
        called.add(seat);
        return copy(state, state.hands(), state.deck(), state.discards(), seat, state.phase(),
                state.exposedCard(), called, null);
    }
    private LongCardState win(LongCardState state, int seat, T context) {
        int incoming = state.exposedCard() == null ? -1 : state.exposedCard();
        if (!rules.canWin(seat, state.hands().get(seat), incoming, context))
            throw new IllegalStateException("winning condition not met");
        return copy(state, state.hands(), state.deck(), state.discards(), seat, LongCardPhase.FINISHED,
                state.exposedCard(), state.calledSeats(), seat);
    }
    private LongCardState pass(LongCardState state, int seat) {
        if (state.phase() != LongCardPhase.RESPONDING) throw new IllegalStateException("nothing to pass");
        return copy(state, state.hands(), state.deck(), state.discards(), nextSeat(state, seat),
                LongCardPhase.PLAYING, null, state.calledSeats(), null);
    }
    private static void requireTurn(LongCardState state, int seat) {
        if (state.currentSeat() != seat || state.phase() == LongCardPhase.RESPONDING)
            throw new IllegalStateException("not current seat");
    }
    private static void requireExposed(LongCardState state, int seat) {
        requireTurn(state, seat);
        if (state.exposedCard() == null) throw new IllegalStateException("no exposed card");
    }
    private static int nextSeat(LongCardState state, int seat) {
        List<Integer> seats = state.hands().keySet().stream().sorted().toList();
        int index = seats.indexOf(seat);
        if (index < 0) throw new IllegalArgumentException("seat not found");
        return seats.get((index + 1) % seats.size());
    }
    private static Map<Integer, List<Integer>> mutableHands(LongCardState state) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        state.hands().forEach((seat, cards) -> result.put(seat, new ArrayList<>(cards)));
        return result;
    }
    private static LongCardState copy(LongCardState old, Map<Integer, List<Integer>> hands,
            List<Integer> deck, List<Integer> discards, int seat, LongCardPhase phase,
            Integer exposed, Set<Integer> called, Integer winner) {
        return new LongCardState(hands, deck, discards, seat, phase, exposed, called, winner);
    }
}

enum LongCardOperation { DRAW, REVEAL, STEAL, DISCARD, CHI, PENG, CALL, HU, PASS }
enum LongCardPhase { WAITING, REVEALED, PLAYING, RESPONDING, FINISHED }
record LongCardCommand(LongCardOperation operation,int seatId,List<Integer>cards){
    LongCardCommand{if(operation==null||seatId<0)throw new IllegalArgumentException("invalid long-card command");cards=List.copyOf(cards==null?List.of():cards);}
    static LongCardCommand of(LongCardOperation operation,int seatId,Integer...cards){return new LongCardCommand(operation,seatId,List.of(cards));}
}
record LongCardRuleContext(int huPoints){LongCardRuleContext{if(huPoints<0)throw new IllegalArgumentException("huPoints must not be negative");}}
interface LongCardRuleFamily{String familyCode();}
interface LongCardRuleSet<T>{Set<LongCardOperation>allowedOperations(int seatId,T state);boolean canWin(int seatId,List<Integer>cards,int incomingCard,T state);}
