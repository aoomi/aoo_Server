package com.aoo.bcg.poker;

import java.util.List;
import java.util.Map;

final class PdkLegalMoves {
    private PdkLegalMoves() {}

    @SuppressWarnings("unchecked")
    static List<CardCombination> hints(PokerAuthoritativeSession session) {
        Map<String,Object> authority = session.authoritativeState();
        PokerTurnState state = (PokerTurnState) authority.get("state");
        int seat = state.currentSeat();
        List<Integer> seats = state.hands().keySet().stream().sorted().toList();
        int next = seats.get((seats.indexOf(seat) + 1) % seats.size());
        int attempts = 0;
        while (state.passed().contains(next) && attempts++ < seats.size())
            next = seats.get((seats.indexOf(next) + 1) % seats.size());
        if (attempts >= seats.size() || state.passed().contains(next))
            throw new IllegalStateException("no active next seat");
        Map<Integer,Integer> plays = (Map<Integer,Integer>) authority.get("plays");
        boolean first = plays.values().stream().mapToInt(Integer::intValue).sum() == 0;
        Integer required = (Integer) authority.get("activeRequiredFirstCard");
        int dealer = ((Number) authority.get("competeDealerSeat")).intValue();
        List<Integer> hand = state.hands().get(seat);
        PaoDeKuaiContext context = new PaoDeKuaiContext(first,
                state.hands().get(next).size(), hand, required, true,
                state.previous() != null || seat == dealer);
        return session.family().rules().hints(hand, state.previous(), context);
    }
}
