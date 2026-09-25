package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.SettlementPayload;
import java.util.LinkedHashMap;
import java.util.Map;

/** 算分差异扩展边界；输入只能来自不可变的服务端权威快照。 */
@FunctionalInterface
public interface PdkScoringPolicy {
    SettlementPayload settle(PdkSettlementContext context);

    static PdkScoringPolicy standard() {
        return context -> {
            PdkAdvancedRules rules = context.config().advancedRules();
            Map<Integer,Long> seats = new LinkedHashMap<>();
            context.players().keySet().forEach(seat -> seats.put(seat, 0L));
            applyInitialPatternScore(context, rules, seats);
            applyJinHuaScore(context, rules, seats);
            boolean competeDealer = rules.dealerRule().enabled()
                    && context.competeDealerSeat() >= 0;
            if (competeDealer)
                applyCompeteDealerScore(context, rules, seats);
            else
                applyNormalScore(context, rules, seats);
            if (!competeDealer || rules.bombScore().mode()
                    == PdkAdvancedRules.BombMode.FIXED_POINTS)
                applyBombTransfers(context, rules, seats);
            long checksum = seats.values().stream().mapToLong(Long::longValue).sum();
            if (checksum != 0) throw new IllegalStateException("PDK settlement is not zero-sum");
            Map<Long,Long> delta = new LinkedHashMap<>();
            context.players().forEach((seat, playerId) -> delta.put(playerId, seats.get(seat)));
            return new SettlementPayload(context.roomId(), context.roundNo(),
                    context.playVersion(), delta);
        };
    }

    private static void applyNormalScore(PdkSettlementContext context, PdkAdvancedRules rules,
            Map<Integer,Long> seats) {
        int winner = context.winnerSeat();
        int totalBombs = context.bombs().values().stream().mapToInt(Integer::intValue).sum();
        long winnerBase = 0;
        for (int seat : context.players().keySet()) {
            if (seat == winner) continue;
            int remaining = context.hands().get(seat).size();
            int plays = context.plays().getOrDefault(seat, 0);
            long score = (long) rules.handScore(remaining) * rules.baseScore();
            if (plays == 0 && rules.spring().mode() != PdkAdvancedRules.ScoreMode.DISABLED)
                score = score(rules.spring(), rules, remaining);
            else if (plays == 1 && rules.reverseSpring().mode()
                    != PdkAdvancedRules.ScoreMode.DISABLED
                    && (seat == context.initialLeadSeat()
                            || rules.reverseSpring().appliesToAllLosers())
                    && context.playedCardCounts().getOrDefault(seat, 0)
                            <= rules.reverseSpring().maxPlayedCards())
                score = score(rules.reverseSpring(), rules, remaining);
            if (rules.bombScore().mode() == PdkAdvancedRules.BombMode.MULTIPLIER
                    && totalBombs > 0) {
                int exponent = Math.min(totalBombs, Math.min(rules.bombScore().cap(), 30));
                score = Math.multiplyExact(score, 1L << exponent);
            }
            seats.merge(seat, -score, Long::sum);
            winnerBase = Math.addExact(winnerBase, score);
        }
        seats.merge(winner, winnerBase, Long::sum);
    }

    private static long score(PdkAdvancedRules.ScoreRule score, PdkAdvancedRules rules,
            int remainingCards) {
        return switch (score.mode()) {
            case DISABLED -> (long) rules.handScore(remainingCards) * rules.baseScore();
            case FIXED -> (long) score.value() * rules.baseScore();
            case HAND_TABLE -> (long) rules.handScore(remainingCards) * rules.baseScore()
                    * score.value();
        };
    }

