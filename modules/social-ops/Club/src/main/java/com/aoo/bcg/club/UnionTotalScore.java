package com.aoo.bcg.club;

import java.math.BigDecimal;
import java.math.BigInteger;

final class UnionTotalScore {
    static final long MIN = 0;
    static final long MAX = 999_999_999L;

    private UnionTotalScore() {}

    static long require(Object value) {
        if (value == null || value instanceof Float || value instanceof Double) throw invalid();
        String raw = String.valueOf(value).trim();
        if (!raw.matches("\\d+")) throw invalid();
        final BigInteger integer;
        try {
            BigDecimal decimal = value instanceof BigDecimal number ? number : new BigDecimal(raw);
            integer = decimal.toBigIntegerExact();
        } catch (RuntimeException failure) {
            throw invalid();
        }
        if (integer.compareTo(BigInteger.valueOf(MIN)) < 0 || integer.compareTo(BigInteger.valueOf(MAX)) > 0) throw invalid();
        return integer.longValueExact();
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("联盟总分必须是0到999999999的整数");
    }
}
