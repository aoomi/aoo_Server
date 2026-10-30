package com.aoo.bcg.common.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/** Canonical ownership catalog for cross-module runtime settings. */
public enum RuntimeConfigKey {
    ENVIRONMENT("aoo.environment", Kind.TEXT, false),
    ADMIN_DB_URL("admin.db.url", Kind.ADDRESS, false),
    ADMIN_DB_USER("admin.db.user", Kind.TEXT, false),
    ADMIN_DB_PASSWORD("admin.db.password", Kind.SECRET, false),
    ADMIN_API_TOKEN("admin.api.token", Kind.SECRET, false),
    ADMIN_API_PORT("admin.api.port", Kind.INTEGER, false),
    ADMIN_MAP_PROVIDER_KEY("admin.map.provider-key", Kind.SECRET, false),
    ADMIN_MAP_TIMEOUT_MS("admin.map.timeout-ms", Kind.INTEGER, false),
    ADMIN_MAP_RATE_PER_MINUTE("admin.map.rate-per-minute", Kind.INTEGER, false),
    ADMIN_MAP_CACHE_SECONDS("admin.map.cache-seconds", Kind.INTEGER, false),
    ADMIN_DEV_OPERATOR_ID("admin.dev.operator-id", Kind.INTEGER, true),
    ADMIN_DEV_PERMISSIONS("admin.dev.permissions", Kind.TEXT, true),
    MQ_NAMESERVER("aoo.mq.namesrv-addr", Kind.ADDRESS, false),
    MQ_PRODUCER_GROUP("aoo.mq.producer-group", Kind.TEXT, false),
    MQ_GAME_PROFILE_TOPIC("aoo.mq.game-profile-topic", Kind.TEXT, false),
    FEATURE_LEGACY_PROTOCOL("feature.legacy-protocol", Kind.BOOLEAN, true);

    public enum Kind { ADDRESS, BOOLEAN, DURATION, INTEGER, SECRET, TEXT }
    public enum Mutability { RESTART_REQUIRED, ATOMIC_HOT_RELOAD }
    private final String canonicalName;
    private final Kind kind;
    private final boolean developmentOnly;

    RuntimeConfigKey(String canonicalName, Kind kind, boolean developmentOnly) {
        this.canonicalName = canonicalName;
        this.kind = kind;
        this.developmentOnly = developmentOnly;
    }
    public String canonicalName() { return canonicalName; }
    public String environmentName() { return canonicalName.replace('.', '_').replace('-', '_').toUpperCase(java.util.Locale.ROOT); }
    public Kind kind() { return kind; }
    public boolean developmentOnly() { return developmentOnly; }
    public Mutability mutability() { return Mutability.RESTART_REQUIRED; }

    public static void validateCatalog() {
        Set<String> names = new HashSet<>(), environments = new HashSet<>();
        for (RuntimeConfigKey key : values()) {
            if (!names.add(key.canonicalName) || !environments.add(key.environmentName()))
                throw new IllegalStateException("duplicate runtime configuration key: " + key.canonicalName);
        }
    }
}
