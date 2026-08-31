package com.aoo.bcg.common.config;

import java.util.Arrays;

/** Short-lived secret material that is wiped when use completes. */
public final class SecretMaterial implements AutoCloseable {
    private final char[] value;
    public SecretMaterial(char[] value) {
        if (value == null || value.length == 0) throw new IllegalArgumentException("empty secret material");
        this.value = value.clone();
    }
    public char[] copy() { return value.clone(); }
    @Override public void close() { Arrays.fill(value, '\0'); }
}
