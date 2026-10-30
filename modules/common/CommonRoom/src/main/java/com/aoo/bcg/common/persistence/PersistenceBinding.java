package com.aoo.bcg.common.persistence;

import java.util.Objects;

public record PersistenceBinding(
        PersistenceCapability capability,
        String implementation,
        boolean durable,
        boolean distributed
) {
    public PersistenceBinding {
        Objects.requireNonNull(capability, "capability");
        if (implementation == null || implementation.isBlank()) {
            throw new IllegalArgumentException("implementation must not be blank");
        }
    }

    public static PersistenceBinding inMemory(PersistenceCapability capability, Class<?> implementation) {
        return new PersistenceBinding(capability, implementation.getName(), false, false);
    }

    public static PersistenceBinding production(PersistenceCapability capability, Class<?> implementation) {
        return new PersistenceBinding(capability, implementation.getName(), true, true);
    }
}
