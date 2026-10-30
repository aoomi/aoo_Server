package com.aoo.bcg.hall.http;

import com.aoo.bcg.hall.room.RoomAuthorityRejected;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Hall must preserve deterministic authority status/code instead of hiding it as HALL_INTERNAL_ERROR. */
final class HallAuthorityErrorPropagationTest {
    @Test
    void preservesCreateAuthorityStatusAndCode() {
        RoomAuthorityRejected failure = new RoomAuthorityRejected(
                409, "ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED", "snapshot metadata source required");
        Map<String, Object> response = HallHttpRoutes.authorityFailure(failure,
                "trace-create");

        assertEquals(409, failure.status());
        assertEquals("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED", response.get("code"));
        assertEquals("snapshot metadata source required", response.get("message"));
        assertEquals("trace-create", response.get("traceId"));
    }

    @Test
    void preservesLeaveAuthorityStatusAndCode() {
        Map<String, Object> response = HallHttpRoutes.authorityFailure(
                new RoomAuthorityRejected(409, "ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT", "leave state conflict"),
                "trace-leave");

        assertEquals("ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT", response.get("code"));
        assertEquals("leave state conflict", response.get("message"));
        assertEquals("trace-leave", response.get("traceId"));
    }

    @Test
    void canonicalAndCompatibilityBoundariesKeepAuthorityErrorsDistinctFromGenericFailures() {
        RoomAuthorityRejected create = new RoomAuthorityRejected(
                409, "ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED", "snapshot metadata source required");
        RoomAuthorityRejected leave = new RoomAuthorityRejected(
                409, "ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT", "leave state conflict");

        assertEquals("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED",
                com.aoo.bcg.lobby.LobbyHttpRoutes.authorityFailure(
                        new com.aoo.bcg.lobby.LobbyRoomAuthorityRejected(create.status(), create.code(), create.getMessage()),
                        "trace-create").get("code"));
        assertEquals("ROOM_ORCHESTRATOR_LEAVE_STATE_VERSION_CONFLICT",
                HallHttpRoutes.authorityFailure(leave, "trace-leave").get("code"));
        assertNotEquals("HALL_INTERNAL_ERROR", HallHttpRoutes.authorityFailure(leave, "trace-leave").get("code"));
    }
}
