package com.ddm.server.protocol.v2;

import com.ddm.server.common.redis.RedisUtil;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class ProtocolRetirementGate {
    public record Result(boolean allowed, List<String> blockers) {
        public Result { blockers = List.copyOf(blockers); }
    }

    private ProtocolRetirementGate() {}

    public static Result evaluate() {
        List<String> blockers = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int offset = 0; offset < 30; offset++) {
            if (!RedisUtil.exists(ProtocolMigrationTelemetry.observedDayKey(today.minusDays(offset)))) {
                blockers.add("Missing protocol observation on " + today.minusDays(offset));
                continue;
            }
            String key = ProtocolMigrationTelemetry.dayKey("v1", today.minusDays(offset));
            String value = RedisUtil.get(key);
            if (value != null && !value.isBlank() && Long.parseLong(value) > 0L) {
                blockers.add("V1 traffic exists on " + today.minusDays(offset));
            }
        }
        String activeRooms = RedisUtil.get("protocol:v1:active-rooms");
        if (activeRooms != null && !activeRooms.isBlank() && Long.parseLong(activeRooms) > 0L) {
            blockers.add("V1 active rooms remain: " + activeRooms);
        }
        if (!"passed".equalsIgnoreCase(RedisUtil.get("protocol:v1:rollback-drill"))) {
            blockers.add("V1 rollback drill has not passed");
        }
        return new Result(blockers.isEmpty(), List.copyOf(blockers));
    }
}
