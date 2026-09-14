package com.aoo.bcg.common.readiness;

import java.util.Objects;

/** A named startup dependency check. */
public record ReadinessCheck(String name, CheckedAction action) {
    public ReadinessCheck {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("readiness check name is blank");
        Objects.requireNonNull(action, "action");
    }

    @FunctionalInterface
    public interface CheckedAction {
        void verify() throws Exception;
    }
}
