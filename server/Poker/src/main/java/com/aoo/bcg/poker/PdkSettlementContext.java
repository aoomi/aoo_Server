package com.aoo.bcg.poker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 算分策略的只读权威输入。构造时深拷贝所有集合，避免策略意外修改牌局状态，
 * 也从类型边界上阻止客户端提交的分数、轮次或赢家进入结算。
 */
public record PdkSettlementContext(long roomId, int roundNo, String playVersion,
        int winnerSeat, int initialLeadSeat, Map<Integer,Long> players,
        Map<Integer,List<Integer>> hands, Map<Integer,Integer> plays,
        Map<Integer,Integer> bombs, Map<Integer,Integer> playedCardCounts,
        Map<Integer,Integer> initialPatternCounts, int jinHuaWinnerSeat,
        int competeDealerSeat, PokerRuleProfile ruleProfile, PaoDeKuaiConfig config) {
    public PdkSettlementContext {
        players = Map.copyOf(players);
        Map<Integer,List<Integer>> copiedHands = new LinkedHashMap<>();
        hands.forEach((seat, cards) -> copiedHands.put(seat, List.copyOf(cards)));
        hands = Map.copyOf(copiedHands);
        plays = Map.copyOf(plays);
        bombs = Map.copyOf(bombs);
        playedCardCounts = Map.copyOf(playedCardCounts);
        initialPatternCounts = Map.copyOf(initialPatternCounts);
    }
}
