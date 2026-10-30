package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

final class ProviderCatalogAssemblerRuntimeMetadataTest {
    @Test
    void productionAssemblerRequiresAnExplicitReleaseProjection() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ProviderCatalogAssembler.production((RuntimeMetadataProjectionSource) null));
        assertEquals("RUNTIME_METADATA_PROJECTION_REQUIRED", error.getMessage());
    }

    @Test
    void identityValidatorConsumesTheSourceAndFailsClosedBeforeBinding() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeMetadataProjectionSource source = () -> {
            calls.incrementAndGet();
            return projection();
        };

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuntimeIdentityValidator.validateProduction(GameRegistryFactory.production(), source));
        assertEquals("RUNTIME_METADATA_PROJECTION_REGISTRY_INCOMPLETE currentCount=0 expected=32",
                error.getMessage());
        assertEquals(1, calls.get());
    }

    private static RuntimeMetadataProjection projection() {
        List<RuntimeMetadataProjection.Entry> rows = new ArrayList<>();
        for (int gameId = 1; gameId <= 32; gameId++) {
            rows.add(new RuntimeMetadataProjection.Row(gameId, "game" + gameId, "family" + gameId,
                    "play-v" + gameId, "region-" + gameId, "snapshot-v1", "card-v3",
                    "events-v4", "protocol-v2", "common-room-v1", String.format("%064x", gameId),
                    1000L + gameId, 2000L + gameId));
        }
        return new RuntimeMetadataProjection(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "r9-v4", rows);
    }
}
