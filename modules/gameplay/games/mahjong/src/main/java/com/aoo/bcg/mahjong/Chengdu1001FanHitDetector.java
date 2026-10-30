package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless Chengdu1001 fan hit detector.
 *
 * <p>Coverage is deliberately split into three groups: tile-visible hits are detected here; event/history
 * hits require {@link WinContext}; draw-audit and transfer rows (查叫、花猪、退雨、呼叫转移及杠分)
 * are outside the winning-hit surface and are never emitted. Mutual exclusion remains the responsibility
 * of {@link MahjongRegionalRules#scoreFans(List, Set, int)}; composite hits are emitted instead of their
 * component hits so the archived composite rules are not counted twice.
 *
 * <p>Tiles follow {@code rank = floorMod(tile, 10)} as in {@link StandardMahjongWinDetector}; terminals
 * are ranks 1 and 9. Chengdu1001 uses the Sichuan three-suit inventory, so honor/flower encodings are absent.
 */
public final class Chengdu1001FanHitDetector {
    private Chengdu1001FanHitDetector() {}

    record WinContext(boolean ziMo, boolean gangShangHua, boolean gangShangPao, boolean qiangGang,
            boolean haiDi, boolean menQing, boolean quanQiuYiHua) {
        static WinContext conservative(MahjongState state, int winnerSeat) {
            boolean selfDraw = state.currentSeatHasDrawn() && state.currentSeat() == winnerSeat;
            boolean concealed = state.melds().getOrDefault(winnerSeat, List.of()).stream()
                    .noneMatch(m -> m.operation() != MahjongOperation.GANG || m.sourceSeat() != winnerSeat);
            return new WinContext(selfDraw, false, false, false, false, concealed, false);
        }
    }

    public static Set<String> detect(MahjongState state, int winnerSeat, WinContext ctx) {
        if (state == null || ctx == null || !state.hands().containsKey(winnerSeat))
            throw new IllegalArgumentException("invalid chengdu1001 fan detection request");
        List<Integer> hand = state.hands().get(winnerSeat);
        List<MahjongState.Meld> melds = state.melds().getOrDefault(winnerSeat, List.of());
        ArrayList<Integer> all = new ArrayList<>(hand);
        melds.forEach(m -> all.addAll(m.tiles()));
        LinkedHashSet<String> hits = new LinkedHashSet<>();

        boolean qingYiSe = oneSuit(all);
        boolean sevenPairs = ctx.menQing() && melds.isEmpty() && sevenPairs(hand);
        boolean longQiDui = sevenPairs && counts(hand).values().stream().anyMatch(n -> n == 4);
        boolean duiDuiHu = allTripletsAndPair(hand, melds);
        boolean jiangYiSe = duiDuiHu && all.stream().allMatch(Chengdu1001FanHitDetector::isJiang);
        boolean qingYaoJiu = all.stream().allMatch(Chengdu1001FanHitDetector::isTerminal);
        boolean yaoJiu = !qingYaoJiu && allWithTerminals(hand, melds);

        if (qingYiSe && longQiDui) hits.add(Chengdu1001FanSchema.QING_YI_SE_LONG_QI_DUI);
        else if (qingYiSe && sevenPairs) hits.add(Chengdu1001FanSchema.QING_YI_SE_QI_DUI);
        else if (jiangYiSe) hits.add(Chengdu1001FanSchema.JIANG_YI_SE_DUI_DUI_HU);
        else if (qingYaoJiu) hits.add(Chengdu1001FanSchema.QING_YAO_JIU);
        else if (qingYiSe && duiDuiHu) hits.add(Chengdu1001FanSchema.QING_DUI);
        else {
            if (qingYiSe) hits.add(Chengdu1001FanSchema.QING_YI_SE);
            if (longQiDui) hits.add(Chengdu1001FanSchema.LONG_QI_DUI);
            else if (sevenPairs) hits.add(Chengdu1001FanSchema.QI_DUI); // #映射行3
            if (duiDuiHu) hits.add(Chengdu1001FanSchema.DUI_DUI_HU);
            if (yaoJiu) hits.add(Chengdu1001FanSchema.YAO_JIU);
        }

        if (isBasicPingHu(hand, melds, qingYiSe, sevenPairs, duiDuiHu, jiangYiSe, qingYaoJiu, yaoJiu))
            hits.add(Chengdu1001FanSchema.PING_HU);
        if (all.stream().noneMatch(Chengdu1001FanHitDetector::isTerminal)) hits.add(Chengdu1001FanSchema.ZHONG_ZHANG);
        if (melds.size() == 4 && hand.size() == 2) hits.add(Chengdu1001FanSchema.JIN_GOU_DIAO);
        if (edgeClosedOrPairWait(state, hand, ctx)) hits.add(Chengdu1001FanSchema.BIAN_KA_DIAO);
        if (ctx.menQing()) hits.add(Chengdu1001FanSchema.MEN_QING);
        if (suitCount(all) < 3) hits.add(Chengdu1001FanSchema.XUAN_QUE);
        if (hasStraightOneToNine(all)) hits.add(Chengdu1001FanSchema.YI_TIAO_LONG);

        boolean mingGui = false, anGui = false;
        for (MahjongState.Meld meld : melds) if (meld.operation() == MahjongOperation.GANG) {
            if (meld.sourceSeat() == winnerSeat) anGui = true; else mingGui = true;
        }
        if (mingGui || anGui) hits.add(Chengdu1001FanSchema.GANG_HAS_FAN);
        if (mingGui) hits.add(Chengdu1001FanSchema.MING_GUI);
        if (anGui) hits.add(Chengdu1001FanSchema.AN_GUI);

        // 需对局历史旗标，W2 接线面默认 false，待 W3 历史扩展。41-row schema has no GANG_SHANG_HUA code.
        if (ctx.gangShangPao()) hits.add(Chengdu1001FanSchema.GANG_SHANG_PAO);
        // 需对局历史旗标，W2 接线面默认 false，待 W3 历史扩展。抢杠/自抢杠归档到现有 schema code。
        if (ctx.qiangGang() && ctx.ziMo()) hits.add(Chengdu1001FanSchema.ZI_MO_ZI_QIANG_GANG);
        // 需对局历史旗标，W2 接线面默认 false，待 W3 历史扩展。全求人一花归档到金钩炮。
        if (ctx.quanQiuYiHua()) hits.add(Chengdu1001FanSchema.JIN_GOU_PAO);
        // 需对局历史旗标，W2 接线面默认 false，待 W3 历史扩展；HAI_DI_LAO/HE_DI_LAO 不在41-row schema。
        if (ctx.gangShangHua() || ctx.haiDi()) { /* authority flag retained without inventing an absent code */ }
        return Set.copyOf(hits);
    }

    private static boolean isBasicPingHu(List<Integer> hand, List<MahjongState.Meld> melds,
            boolean qingYiSe, boolean sevenPairs, boolean duiDuiHu, boolean jiangYiSe,
            boolean qingYaoJiu, boolean yaoJiu) {
        return !qingYiSe && !sevenPairs && !duiDuiHu && !jiangYiSe && !qingYaoJiu && !yaoJiu
                && melds.stream().noneMatch(m -> m.operation() == MahjongOperation.GANG)
                && new StandardMahjongWinDetector().isWinning(hand, 0);
    }

    private static boolean sevenPairs(List<Integer> hand) {
        if (hand.size() != 14) return false;
        Map<Integer,Integer> counts = counts(hand);
        return counts.values().stream().allMatch(n -> n == 2 || n == 4);
    }

    private static boolean allTripletsAndPair(List<Integer> hand, List<MahjongState.Meld> melds) {
        if (melds.stream().anyMatch(m -> m.operation() == MahjongOperation.CHI)) return false;
        if (melds.size() * 3 + hand.size() != 14) return false;
        Map<Integer,Integer> counts = counts(hand);
        int pairs = 0;
        for (int count : counts.values()) {
            if (count % 3 == 2) pairs++;
            else if (count % 3 != 0) return false;
        }
        return pairs == 1;
    }

    private static boolean allWithTerminals(List<Integer> hand, List<MahjongState.Meld> melds) {
        if (melds.stream().anyMatch(m -> m.tiles().stream().noneMatch(Chengdu1001FanHitDetector::isTerminal))) return false;
        return terminalMelds(counts(hand), false);
    }

    private static boolean terminalMelds(Map<Integer,Integer> counts, boolean pairUsed) {
        Integer tile = counts.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse(null);
        if (tile == null) return pairUsed;
        int count = counts.get(tile);
        if (!pairUsed && isTerminal(tile) && count >= 2) {
            take(counts, tile, 2);
            if (terminalMelds(counts, true)) return true;
            take(counts, tile, -2);
        }
        if (isTerminal(tile) && count >= 3) {
            take(counts, tile, 3);
            if (terminalMelds(counts, pairUsed)) return true;
            take(counts, tile, -3);
        }
        int rank = Math.floorMod(tile, 10);
        if ((rank == 1 || rank == 7) && counts.getOrDefault(tile + 1, 0) > 0 && counts.getOrDefault(tile + 2, 0) > 0) {
            take(counts, tile, 1); take(counts, tile + 1, 1); take(counts, tile + 2, 1);
            if (terminalMelds(counts, pairUsed)) return true;
            take(counts, tile, -1); take(counts, tile + 1, -1); take(counts, tile + 2, -1);
        }
        return false;
    }

    private static boolean edgeClosedOrPairWait(MahjongState state, List<Integer> hand, WinContext ctx) {
        int win = ctx.ziMo() ? state.lastDrawnTile() : state.lastDiscard();
        if (win <= 0 || !hand.contains(win)) return false;
        ArrayList<Integer> before = new ArrayList<>(hand);
        before.remove(Integer.valueOf(win));
        int rank = Math.floorMod(win, 10);
        return before.contains(win)
                || rank == 3 && before.contains(win - 2) && before.contains(win - 1)
                || rank == 7 && before.contains(win + 1) && before.contains(win + 2)
                || rank >= 2 && rank <= 8 && before.contains(win - 1) && before.contains(win + 1);
    }

    private static boolean oneSuit(List<Integer> tiles) { return !tiles.isEmpty() && suitCount(tiles) == 1; }
    private static int suitCount(List<Integer> tiles) { return (int)tiles.stream().map(t -> t / 10).distinct().count(); }
    private static boolean isTerminal(int tile) { int rank=Math.floorMod(tile,10);return rank==1||rank==9; }
    private static boolean isJiang(int tile) { int rank=Math.floorMod(tile,10);return rank==2||rank==5||rank==8; }
    private static boolean hasStraightOneToNine(List<Integer> tiles) {
        return tiles.stream().map(t -> t / 10).distinct().anyMatch(suit -> {
            for (int rank=1;rank<=9;rank++) if (!tiles.contains(suit*10+rank)) return false;
            return true;
        });
    }
    private static Map<Integer,Integer> counts(List<Integer> tiles){Map<Integer,Integer>out=new HashMap<>();tiles.forEach(t->out.merge(t,1,Integer::sum));return out;}
    private static void take(Map<Integer,Integer> counts,int tile,int amount){counts.put(tile,counts.get(tile)-amount);}
}
