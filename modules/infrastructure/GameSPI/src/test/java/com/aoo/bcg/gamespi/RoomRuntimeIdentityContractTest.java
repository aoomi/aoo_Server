package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoomRuntimeIdentityContractTest {
    @Test
    void requiresExplicitNonFabricatedRuntimeFence() {
        var identity = new RoomRuntimeIdentity("instance-1", 4L, 9L, 12L);
        assertEquals("instance-1", identity.roomInstanceId());
        assertEquals(4L, identity.lifecycleEpoch());
        assertEquals(9L, identity.fencingToken());
        assertEquals(12L, identity.stateVersion());
        assertEquals(identity, new GameRoomHandle(7L, 62, "v1", new Object(), identity)
                .runtimeIdentity());
        assertNull(new GameRoomHandle(8L, 62, "v1", new Object()).runtimeIdentity(),
                "legacy constructor must not fabricate an identity");
    }

    @Test
    void rejectsMissingOrInvalidGenerationValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoomRuntimeIdentity("", 1L, 1L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new RoomRuntimeIdentity("instance", 0L, 1L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new RoomRuntimeIdentity("instance", 1L, 0L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new RoomRuntimeIdentity("instance", 1L, 1L, -1L));
    }

    @Test
    void generationComparisonDoesNotTreatAChangedInstanceAsTheSameGeneration() {
        var first = new RoomRuntimeIdentity("instance-1", 1L, 2L, 3L);
        var next = new RoomRuntimeIdentity("instance-2", 1L, 2L, 4L);
        assertEquals(false, first.sameGeneration(next));
        assertEquals(true, first.sameGeneration(
                new RoomRuntimeIdentity("instance-1", 1L, 2L, 4L)));
    }
}
