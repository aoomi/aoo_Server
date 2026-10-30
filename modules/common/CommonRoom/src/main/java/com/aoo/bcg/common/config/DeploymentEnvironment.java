package com.aoo.bcg.common.config;

import java.util.Locale;

public enum DeploymentEnvironment {
    DEVELOPMENT, TEST, STAGING, PRODUCTION;

    public static DeploymentEnvironment parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("aoo.environment must be explicit");
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new IllegalStateException("unsupported aoo.environment", error); }
    }
}
