package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeterministicAuthorityErrorBoundaryTest {
    @Test
    void mapsOnlyDeterministicAuthorityCodesToNonRetryableBusinessErrors() {
        for (String code : new String[]{
                "ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED",
                "ROOM_RUNTIME_IDENTITY_DESTROYED",
                "ROOM_RUNTIME_IDENTITY_CONFLICT",
                "ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT",
                "ROOM_ORCHESTRATOR_LEAVE_CANONICAL_METADATA_CONFLICT"}) {
            RuntimeException mapped = DeterministicAuthorityErrorBoundary
                    .deterministicOrOriginal(new IllegalStateException(code));
            assertTrue(mapped instanceof RoomAuthorityBusinessError, code);
            RoomAuthorityBusinessError error = (RoomAuthorityBusinessError) mapped;
            assertEquals(409, error.status(), code);
            assertEquals(code, error.code(), code);
        }
    }

    @Test
    void preservesUnknownAndTemporaryFailuresForExistingRetryablePath() {
        RuntimeException original = new IllegalStateException("database temporarily unavailable");
        assertSame(original, DeterministicAuthorityErrorBoundary.deterministicOrOriginal(original));
        assertNull(DeterministicAuthorityErrorBoundary.deterministicCode(
                new IllegalStateException("database temporarily unavailable")));
    }
}
