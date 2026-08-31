package com.aoo.bcg.common.config;

import java.util.Set;

/** Closed-by-default production policy applied after strict source binding. */
public final class ProductionConfigPolicy {
    private ProductionConfigPolicy() {}

    public static void verify(StrictRuntimeConfig config, Set<RuntimeConfigKey> requiredForService) {
        DeploymentEnvironment environment = DeploymentEnvironment.parse(config.require(RuntimeConfigKey.ENVIRONMENT));
        for (RuntimeConfigKey required : requiredForService) config.require(required);
        if (environment != DeploymentEnvironment.PRODUCTION) return;
        for (RuntimeConfigKey key : RuntimeConfigKey.values()) {
            if (key.developmentOnly() && config.has(key))
                throw new IllegalStateException("development-only configuration is forbidden in production: " + key.canonicalName());
        }
        if (config.has(RuntimeConfigKey.FEATURE_LEGACY_PROTOCOL)
                && Boolean.parseBoolean(config.require(RuntimeConfigKey.FEATURE_LEGACY_PROTOCOL)))
            throw new IllegalStateException("legacy protocol cannot be enabled in production");
    }
}
