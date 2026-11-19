package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeConfigResolverTest {
    @Test void enforcesOneDeterministicPrecedenceOrder() {
        var resolver = new RuntimeConfigResolver(
            Map.of("room.timeout-ms", "1"), Map.of("ROOM_TIMEOUT_MS", "2"),
            Map.of("room.timeout-ms", "3", "db.pool-size", "30"),
            Map.of("room.timeout-ms", "4", "game.version", "v4"),
            Map.of("room.timeout-ms", "5", "feature.enabled", "false"));
        assertEquals("1", resolver.require("room.timeout-ms"));
        assertEquals(RuntimeConfigResolver.Origin.ARGUMENT, resolver.find("room.timeout-ms").orElseThrow().origin());
        assertEquals(RuntimeConfigResolver.Origin.FILE, resolver.find("db.pool-size").orElseThrow().origin());
        assertEquals(RuntimeConfigResolver.Origin.CONFIG_CENTER, resolver.find("game.version").orElseThrow().origin());
        assertEquals(RuntimeConfigResolver.Origin.DEFAULT, resolver.find("feature.enabled").orElseThrow().origin());
        assertThrows(IllegalStateException.class, () -> resolver.require("missing.value"));
        assertThrows(IllegalArgumentException.class, () -> resolver.find("Bad_Key"));
    }

    @Test void environmentWinsOverFileAndRemote() {
        var resolver = new RuntimeConfigResolver(Map.of(), Map.of("DB_POOL_SIZE", "20"),
            Map.of("db.pool-size", "10"), Map.of("db.pool-size", "8"), Map.of());
        assertEquals("20", resolver.require("db.pool-size"));
        assertEquals(RuntimeConfigResolver.Origin.ENVIRONMENT, resolver.find("db.pool-size").orElseThrow().origin());
    }
}
