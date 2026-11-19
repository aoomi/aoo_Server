package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class WaitingRoomExpirationErrorMappingTest {
    @Test
    void endedRoomIsAConflictButUnknownPermissionFailuresRemainUnchanged() {
        RuntimeException ended = DeterministicAuthorityErrorBoundary
                .deterministicOrOriginal(new IllegalStateException("ROOM_ENDED"));
        RoomAuthorityBusinessError conflict = assertInstanceOf(RoomAuthorityBusinessError.class, ended);
        assertEquals(409, conflict.status());
        assertEquals("ROOM_ENDED", conflict.code());

        SecurityException forbidden = new SecurityException("waiting room activity is not owned by this authority");
        assertEquals(forbidden, DeterministicAuthorityErrorBoundary.deterministicOrOriginal(forbidden));
    }
}
