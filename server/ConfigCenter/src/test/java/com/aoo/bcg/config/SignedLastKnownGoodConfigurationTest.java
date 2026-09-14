package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class SignedLastKnownGoodConfigurationTest {
    @TempDir Path directory;
    @Test void fallbackRequiresRemoteFailureValidSignatureAndFreshSnapshot() throws Exception {
        Instant now = Instant.parse("2026-08-23T00:00:00Z");
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes();
        Path file = directory.resolve("last-good.json");
        var store = new SignedLastKnownGoodConfiguration(file, new ObjectMapper().findAndRegisterModules(),
            Clock.fixed(now, ZoneOffset.UTC), Duration.ofHours(1));
        var release = ConfigurationRelease.create("release-9", Map.of("runtime", "v9", "play.poker", "v2"), now);
        store.save(release, key);
        assertEquals(release, store.loadAfterRemoteFailure(new RuntimeException("remote unavailable"), key));
        assertThrows(IllegalArgumentException.class, () -> store.loadAfterRemoteFailure(null, key));
        byte[] tampered = Files.readAllBytes(file); tampered[tampered.length / 2] ^= 1; Files.write(file, tampered);
        assertThrows(IllegalStateException.class, () -> store.loadAfterRemoteFailure(new RuntimeException(), key));
    }
}
