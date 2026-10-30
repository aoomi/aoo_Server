package com.ddm.server.protocol.v2;

import com.ddm.server.common.redis.RedisUtil;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class ProtocolMigrationTelemetry {
    private static final int RETENTION_SECONDS = 45 * 24 * 60 * 60;

    private ProtocolMigrationTelemetry() {}

    public static void recordV1(String event) {
        recordObservedDay();
        RedisUtil.incrementWithTtl(dayKey("v1", LocalDate.now(ZoneOffset.UTC)), RETENTION_SECONDS);
        RedisUtil.incrementWithTtl("protocol:traffic:v1:event:" + safe(event), RETENTION_SECONDS);
    }

    public static void recordV2(String msgId) {
        recordObservedDay();
        RedisUtil.incrementWithTtl(dayKey("v2", LocalDate.now(ZoneOffset.UTC)), RETENTION_SECONDS);
        RedisUtil.incrementWithTtl("protocol:traffic:v2:message:" + safe(msgId), RETENTION_SECONDS);
    }

    /** Independent collection heartbeat; a quiet day must still be distinguishable from missing monitoring. */
    public static void observeToday() {
        recordObservedDay();
    }

    public static String dayKey(String version, LocalDate date) {
        return "protocol:traffic:" + version + ":day:" + date;
    }

    public static String observedDayKey(LocalDate date) {
        return "protocol:traffic:observed:day:" + date;
    }

    private static void recordObservedDay() {
        RedisUtil.setNxEx(observedDayKey(LocalDate.now(ZoneOffset.UTC)), "1", RETENTION_SECONDS);
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