    private static void applyCompeteDealerScore(PdkSettlementContext context,
            PdkAdvancedRules rules, Map<Integer,Long> seats) {
        int dealer = context.competeDealerSeat();
        int winner = context.winnerSeat();
        long unit = 3L * rules.baseScore();
        if (rules.spring().mode() == PdkAdvancedRules.ScoreMode.FIXED)
            unit = (long) rules.spring().value() * rules.baseScore();
        int totalBombs = context.bombs().values().stream().mapToInt(Integer::intValue).sum();
        if (rules.bombScore().mode() == PdkAdvancedRules.BombMode.MULTIPLIER
                && totalBombs > 0) {
            int exponent = Math.min(totalBombs, Math.min(rules.bombScore().cap(), 30));
            unit = Math.multiplyExact(unit, 1L << exponent);
        }
        long total = Math.multiplyExact(unit, context.players().size() - 1L);
        if (rules.dealerRule().mustSpringToWin() && !dealerMadeSpring(context, dealer)) {
            // XQP ends this round as soon as a non-dealer successfully plays. The robber
            // pays the complete table loss to that actual winner; uninvolved opponents do
            // not share it merely because they occupied a seat.
            seats.merge(dealer, -total, Long::sum);
            seats.merge(winner, total, Long::sum);
        } else if (winner == dealer) {
            for (int seat : context.players().keySet()) {
                if (seat == dealer) continue;
                seats.merge(seat, -unit, Long::sum);
                seats.merge(dealer, unit, Long::sum);
            }
        } else {
            seats.merge(dealer, -total, Long::sum);
            seats.merge(winner, total, Long::sum);
        }
    }

    private static boolean dealerMadeSpring(PdkSettlementContext context, int dealer) {
        if (context.winnerSeat() != dealer) return false;
        return context.players().keySet().stream()
                .filter(seat -> seat != dealer)
                .allMatch(seat -> context.plays().getOrDefault(seat, 0) == 0);
    }

    private static void applyBombTransfers(PdkSettlementContext context,
            PdkAdvancedRules rules, Map<Integer,Long> seats) {
        PdkAdvancedRules.BombMode mode = rules.bombScore().mode();
        if (mode != PdkAdvancedRules.BombMode.PAIRWISE
                && mode != PdkAdvancedRules.BombMode.FIXED_POINTS) return;
        long unit = mode == PdkAdvancedRules.BombMode.FIXED_POINTS
                ? Math.multiplyExact((long) rules.bombScore().points(), rules.baseScore())
                : rules.bombScore().points();
        var ordered = context.players().keySet().stream().sorted().toList();
        // Each bomb is paid for by every other seat, regardless of who won the hand.
        for (int first = 0; first < ordered.size(); first++)
            for (int second = first + 1; second < ordered.size(); second++) {
                int a = ordered.get(first), b = ordered.get(second);
                long difference = (long) context.bombs().getOrDefault(a, 0)
                        - context.bombs().getOrDefault(b, 0);
                long transfer = Math.multiplyExact(difference, unit);
                seats.merge(a, transfer, Long::sum);
                seats.merge(b, -transfer, Long::sum);
            }
    }

    private static void applyInitialPatternScore(PdkSettlementContext context,
            PdkAdvancedRules rules, Map<Integer,Long> seats) {
        if (rules.initialPatternLimit() == 0 || rules.initialPatternScoreUnit() == 0) return;
        int cap = rules.initialPatternLimit();
        long unit = (long) rules.initialPatternScoreUnit() * rules.baseScore();
        for (int first : context.players().keySet()) {
            int firstCount = Math.min(cap, context.initialPatternCounts().getOrDefault(first, 0));
            long score = 0;
            for (int second : context.players().keySet()) if (first != second) {
                int secondCount = Math.min(cap,
                        context.initialPatternCounts().getOrDefault(second, 0));
                score = Math.addExact(score, unit * (firstCount - secondCount));
            }
            seats.merge(first, score, Long::sum);
        }
    }

    private static void applyJinHuaScore(PdkSettlementContext context,
            PdkAdvancedRules rules, Map<Integer,Long> seats) {
        if (rules.jinHuaScoreUnit() == 0 || context.jinHuaWinnerSeat() < 0) return;
        int winner = context.jinHuaWinnerSeat();
        long unit = (long) rules.jinHuaScoreUnit() * rules.baseScore();
        for (int seat : context.players().keySet()) if (seat != winner) {
            seats.merge(seat, -unit, Long::sum);
            seats.merge(winner, unit, Long::sum);
        }
    }
}
