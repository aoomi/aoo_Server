package com.aoo.bcg.poker;

/** The six published legacy workbook shapes; behavior stays in LegacyPdkRegionRules. */
enum LegacyPdkVariant {
    A1_MINIMAL(false, false, false),
    A2_WITH_FOUR(true, true, true),
    A3_WITHOUT_FOUR(false, true, true),
    A4_WITHOUT_BOMB_SETTLEMENT(false, false, true),
    A5_WITHOUT_BOMB_SCORE(false, true, false),
    A6_SPECIAL_STRUCTURE(true, true, true);

    private final boolean fourWith;
    private final boolean bombSettlement;
    private final boolean bombScore;

    LegacyPdkVariant(boolean fourWith, boolean bombSettlement, boolean bombScore) {
        this.fourWith = fourWith;
        this.bombSettlement = bombSettlement;
        this.bombScore = bombScore;
    }

    boolean hasFourWith() { return fourWith; }
    boolean hasBombSettlement() { return bombSettlement; }
    boolean hasBombScore() { return bombScore; }
}
