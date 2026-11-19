package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.List;

/**
 * chengdu1001 winning-hand settlement. Draw audit is intentionally absent: see
 * {@link Chengdu1001FanSchema#pendingAuthorityNotes()} — 依例外条款 e（待用户确认），流局结算面未接线。
 */
public final class Chengdu1001FanScoring {
    private Chengdu1001FanScoring() {}

    public static List<MahjongRegionalRules.SettlementLine> settleChengdu1001(
            MahjongState state, int fengDingValue, int baseUnit) {
        return settleChengdu1001(state, fengDingValue, baseUnit, 0);
    }

    public static List<MahjongRegionalRules.SettlementLine> settleChengdu1001(
            MahjongState state, int fengDingValue, int baseUnit, int addBase) {
        if (state == null || baseUnit < 0) throw new IllegalArgumentException("invalid chengdu1001 settlement request");
        ArrayList<MahjongRegionalRules.SettlementLine> lines = new ArrayList<>();
        for (int winner : state.winners()) {
            Chengdu1001FanHitDetector.WinContext ctx = Chengdu1001FanHitDetector.WinContext.conservative(state, winner);
            var matched = Chengdu1001FanHitDetector.detect(state, winner, ctx);
            long cap = Chengdu1001FanSchema.capFromFengDing(fengDingValue);
            // scoreFans has an int cap signature (MahjongRegionalRules.java:137-141); adapt without changing shared code.
            int capInt = cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)cap;
            int fan = MahjongRegionalRules.scoreFans(Chengdu1001FanSchema.schema(), matched, capInt).cappedFan();
            long fanAmount = fan >= 63 ? Long.MAX_VALUE : saturatedMultiply(1L << fan, baseUnit);
            long amount = Math.min(saturatedAdd(fanAmount, saturatedMultiply(addBase, baseUnit)), cap);
            if (amount <= 0) continue; // SettlementLine rejects non-positive points; the conserved zero result is an empty line set.
            if (ctx.ziMo()) {
                // 源: CDXZMJCalcPosEnd.java:314-345; MahjongRoom.java:2083-2117,2166-2178
                // 自摸：每个仍在本局的非赢家 payer 各付 amount 给该 winner。
                for (int payer : state.hands().keySet()) if (!state.winners().contains(payer))
                    lines.add(line(payer, winner, amount));
            } else {
                // 源: CDXZMJCalcPosEnd.java:284-305; MahjongRoom.java:2185-2189,2255-2262
                // 点炮：lastDiscardSeat 独付 amount 给该 winner；多赢家逐个独立应用同一式。
                int payer = state.lastDiscardSeat();
                if (!state.hands().containsKey(payer) || payer == winner)
                    throw new IllegalStateException("invalid chengdu1001 discard payer");
                lines.add(line(payer, winner, amount));
            }
        }
        return List.copyOf(lines);
    }

    private static long saturatedMultiply(long left, long right) {
        try { return Math.multiplyExact(left, right); } catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }

    private static long saturatedAdd(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }

    private static MahjongRegionalRules.SettlementLine line(int payer, int receiver, long amount) {
        return new MahjongRegionalRules.SettlementLine(payer, receiver, amount, "WIN", "chengdu1001_fan_scoring");
    }
}
