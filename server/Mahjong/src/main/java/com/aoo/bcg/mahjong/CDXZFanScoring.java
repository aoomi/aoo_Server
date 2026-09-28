package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.List;

/**
 * CD101 winning-hand settlement. Draw audit is intentionally absent: see
 * {@link CDXZFanSchema#pendingAuthorityNotes()} — 依例外条款 e（待用户确认），流局结算面未接线。
 */
public final class CDXZFanScoring {
    private CDXZFanScoring() {}

    public static List<MahjongRegionalRules.SettlementLine> settleCd101(
            MahjongState state, int fengDingValue, int baseUnit) {
        if (state == null || baseUnit < 0) throw new IllegalArgumentException("invalid CD101 settlement request");
        ArrayList<MahjongRegionalRules.SettlementLine> lines = new ArrayList<>();
        for (int winner : state.winners()) {
            CDXZFanHitDetector.WinContext ctx = CDXZFanHitDetector.WinContext.conservative(state, winner);
            var matched = CDXZFanHitDetector.detect(state, winner, ctx);
            long cap = CDXZFanSchema.capFromFengDing(fengDingValue);
            // scoreFans has an int cap signature (MahjongRegionalRules.java:137-141); adapt without changing shared code.
            int capInt = cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)cap;
            int fan = MahjongRegionalRules.scoreFans(CDXZFanSchema.schema(), matched, capInt).cappedFan();
            long amount = Math.multiplyExact((long)fan, baseUnit);
            if (amount == 0) continue; // SettlementLine rejects non-positive points; the conserved zero result is an empty line set.
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
                    throw new IllegalStateException("invalid CD101 discard payer");
                lines.add(line(payer, winner, amount));
            }
        }
        return List.copyOf(lines);
    }

    private static MahjongRegionalRules.SettlementLine line(int payer, int receiver, long amount) {
        return new MahjongRegionalRules.SettlementLine(payer, receiver, amount, "WIN", "CDXZ_FAN_SCORING");
    }
}
