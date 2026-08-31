package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authoritative draw/discard/chi/peng/gang/hu/pass state machine. */
public final class MahjongCoreEngine {
    public <S extends MahjongState> MahjongState execute(MahjongState state, MahjongCommand command,
            MahjongRuleSet<MahjongState> rules) {
        if (state.finished()) throw new IllegalStateException("mahjong round already finished");
        return switch (command.operation()) {
            case DRAW -> draw(state, command);
            case DISCARD -> discard(state, command, rules);
            case PASS -> pass(state, command);
            case CHI, PENG, GANG, WEI, PAO, TI -> claim(state, command);
            case HU -> win(state, command, rules);
        };
    }

    private MahjongState draw(MahjongState state, MahjongCommand command) {
        requireTurn(state, command.seatId());
        if (state.currentSeatHasDrawn() || !state.window().isEmpty() || state.wall().isEmpty())
            throw new IllegalStateException("draw is not allowed");
        List<Integer> wall = new ArrayList<>(state.wall()); int tile = wall.remove(0);
        Map<Integer, List<Integer>> hands = copyHands(state); List<Integer> hand = new ArrayList<>(hands.get(command.seatId())); hand.add(tile); hands.put(command.seatId(), hand);
        return state.replace(wall, hands, state.currentSeat(), true, state.lastDiscard(), state.lastDiscardSeat(), MahjongOperationWindow.empty(), false, -1);
    }

    private MahjongState discard(MahjongState state, MahjongCommand command, MahjongRuleSet<MahjongState> rules) {
        requireTurn(state, command.seatId());
        if (!state.currentSeatHasDrawn() || !state.window().isEmpty()) throw new IllegalStateException("discard is not allowed");
        Map<Integer, List<Integer>> hands = copyHands(state); List<Integer> hand = new ArrayList<>(hands.get(command.seatId()));
        if (!hand.remove(Integer.valueOf(command.tile()))) throw new IllegalStateException("tile is not in hand");
        hands.put(command.seatId(), hand);
        MahjongState discarded = state.replace(state.wall(), hands, state.currentSeat(), false, command.tile(), command.seatId(), MahjongOperationWindow.empty(), false, -1);
        Map<Integer, Set<MahjongOperation>> candidates = new HashMap<>();
        for (int seat : hands.keySet()) if (seat != command.seatId()) {
            Set<MahjongOperation> allowed = rules.allowedOperations(seat, discarded);
            if (!allowed.isEmpty()) candidates.put(seat, allowed);
        }
        return discarded.replace(discarded.wall(), discarded.hands(), discarded.currentSeat(), false, discarded.lastDiscard(), discarded.lastDiscardSeat(), new MahjongOperationWindow(candidates), false, -1);
    }

    private MahjongState pass(MahjongState state, MahjongCommand command) {
        if (!state.window().allows(command.seatId(), MahjongOperation.PASS)) throw new IllegalStateException("pass is not allowed");
        MahjongOperationWindow window = state.window().pass(command.seatId());
        int next = window.isEmpty() ? nextSeat(state, state.lastDiscardSeat()) : state.currentSeat();
        return state.replace(state.wall(), state.hands(), next, false, state.lastDiscard(), state.lastDiscardSeat(), window, false, -1);
    }

    private MahjongState claim(MahjongState state, MahjongCommand command) {
        if (!state.window().allows(command.seatId(), command.operation())) throw new IllegalStateException("claim is not allowed");
        List<Integer> hand = state.mutableHand(command.seatId());
        for (int tile : command.consumedTiles()) if (!hand.remove(Integer.valueOf(tile))) throw new IllegalStateException("claimed tile is not in hand");
        Map<Integer, List<Integer>> hands = copyHands(state); hands.put(command.seatId(), hand);
        return state.replace(state.wall(), hands, command.seatId(), false, state.lastDiscard(), state.lastDiscardSeat(), MahjongOperationWindow.empty(), false, -1);
    }

    private MahjongState win(MahjongState state, MahjongCommand command, MahjongRuleSet<MahjongState> rules) {
        boolean candidate = state.window().allows(command.seatId(), MahjongOperation.HU);
        boolean selfDraw = command.seatId() == state.currentSeat() && state.currentSeatHasDrawn();
        if (!candidate && !selfDraw) throw new IllegalStateException("hu is not allowed");
        int tile = selfDraw ? command.tile() : state.lastDiscard();
        if (!rules.canWin(command.seatId(), state.hands().get(command.seatId()), tile, state)) throw new IllegalStateException("hand cannot win");
        return state.replace(state.wall(), state.hands(), state.currentSeat(), state.currentSeatHasDrawn(), state.lastDiscard(), state.lastDiscardSeat(), MahjongOperationWindow.empty(), true, command.seatId());
    }

    private static void requireTurn(MahjongState state, int seat) { if (state.currentSeat() != seat) throw new IllegalStateException("not current seat"); }
    private static Map<Integer, List<Integer>> copyHands(MahjongState state) { return new LinkedHashMap<>(state.hands()); }
    private static int nextSeat(MahjongState state, int seat) { List<Integer> seats = state.hands().keySet().stream().sorted().toList(); return seats.get((seats.indexOf(seat) + 1) % seats.size()); }
}
