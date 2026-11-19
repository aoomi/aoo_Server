package com.aoo.bcg.families;

import java.util.Locale;

/** Stable, category-qualified card identity used at persistence and replay boundaries. */
public record CanonicalCard(Family family, int suit, int rank, int copy) {
    public enum Family { MAHJONG, POKER, LONG_CARD, WORD_CARD }

    public CanonicalCard {
        if (family == null || suit < 0 || rank < 0 || copy < 0)
            throw new IllegalArgumentException("invalid canonical card");
        switch (family) {
            case MAHJONG -> require(suit <= 6 && rank <= 9 && copy <= 3);
            case POKER -> require(suit <= 4 && rank <= 14 && copy == 0);
            case LONG_CARD -> require(suit <= 3 && rank <= 10 && copy <= 3);
            case WORD_CARD -> require(suit <= 1 && rank >= 1 && rank <= 10 && copy <= 3);
        }
    }

    public String value() {
        return family.name().toLowerCase(Locale.ROOT) + ":" + suit + ":" + rank + ":" + copy;
    }

    public static CanonicalCard parse(String value) {
        if (value == null) throw new IllegalArgumentException("card value required");
        String[] parts = value.split(":", -1);
        if (parts.length != 4) throw new IllegalArgumentException("invalid canonical card: " + value);
        try {
            return new CanonicalCard(Family.valueOf(parts[0].toUpperCase(Locale.ROOT)),
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("invalid canonical card: " + value, error);
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("card coordinates outside family range");
    }
}
