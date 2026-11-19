package com.aoo.bcg.families;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact score quantity; conversions never pass through floating point. */
public record ScoreAmount(long minorUnits, Unit unit) {
    public enum Unit {
        HU_XI(1), TUN(3), FAN(1), MULTIPLIER(1), BASE_STAKE(100);
        private final long scale;
        Unit(long scale) { this.scale = scale; }
        public long scale() { return scale; }
    }

    public ScoreAmount {
        if (unit == null) throw new IllegalArgumentException("score unit required");
    }

    public BigDecimal value() {
        return BigDecimal.valueOf(minorUnits).divide(BigDecimal.valueOf(unit.scale()), 8, RoundingMode.UNNECESSARY);
    }

    public ScoreAmount convertTo(Unit target) {
        if (target == null) throw new IllegalArgumentException("target unit required");
        long converted = Math.multiplyExact(minorUnits, target.scale()) / unit.scale();
        if (Math.multiplyExact(converted, unit.scale()) != Math.multiplyExact(minorUnits, target.scale()))
            throw new ArithmeticException("inexact score conversion");
        return new ScoreAmount(converted, target);
    }

    public ScoreAmount add(ScoreAmount other) {
        ScoreAmount converted = other.convertTo(unit);
        return new ScoreAmount(Math.addExact(minorUnits, converted.minorUnits), unit);
    }
}
