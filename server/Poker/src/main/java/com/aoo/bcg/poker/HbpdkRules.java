package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HbpdkRules {
    static final int HEART3 = PokerCardCodec.fromLegacyNibble(0x23);
    static final int SPADE3 = PokerCardCodec.fromLegacyNibble(0x33);
    private static final List<Integer> BASE_DECK = baseDeck();

    private HbpdkRules() {}

    static PaoDeKuaiFamily family(HbpdkOptions o) {
        int first = o.xianchu() < 2 ? HEART3 : SPADE3;
        PaoDeKuaiConfig c = new PaoDeKuaiConfig(5, false, false, false, first, true, 14,
                o.kexuan().contains(4), false, 1, 2, 1, o.kexuan().contains(13));
        PokerRuleProfile p = new PokerRuleProfile(HbpdkGameProvider.VERSION, 48, 2, 3,
                PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER, first, 5, 2, false, false,
                true, true, o.bombScore() == 1 ? 10 : 1, 64, 2, 24, BASE_DECK);
        return new PaoDeKuaiFamily(c, p);
    }

    /** 算分差异由独立策略消费只读权威快照，客户端提交的分数或牌型不会进入计算。 */
    static PdkVariantPolicy policy(HbpdkOptions options) {
        PaoDeKuaiFamily family = family(options);
        PdkScoringPolicy scoringPolicy = context -> {
            Map<Integer,Long> seatDelta = new LinkedHashMap<>();
            context.players().keySet().forEach(seat -> seatDelta.put(seat, 0L));
            int winner = context.winnerSeat();
            for (int seat : context.players().keySet()) if (seat != winner) {
                long loss = Math.max(1, context.hands().get(seat).size());
                seatDelta.merge(seat, -loss, Long::sum);
                seatDelta.merge(winner, loss, Long::sum);
            }
            if (options.bombAlgorithm() != 2) for (int seat : context.players().keySet()) {
                if (options.bombAlgorithm() == 1 && seat != winner) continue;
                for (int count = 0; count < context.bombs().getOrDefault(seat, 0); count++)
                    for (int other : context.players().keySet()) if (other != seat) {
                        long transfer = options.bombScore() == 1 ? 10
                                : Math.max(1, Math.abs(seatDelta.get(other)));
                        seatDelta.merge(seat, transfer, Long::sum);
                        seatDelta.merge(other, -transfer, Long::sum);
                    }
            }
            Map<Long,Long> delta = new LinkedHashMap<>();
            context.players().forEach((seat, player) -> delta.put(player, seatDelta.get(seat)));
            return new com.aoo.bcg.gamespi.SettlementPayload(context.roomId(), context.roundNo(),
                    context.playVersion(), delta);
        };
        return new PdkVariantPolicy(HbpdkGameProvider.VERSION, family,
                PdkCardPatternPolicy.standard(), scoringPolicy);
    }

    static boolean excludedFromFifteenCardDeck(int card) {
        return card == PokerCardCodec.fromLegacyNibble(0x1e)
                || card == PokerCardCodec.fromLegacyNibble(0x2e)
                || card == PokerCardCodec.fromLegacyNibble(0x0d);
    }

    private static List<Integer> baseDeck() {
        List<Integer> deck = new ArrayList<>(52);
        for (int suit = 1; suit <= 4; suit++) {
            for (int rank = 3; rank <= 15; rank++) deck.add(PokerCardCodec.encode(suit, rank));
        }
        deck.remove(Integer.valueOf(PokerCardCodec.fromLegacyNibble(0x0e)));
        deck.remove(Integer.valueOf(PokerCardCodec.fromLegacyNibble(0x0f)));
        deck.remove(Integer.valueOf(PokerCardCodec.fromLegacyNibble(0x1f)));
        deck.remove(Integer.valueOf(PokerCardCodec.fromLegacyNibble(0x2f)));
        return List.copyOf(deck);
    }
}
