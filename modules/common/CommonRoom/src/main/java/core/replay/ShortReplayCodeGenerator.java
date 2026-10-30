package core.replay;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Generates fixed-width numeric codes without deriving them from legacy identifiers. */
public final class ShortReplayCodeGenerator {
    private final RandomGenerator random;
    public ShortReplayCodeGenerator() { this(new SecureRandom()); }
    public ShortReplayCodeGenerator(RandomGenerator random) { this.random = Objects.requireNonNull(random); }
    public String next(int length) {
        if (length < 6 || length > 8) throw new IllegalArgumentException("short replay code length must be 6..8");
        int lower = power10(length - 1), upper = power10(length);
        return Integer.toString(random.nextInt(lower, upper));
    }
    public static long capacity(int length) {
        if (length < 6 || length > 8) throw new IllegalArgumentException("short replay code length must be 6..8");
        return 9L * power10(length - 1);
    }
    private static int power10(int exponent) { int value=1; for(int i=0;i<exponent;i++) value*=10; return value; }
}
