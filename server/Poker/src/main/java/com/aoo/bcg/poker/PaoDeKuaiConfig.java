package com.aoo.bcg.poker;

import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable card-rule snapshot shared by every regional PDK policy. */
public record PaoDeKuaiConfig(int minimumStraightLength, boolean allowTripleWithPair,
        boolean allowFourWithTwo, boolean allowFourWithThree,
        Integer requiredFirstCard, boolean forceHighestSingleAgainstReportedSingle,
        Integer specialTripleBombRank, boolean allowSpecialTripleBombWithOne,
        boolean allowFourBombWithOne, int standardBombTier, int specialBombTier,
        int fourBombWithOneTier, boolean allowTerminalAttachmentShortage,
        int minimumPairRunLength, AttachmentMode tripleAttachmentMode,
        AttachmentMode airplaneAttachmentMode, AttachmentMode fourAttachmentMode,
        PlayTiming tripleWithoutAttachmentTiming, PlayTiming airplaneWithoutAttachmentTiming,
        boolean allowAirplaneWithTwo, boolean compareTripleAttachments,
        boolean forceHighestPairAgainstReportedPair, boolean allowSingle, boolean allowPair,
        int cardsPerPlayer, boolean allowConsecutiveBomb,
        Set<Integer> specialTripleBombRanks, PlayedCardVisibility playedCardVisibility,
        PdkAdvancedRules advancedRules) {

    public enum AttachmentMode { DISABLED, SINGLES, PAIRS, EITHER }
    public enum PlayTiming { DISABLED, FINAL_ONLY, ANYTIME }
    public enum PlayedCardVisibility { LAST_ONLY, ALL_IN_ORDER }

    public PaoDeKuaiConfig(int minimumStraightLength, boolean allowTripleWithPair,
            boolean allowFourWithTwo, boolean allowFourWithThree, Integer requiredFirstCard,
            boolean forceHighestSingleAgainstReportedSingle) {
        this(minimumStraightLength, allowTripleWithPair, allowFourWithTwo, allowFourWithThree,
                requiredFirstCard, forceHighestSingleAgainstReportedSingle, null, false, false,
                1, 2, 1, false);
    }

    /** Compatibility constructor for existing published variants. */
    public PaoDeKuaiConfig(int minimumStraightLength, boolean allowTripleWithPair,
            boolean allowFourWithTwo, boolean allowFourWithThree, Integer requiredFirstCard,
            boolean forceHighestSingleAgainstReportedSingle, Integer specialTripleBombRank,
            boolean allowSpecialTripleBombWithOne, boolean allowFourBombWithOne,
            int standardBombTier, int specialBombTier, int fourBombWithOneTier,
            boolean allowTerminalAttachmentShortage) {
        this(minimumStraightLength, allowTripleWithPair, allowFourWithTwo, allowFourWithThree,
                requiredFirstCard, forceHighestSingleAgainstReportedSingle, specialTripleBombRank,
                allowSpecialTripleBombWithOne, allowFourBombWithOne, standardBombTier,
                specialBombTier, fourBombWithOneTier, allowTerminalAttachmentShortage, 2,
                allowTripleWithPair ? AttachmentMode.EITHER : AttachmentMode.SINGLES,
                allowTripleWithPair ? AttachmentMode.EITHER : AttachmentMode.SINGLES,
                allowFourWithTwo ? AttachmentMode.SINGLES : AttachmentMode.DISABLED,
                PlayTiming.ANYTIME, PlayTiming.ANYTIME, allowTripleWithPair, false, false,
                true, true, 0, false,
                specialTripleBombRank == null ? Set.of() : Set.of(specialTripleBombRank),
                PlayedCardVisibility.LAST_ONLY, PdkAdvancedRules.defaults());
    }

    public PaoDeKuaiConfig {
        if (minimumStraightLength < 3 || minimumStraightLength > 12)
            throw new IllegalArgumentException("straight length must be between three and twelve");
        if (minimumPairRunLength < 2 || minimumPairRunLength > 6)
            throw new IllegalArgumentException("pair run length must be between two and six");
        if (specialTripleBombRank != null
                && (specialTripleBombRank < 3 || specialTripleBombRank > 15))
            throw new IllegalArgumentException("invalid special bomb rank");
        if (standardBombTier <= 0 || specialBombTier <= 0 || fourBombWithOneTier <= 0)
            throw new IllegalArgumentException("bomb tiers must be positive");
        Objects.requireNonNull(tripleAttachmentMode);
        Objects.requireNonNull(airplaneAttachmentMode);
        Objects.requireNonNull(fourAttachmentMode);
        Objects.requireNonNull(tripleWithoutAttachmentTiming);
        Objects.requireNonNull(airplaneWithoutAttachmentTiming);
        Objects.requireNonNull(playedCardVisibility);
        Objects.requireNonNull(advancedRules);
        LinkedHashSet<Integer> bombRanks = new LinkedHashSet<>(Objects.requireNonNull(specialTripleBombRanks));
        if (specialTripleBombRank != null) bombRanks.add(specialTripleBombRank);
        if (bombRanks.stream().anyMatch(rank -> rank < 3 || rank > 15))
            throw new IllegalArgumentException("invalid special bomb rank set");
        specialTripleBombRanks = Set.copyOf(bombRanks);
        if (cardsPerPlayer < 0 || cardsPerPlayer > 54)
            throw new IllegalArgumentException("invalid cards per player");
        if (allowTripleWithPair != (tripleAttachmentMode == AttachmentMode.PAIRS
                || tripleAttachmentMode == AttachmentMode.EITHER))
            throw new IllegalArgumentException("triple attachment compatibility flag conflicts with mode");
        if (allowFourWithTwo != (fourAttachmentMode != AttachmentMode.DISABLED))
            throw new IllegalArgumentException("four attachment compatibility flag conflicts with mode");
        if (allowSpecialTripleBombWithOne && specialTripleBombRanks.isEmpty())
            throw new IllegalArgumentException("special bomb ranks required");
    }

    public static PaoDeKuaiConfig defaults() {
        return new PaoDeKuaiConfig(5, true, true, false, null, true);
    }

    public PaoDeKuaiConfig withCardsPerPlayer(int value) {
        return new PaoDeKuaiConfig(minimumStraightLength, allowTripleWithPair,
                allowFourWithTwo, allowFourWithThree, requiredFirstCard,
                forceHighestSingleAgainstReportedSingle, specialTripleBombRank,
                allowSpecialTripleBombWithOne, allowFourBombWithOne, standardBombTier,
                specialBombTier, fourBombWithOneTier, allowTerminalAttachmentShortage,
                minimumPairRunLength, tripleAttachmentMode, airplaneAttachmentMode,
                fourAttachmentMode, tripleWithoutAttachmentTiming,
                airplaneWithoutAttachmentTiming, allowAirplaneWithTwo,
                compareTripleAttachments, forceHighestPairAgainstReportedPair,
                allowSingle, allowPair, value, allowConsecutiveBomb,
                specialTripleBombRanks, playedCardVisibility, advancedRules);
    }
}
