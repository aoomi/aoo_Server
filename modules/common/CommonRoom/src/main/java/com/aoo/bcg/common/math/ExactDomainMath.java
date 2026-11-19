package com.aoo.bcg.common.math;

/** Checked arithmetic for persisted identities, revisions, rounds, counters and scores. */
public final class ExactDomainMath {
    private ExactDomainMath() {}
    public static long increment(long value, String domain) {
        try { return Math.incrementExact(value); }
        catch (ArithmeticException overflow) { throw overflow(domain, overflow); }
    }
    public static int increment(int value, String domain) {
        try { return Math.incrementExact(value); }
        catch (ArithmeticException overflow) { throw overflow(domain, overflow); }
    }
    public static long add(long left, long right, String domain) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException overflow) { throw overflow(domain, overflow); }
    }
    public static long requirePositiveId(long value, String domain) {
        if (value <= 0) throw new IllegalStateException(domain + " generator returned a non-positive id");
        return value;
    }
    private static ArithmeticException overflow(String domain, ArithmeticException cause) {
        ArithmeticException failure = new ArithmeticException(domain + " overflow");
        failure.initCause(cause); return failure;
    }
}
