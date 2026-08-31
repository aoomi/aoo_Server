package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source-backed Anyue/Ziyang PDK rule snapshot. */
final class AypdkRules {
    static final int SPADE_THREE = PokerCardCodec.encode(4, 3);

    private AypdkRules() {}

    static PaoDeKuaiFamily family() { return family(AypdkOptions.from(java.util.Map.of())); }

    static PaoDeKuaiFamily family(AypdkOptions options) {
        Integer mandatory = options.heitaosanbichu() == 0 && options.chupai() != 2 ? SPADE_THREE : null;
        PaoDeKuaiConfig config = new PaoDeKuaiConfig(5, options.paixing().contains(2), false, false,
                mandatory, true, 14, options.teshu().contains(0), options.paixing().contains(5),
                1, 2, 1, options.paixing().contains(0));
        PokerRuleProfile.FirstLead lead = options.chupai() == 2 ? PokerRuleProfile.FirstLead.RANDOM
                : PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER;
        PokerRuleProfile profile = new PokerRuleProfile(AypdkGameProvider.VERSION, 48, 3, 3,
                lead, lead == PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER ? SPADE_THREE : null,
                5, options.paixing().contains(4) ? 2 : 99, false, false, true, true,
                options.zhadan() == 2 ? 10 : options.zhadan() == 1 ? 1 : 0,
                options.zhadan() == 1 ? 4 : 64, 2, 16, legacyDeck());
        return new PaoDeKuaiFamily(config, profile);
    }

    /**
     * 地方差异只在玩法发布时装配成不可变策略。公共房间只调用策略接口，避免按地区或 gameId
     * 分支后出现第二套状态机，也保证恢复时使用与 playVersion 完全一致的规则快照。
     */
    static PdkVariantPolicy policy(AypdkOptions options) {
        PaoDeKuaiFamily family = family(options);
        PdkCardPatternPolicy cardPolicy = (request, combination, context) -> {
            boolean legal = switch (combination.type()) {
                case "TRIPLE" -> options.paixing().contains(0)
                        && combination.cards().size() == context.handBeforePlay().size();
                case "TRIPLE_WITH_ONE" -> options.paixing().contains(1);
                case "TRIPLE_WITH_PAIR" -> options.paixing().contains(2);
                case "CONSECUTIVE_PAIRS" -> options.paixing().contains(4);
                case "FOUR_BOMB_WITH_ONE" -> options.paixing().contains(5);
                default -> true;
            };
            if (!legal) throw new IllegalArgumentException("card type disabled by variant policy");
        };
        PdkScoringPolicy scoringPolicy = context -> {
            Map<Integer,Long> seatDelta = new LinkedHashMap<>();
            context.players().keySet().forEach(seat -> seatDelta.put(seat, 0L));
            int bigGate = options.daxiaoguan() == 0 ? 50 : 30;
            int smallGate = options.daxiaoguan() == 0 ? 30 : 20;
            int totalBombs = context.bombs().values().stream().mapToInt(Integer::intValue).sum();
            int multiplier = options.zhadan() == 1 ? Math.min(4, 1 << Math.min(totalBombs, 2)) : 1;
            long winnerScore = 0;
            for (int seat : context.players().keySet()) if (seat != context.winnerSeat()) {
                int playCount = context.plays().getOrDefault(seat, 0);
                long loss = (playCount == 0 ? bigGate : playCount == 1 ? smallGate
                        : Math.max(1, context.hands().get(seat).size())) * multiplier;
                seatDelta.put(seat, -loss);
                winnerScore += loss;
            }
            seatDelta.put(context.winnerSeat(), winnerScore);
            if (options.zhadan() == 2) for (int first : context.players().keySet())
                for (int second : context.players().keySet()) if (first < second) {
                    long transfer = (long) (context.bombs().getOrDefault(first, 0)
                            - context.bombs().getOrDefault(second, 0)) * 10;
                    seatDelta.merge(first, transfer, Long::sum);
                    seatDelta.merge(second, -transfer, Long::sum);
                }
            Map<Long,Long> delta = new LinkedHashMap<>();
            context.players().forEach((seat, player) -> delta.put(player, seatDelta.get(seat)));
            return new com.aoo.bcg.gamespi.SettlementPayload(context.roomId(), context.roundNo(),
                    context.playVersion(), delta);
        };
        return new PdkVariantPolicy(AypdkGameProvider.VERSION, family, cardPolicy, scoringPolicy);
    }

    /** Legacy AYPDK removes one ace and three twos, rather than all four threes. */
    static List<Integer> legacyDeck() {
        List<Integer> deck = new ArrayList<>(PokerCardCodec.deck(52));
        deck.removeAll(List.of(PokerCardCodec.encode(1, 14), PokerCardCodec.encode(1, 15),
                PokerCardCodec.encode(2, 15), PokerCardCodec.encode(3, 15)));
        return List.copyOf(deck);
    }
}
