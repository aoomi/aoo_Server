package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

final class RuntimeMetadataProjectionContractTest {
    @Test
    void publicApiCannotConstructOrPromoteTrustedProjection() {
        RuntimeMetadataProjection projection = projection();
        assertEquals(RuntimeMetadataProjection.Trust.UNVERIFIED, projection.trust());
        assertFalse(java.util.Arrays.stream(RuntimeMetadataProjection.class.getMethods())
                .anyMatch(method -> method.getName().equals("signedCanonical")
                        || method.getName().equals("verifiedLocalMirror")
                        || method.getName().equals("trusted")));
        assertFalse(java.util.Arrays.stream(RuntimeMetadataProjection.class.getConstructors())
                .anyMatch(constructor -> java.util.Arrays.asList(constructor.getParameterTypes())
                        .contains(RuntimeMetadataProjection.Trust.class)));
    }
    @Test
    void projectionCarriesTheSignedEnvelopeAndAllEightFields() {
        RuntimeMetadataProjection projection = projection();

        assertEquals(32, projection.rowCount());
        assertEquals("r9-v4", projection.edition());
        assertEquals(RuntimeMetadataProjection.REQUIRED_FIELDS.size(), projection.require(1).fields().size());
        assertEquals(RuntimeMetadataProjection.REQUIRED_FIELDS.stream().sorted().toList(),
                projection.require(1).fields().keySet().stream().sorted().toList());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                projection.require(1).sourceHash());
        assertEquals("common-room-v1", projection.require(1).contractVersion());
        assertEquals(32, projection.manifestEnvelope().get("rowCount"));
    }

    @Test
    void incompleteAndDuplicateRowsFailClosed() {
        List<RuntimeMetadataProjection.Entry> incomplete = new ArrayList<>(projection().rows());
        incomplete.remove(incomplete.size() - 1);
        assertThrows(IllegalArgumentException.class,
                () -> new RuntimeMetadataProjection(projection().manifestSha256(), "r9-v4", incomplete));

        List<RuntimeMetadataProjection.Entry> duplicate = new ArrayList<>(projection().rows());
        duplicate.set(31, duplicate.get(0));
        assertThrows(IllegalArgumentException.class,
                () -> new RuntimeMetadataProjection(projection().manifestSha256(), "r9-v4", duplicate));
    }

    @Test
    void sourceIsAnExplicitReleaseInput() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeMetadataProjectionSource source = () -> {
            calls.incrementAndGet();
            return projection();
        };

        assertEquals("r9-v4", source.projection().edition());
        assertEquals(1, calls.get());
    }

    private static RuntimeMetadataProjection projection() {
        List<RuntimeMetadataProjection.Entry> rows = new ArrayList<>();
        for (int gameId = 1; gameId <= 32; gameId++) {
            String hash = gameId == 1 ? "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    : String.format("%064x", gameId);
            rows.add(new RuntimeMetadataProjection.Row(gameId, "game" + gameId, "family" + gameId,
                    "play-v" + gameId, "region-" + gameId, "snapshot-v1", "card-v3",
                    "events-v4", "protocol-v2", "common-room-v1", hash,
                    1000L + gameId, 2000L + gameId));
        }
        return new RuntimeMetadataProjection(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "r9-v4", rows);
    }
}
