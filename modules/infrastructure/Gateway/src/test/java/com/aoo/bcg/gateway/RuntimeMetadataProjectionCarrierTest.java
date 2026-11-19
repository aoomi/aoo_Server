package com.aoo.bcg.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeMetadataProjectionCarrierTest {
    @Test
    void productionGlobalCarriesOneProjectionAndRejectsConflicts() {
        RuntimeMetadataProjection projection = projection("0");
        RuntimeGameRoomRegistry installed = RuntimeGameRoomRegistry.configureGlobal(projection);

        assertSame(installed, RuntimeGameRoomRegistry.global());
        assertEquals(32, projection.rowCount());
        assertThrows(IllegalStateException.class,
                () -> RuntimeGameRoomRegistry.configureGlobal(projection("f")));
    }

    private static RuntimeMetadataProjection projection(String hashPrefix) {
        List<RuntimeMetadataProjection.Entry> rows = new ArrayList<>();
        for (int gameId = 1; gameId <= 32; gameId++) {
            rows.add(new RuntimeMetadataProjection.Row(gameId, "game" + gameId, "family" + gameId,
                    "play-v" + gameId, "region-" + gameId, "snapshot-v1", "card-v1",
                    "events-v1", "protocol-v1", "common-room-v1", String.format("%064x", gameId),
                    1000L + gameId, 2000L + gameId));
        }
        return new RuntimeMetadataProjection(
                "f".equals(hashPrefix) ? "f".repeat(64) : "0".repeat(64), "r9-v4", rows);
    }
}
