package com.aoo.bcg.poker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * XQP 跑得快服务端二进制已经证实的发牌、流程与结算规则快照。
 * 地区玩法只能在发布时装配此值对象，牌局中不允许按 gameId 或地区名分支。
 */
public record PdkAdvancedRules(int baseScore, int requiredFirstCardRounds,
        Integer bankerSelectionCard, boolean selectBankerEveryRound,
        ScoreRule spring, ScoreRule reverseSpring, List<HandScoreBand> handScoreTable,
        BombScore bombScore, DealerRule dealerRule, Set<InitialPattern> initialPatterns,
        Set<Integer> fourOfKindPatternRanks, int initialPatternLimit,
        int initialPatternScoreUnit, int jinHuaScoreUnit,
        List<Set<Integer>> directWinPatterns, int operationTimeoutSeconds,
        int hostingMissThreshold, RoomGovernance governance) {

    public enum ScoreMode { DISABLED, FIXED, HAND_TABLE }
    public enum BombMode { PAIRWISE, DISABLED, MULTIPLIER, FIXED_POINTS }
    public enum PayerMode { SPLIT, OWNER, PROPORTIONAL }
    public enum SettlementPresentation { FLOATING, POPUP }
    public enum EntryMode { PARTICIPANT, OBSERVER }
    public enum InitialPattern {
        FOUR_ACES, FOUR_CONFIGURED_RANK, ALL_SINGLES, FULL_STRAIGHT,
        FULL_CONSECUTIVE_PAIRS, ALL_PAIRS, ALL_BLACK, ALL_RED, ALL_BIG, ALL_SMALL
    }

    public record ScoreRule(ScoreMode mode, int value, int maxPlayedCards,
            boolean appliesToAllLosers) {
        public ScoreRule {
            Objects.requireNonNull(mode);
            if (value < 0 || maxPlayedCards < 0)
                throw new IllegalArgumentException("invalid spring score rule");
            if (mode != ScoreMode.DISABLED && value == 0)
                throw new IllegalArgumentException("enabled spring score must be positive");
        }

        public static ScoreRule disabled() {
            return new ScoreRule(ScoreMode.DISABLED, 0, Integer.MAX_VALUE, false);
        }
    }

    public record HandScoreBand(int minimumCards, int score) {
        public HandScoreBand {
            if (minimumCards < 0 || score < 0)
                throw new IllegalArgumentException("invalid hand score band");
        }
    }

    public record BombScore(BombMode mode, int cap, int points) {
        public BombScore {
            Objects.requireNonNull(mode);
            if (cap < 0 || points < 0)
                throw new IllegalArgumentException("invalid bomb score rule");
            if (mode == BombMode.MULTIPLIER && cap == 0)
                throw new IllegalArgumentException("bomb multiplier cap must be positive");
            if (mode == BombMode.FIXED_POINTS && points == 0)
                throw new IllegalArgumentException("fixed bomb points must be positive");
        }
    }

    /**
     * XQP type-119 的抢庄结算类型是独立于通用春天加倍的业务规则：抢庄者只有在
     * 所有闲家均未出牌时才能获胜；否则每位闲家向其取得一份抢庄分。
     */
    public record DealerRule(boolean enabled, boolean startAfterBanker,
            boolean bankerCannotCompeteAfterAllPass, boolean skipFirstRound,
            boolean mustSpringToWin) { }

    /** Platform-facing room policy proven by the same XQP server snapshot. */
    public record RoomGovernance(PayerMode payerMode, int offlineDissolveSeconds,
            boolean autoReady, int declaredRoundTimeoutSeconds,
            boolean gpsAdmissionRequired, int gpsMinimumDistanceMeters,
            boolean uniqueIpRequired,
            boolean textChatEnabled, SettlementPresentation settlementPresentation,
            EntryMode entryMode, List<Integer> carryInMultipliers,
            boolean distanceWarningEnabled, boolean interactionEnabled) {
        public RoomGovernance {
            Objects.requireNonNull(payerMode);
            Objects.requireNonNull(settlementPresentation);
            Objects.requireNonNull(entryMode);
            if (offlineDissolveSeconds < 0 || declaredRoundTimeoutSeconds < 1
                    || gpsMinimumDistanceMeters < 0 || gpsMinimumDistanceMeters > 100000)
                throw new IllegalArgumentException("invalid room governance timeout");
            ArrayList<Integer> multipliers = new ArrayList<>(Objects.requireNonNull(
                    carryInMultipliers));
            if (multipliers.stream().anyMatch(value -> value <= 0)
                    || multipliers.stream().distinct().count() != multipliers.size())
                throw new IllegalArgumentException("invalid carry-in multiplier choices");
            carryInMultipliers = List.copyOf(multipliers);
        }

        public static RoomGovernance defaults() {
            return new RoomGovernance(PayerMode.OWNER, 0, false, 1800, false, 0, false,
                    true, SettlementPresentation.POPUP, EntryMode.PARTICIPANT,
                    List.of(), false, true);
        }
    }

    public PdkAdvancedRules {
        if (baseScore <= 0) throw new IllegalArgumentException("baseScore must be positive");
        if (requiredFirstCardRounds < 0)
            throw new IllegalArgumentException("requiredFirstCardRounds cannot be negative");
        if (bankerSelectionCard != null) PokerCardCodec.validate(bankerSelectionCard);
        Objects.requireNonNull(spring);
        Objects.requireNonNull(reverseSpring);
        Objects.requireNonNull(bombScore);
        Objects.requireNonNull(dealerRule);
        if (initialPatternLimit < 0 || initialPatternScoreUnit < 0 || jinHuaScoreUnit < 0)
            throw new IllegalArgumentException("invalid initial hand scoring rule");
        if (operationTimeoutSeconds < 1 || operationTimeoutSeconds > 3600)
            throw new IllegalArgumentException("operation timeout must be 1..3600 seconds");
        if (hostingMissThreshold < -1 || hostingMissThreshold == 0)
            throw new IllegalArgumentException("hosting threshold must be -1 or positive");
        Objects.requireNonNull(governance);

        ArrayList<HandScoreBand> bands = new ArrayList<>(Objects.requireNonNull(handScoreTable));
        bands.sort(Comparator.comparingInt(HandScoreBand::minimumCards));
        if (bands.stream().map(HandScoreBand::minimumCards).distinct().count() != bands.size())
            throw new IllegalArgumentException("duplicate hand score threshold");
        handScoreTable = List.copyOf(bands);

        initialPatterns = Set.copyOf(Objects.requireNonNull(initialPatterns));
        LinkedHashSet<Integer> ranks = new LinkedHashSet<>(Objects.requireNonNull(
                fourOfKindPatternRanks));
        if (ranks.stream().anyMatch(rank -> rank < 3 || rank > 15))
            throw new IllegalArgumentException("invalid four-of-kind pattern rank");
        fourOfKindPatternRanks = Set.copyOf(ranks);
        if (!ranks.isEmpty() && !initialPatterns.contains(InitialPattern.FOUR_CONFIGURED_RANK))
            throw new IllegalArgumentException("configured four-of-kind ranks require pattern");

        ArrayList<Set<Integer>> direct = new ArrayList<>();
        for (Collection<Integer> pattern : Objects.requireNonNull(directWinPatterns)) {
            LinkedHashSet<Integer> cards = new LinkedHashSet<>(pattern);
            if (cards.isEmpty() || cards.size() != pattern.size())
                throw new IllegalArgumentException("invalid direct-win pattern");
            cards.forEach(PokerCardCodec::validate);
            direct.add(Set.copyOf(cards));
        }
        if (direct.stream().distinct().count() != direct.size())
            throw new IllegalArgumentException("duplicate direct-win pattern");
        directWinPatterns = List.copyOf(direct);
    }

    public int handScore(int cards) {
        if (cards < 0) throw new IllegalArgumentException("remaining cards cannot be negative");
        int score = 0;
        for (HandScoreBand band : handScoreTable) {
            if (cards < band.minimumCards()) break;
            score = band.score();
        }
        return handScoreTable.isEmpty() ? Math.max(1, cards) : score;
    }

    public static PdkAdvancedRules defaults() {
        return new PdkAdvancedRules(1, Integer.MAX_VALUE, null, false,
                new ScoreRule(ScoreMode.HAND_TABLE, 2, Integer.MAX_VALUE, true),
                new ScoreRule(ScoreMode.HAND_TABLE, 2, Integer.MAX_VALUE, false),
                List.of(), new BombScore(BombMode.PAIRWISE, 0, 1),
                new DealerRule(false, false, false, false, false), Set.of(), Set.of(),
                0, 3, 0, List.of(), 20, -1, RoomGovernance.defaults());
    }
}
