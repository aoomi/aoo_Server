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
        if (state.currentSeatHasDrawn() || !state.window().isEmpty())
            throw new IllegalStateException("draw is not allowed");
        if (state.wall().isEmpty()) return finishDraw(state);
        List<Integer> wall = new ArrayList<>(state.wall()); int tile = wall.remove(0);
        Map<Integer, List<Integer>> hands = copyHands(state); List<Integer> hand = new ArrayList<>(hands.get(command.seatId())); hand.add(tile); hands.put(command.seatId(), hand);
        return state.replaceAll(wall,hands,state.currentSeat(),true,state.lastDiscard(),state.lastDiscardSeat(),
                MahjongOperationWindow.empty(),false,state.winnerSeat(),state.melds(),state.winners(),tile,state.openingSelections());
    }

    private MahjongState discard(MahjongState state, MahjongCommand command, MahjongRuleSet<MahjongState> rules) {
        requireTurn(state, command.seatId());
        if (!state.currentSeatHasDrawn() || !state.window().isEmpty()) throw new IllegalStateException("discard is not allowed");
        Map<Integer, List<Integer>> hands = copyHands(state); List<Integer> hand = new ArrayList<>(hands.get(command.seatId()));
        if (!hand.remove(Integer.valueOf(command.tile()))) throw new IllegalStateException("tile is not in hand");
        hands.put(command.seatId(), hand);
        MahjongState discarded = state.replace(state.wall(), hands, state.currentSeat(), false, command.tile(), command.seatId(), MahjongOperationWindow.empty(), false, -1);
        Map<Integer, Set<MahjongOperation>> candidates = new HashMap<>();
        for (int seat : hands.keySet()) if (seat != command.seatId() && !state.winners().contains(seat)) {
            Set<MahjongOperation> allowed = rules.allowedOperations(seat, discarded);
            if (!allowed.isEmpty()) candidates.put(seat, allowed);
        }
        int nextSeat = candidates.isEmpty() ? nextActiveSeat(discarded, command.seatId()) : discarded.currentSeat();
        MahjongState result=discarded.replaceAll(discarded.wall(),discarded.hands(),nextSeat,false,
                discarded.lastDiscard(),discarded.lastDiscardSeat(),new MahjongOperationWindow(candidates),false,
                state.winnerSeat(),state.melds(),state.winners(),0,state.openingSelections());
        return candidates.isEmpty()&&result.wall().isEmpty()?finishDraw(result):result;
    }

    private MahjongState pass(MahjongState state, MahjongCommand command) {
        if (!state.window().allows(command.seatId(), MahjongOperation.PASS)) throw new IllegalStateException("pass is not allowed");
        MahjongOperationWindow window = state.window().pass(command.seatId());
        int next = window.isEmpty() ? nextActiveSeat(state, state.lastDiscardSeat()) : state.currentSeat();
        MahjongState result=state.replaceAll(state.wall(),state.hands(),next,false,state.lastDiscard(),state.lastDiscardSeat(),
                window,false,state.winnerSeat(),state.melds(),state.winners(),0,state.openingSelections());
        return window.isEmpty()&&result.wall().isEmpty()?finishDraw(result):result;
    }

    private MahjongState claim(MahjongState state, MahjongCommand command) {
        if (command.operation() == MahjongOperation.GANG && (command.gangType() == 3 || command.gangType() == 4))
            return selfGang(state, command);
        if (!state.window().allows(command.seatId(), command.operation())) throw new IllegalStateException("claim is not allowed");
        if (command.operation() == MahjongOperation.CHI) {
            if (command.consumedTiles().size() != 2) throw new IllegalStateException("chi requires two hand tiles");
            List<Integer> sequence = new ArrayList<>(command.consumedTiles());
            sequence.add(state.lastDiscard());
            sequence.sort(Integer::compareTo);
            if (sequence.get(0) < 11 || sequence.get(2) > 39
                    || sequence.get(0) % 10 < 1 || sequence.get(2) % 10 > 9
                    || sequence.get(0) / 10 != sequence.get(2) / 10
                    || sequence.get(0) + 1 != sequence.get(1) || sequence.get(1) + 1 != sequence.get(2)) {
                throw new IllegalStateException("chi tiles are not a suited sequence");
            }
        } else if (command.operation() == MahjongOperation.PENG || command.operation() == MahjongOperation.GANG) {
            int required = command.operation() == MahjongOperation.GANG ? 3 : 2;
            if (command.consumedTiles().size() != required
                    || command.consumedTiles().stream().anyMatch(tile -> tile != state.lastDiscard())) {
                throw new IllegalStateException("claim tiles do not match the discard");
            }
        }
        List<Integer> hand = state.mutableHand(command.seatId());
        for (int tile : command.consumedTiles()) if (!hand.remove(Integer.valueOf(tile))) throw new IllegalStateException("claimed tile is not in hand");
        List<Integer> meldTiles=new ArrayList<>(command.consumedTiles());meldTiles.add(state.lastDiscard());
        int expected=command.operation()==MahjongOperation.GANG?4:3;
        if(meldTiles.size()!=expected)throw new IllegalStateException("claim tile count is invalid");
        Map<Integer, List<Integer>> hands = copyHands(state); hands.put(command.seatId(), hand);
        Map<Integer,List<MahjongState.Meld>>melds=new LinkedHashMap<>(state.melds());
        List<MahjongState.Meld>seatMelds=new ArrayList<>(melds.getOrDefault(command.seatId(),List.of()));
        seatMelds.add(new MahjongState.Meld(command.operation(),meldTiles,state.lastDiscardSeat()));melds.put(command.seatId(),seatMelds);
        MahjongState claimed=state.replaceAll(state.wall(),hands,command.seatId(),true,state.lastDiscard(),state.lastDiscardSeat(),
                MahjongOperationWindow.empty(),false,state.winnerSeat(),melds,state.winners(),0,state.openingSelections());
        if(command.operation()!=MahjongOperation.GANG)return claimed;
        if(claimed.wall().isEmpty())return finishDraw(claimed);
        MahjongRegionalRules.KongCapability capability=new MahjongRegionalRules.KongCapability(
                Set.of(MahjongRegionalRules.KongType.EXPOSED),true,false);
        MahjongRegionalRules.KongResult replacement=capability.apply(MahjongRegionalRules.KongType.EXPOSED,claimed.wall());
        List<Integer>replacementHand=claimed.mutableHand(command.seatId());replacementHand.add(replacement.replacementTile());
        Map<Integer,List<Integer>>replacementHands=copyHands(claimed);replacementHands.put(command.seatId(),replacementHand);
        return claimed.replaceAll(replacement.wall(),replacementHands,command.seatId(),true,claimed.lastDiscard(),claimed.lastDiscardSeat(),
                MahjongOperationWindow.empty(),false,claimed.winnerSeat(),claimed.melds(),claimed.winners(),replacement.replacementTile(),claimed.openingSelections());
    }

    private MahjongState selfGang(MahjongState state, MahjongCommand command) {
        if (state.currentSeat() != command.seatId() || !state.currentSeatHasDrawn() || !state.window().isEmpty())
            throw new IllegalStateException("self gang is not allowed");
        int tile = command.tile();
        List<Integer> hand = state.mutableHand(command.seatId());
        int needed = command.gangType() == 4 ? 4 : 1;
        if (command.gangType() == 3 && state.melds().getOrDefault(command.seatId(), List.of()).stream()
                .noneMatch(m -> m.operation() == MahjongOperation.PENG && m.tiles().get(0) == tile))
            throw new IllegalStateException("peng meld is required for added gang");
        if (java.util.Collections.frequency(hand, tile) < needed) throw new IllegalStateException("gang tile is not in hand");
        for (int i = 0; i < needed; i++) hand.remove(Integer.valueOf(tile));
        Map<Integer, List<Integer>> hands = copyHands(state); hands.put(command.seatId(), hand);
        Map<Integer,List<MahjongState.Meld>> melds = new LinkedHashMap<>(state.melds());
        List<MahjongState.Meld> seatMelds = new ArrayList<>(melds.getOrDefault(command.seatId(), List.of()));
        if (command.gangType() == 3) {
            int index = -1;
            for (int i = 0; i < seatMelds.size(); i++) if (seatMelds.get(i).operation() == MahjongOperation.PENG
                    && seatMelds.get(i).tiles().get(0) == tile) { index = i; break; }
            MahjongState.Meld previous = seatMelds.get(index);
            seatMelds.set(index, new MahjongState.Meld(MahjongOperation.GANG, List.of(tile,tile,tile,tile), previous.sourceSeat()));
        } else seatMelds.add(new MahjongState.Meld(MahjongOperation.GANG, List.of(tile,tile,tile,tile), command.seatId()));
        melds.put(command.seatId(), seatMelds);
        if (state.wall().isEmpty()) return finishDraw(state.replaceAll(state.wall(),hands,command.seatId(),true,
                state.lastDiscard(),state.lastDiscardSeat(),MahjongOperationWindow.empty(),false,state.winnerSeat(),melds,
                state.winners(),0,state.openingSelections()));
        MahjongRegionalRules.KongType kind = command.gangType() == 4
                ? MahjongRegionalRules.KongType.CONCEALED : MahjongRegionalRules.KongType.ADDED;
        MahjongRegionalRules.KongResult replacement = new MahjongRegionalRules.KongCapability(Set.of(kind),true,false).apply(kind,state.wall());
        hand.add(replacement.replacementTile()); hands.put(command.seatId(),hand);
        return state.replaceAll(replacement.wall(),hands,command.seatId(),true,state.lastDiscard(),state.lastDiscardSeat(),
                MahjongOperationWindow.empty(),false,state.winnerSeat(),melds,state.winners(),replacement.replacementTile(),state.openingSelections());
    }

    private MahjongState win(MahjongState state, MahjongCommand command, MahjongRuleSet<MahjongState> rules) {
        boolean candidate = state.window().allows(command.seatId(), MahjongOperation.HU);
        boolean selfDraw = command.seatId() == state.currentSeat() && state.currentSeatHasDrawn();
        if (!candidate && !selfDraw) throw new IllegalStateException("hu is not allowed");
        int tile = selfDraw ? state.lastDrawnTile() : state.lastDiscard();
        if(selfDraw&&command.tile()!=0&&command.tile()!=tile)throw new IllegalStateException("hu tile is not the drawn tile");
        if (!rules.canWin(command.seatId(), state.hands().get(command.seatId()), tile, state)) throw new IllegalStateException("hand cannot win");
        java.util.LinkedHashSet<Integer>winners=new java.util.LinkedHashSet<>(state.winners());winners.add(command.seatId());
        boolean finished=!rules.continueAfterWin()||winners.size()>=state.hands().size()-1;
        int next=finished?state.currentSeat():nextActiveSeat(state,command.seatId(),winners);
        return state.replaceAll(state.wall(),state.hands(),next,finished&&state.currentSeatHasDrawn(),state.lastDiscard(),
                state.lastDiscardSeat(),MahjongOperationWindow.empty(),finished,command.seatId(),state.melds(),winners,0,state.openingSelections());
    }

    private static void requireTurn(MahjongState state, int seat) { if (state.currentSeat() != seat) throw new IllegalStateException("not current seat"); }
    private static Map<Integer, List<Integer>> copyHands(MahjongState state) { return new LinkedHashMap<>(state.hands()); }
    private static int nextActiveSeat(MahjongState state,int seat){return nextActiveSeat(state,seat,state.winners());}
    private static int nextActiveSeat(MahjongState state,int seat,Set<Integer>winners){List<Integer>seats=state.hands().keySet().stream().sorted().toList();int start=seats.indexOf(seat);for(int i=1;i<=seats.size();i++){int candidate=seats.get((start+i)%seats.size());if(!winners.contains(candidate))return candidate;}return seat;}
    private static MahjongState finishDraw(MahjongState state){return state.replaceAll(state.wall(),state.hands(),state.currentSeat(),false,state.lastDiscard(),state.lastDiscardSeat(),MahjongOperationWindow.empty(),true,state.winnerSeat(),state.melds(),state.winners(),0,state.openingSelections());}
}
