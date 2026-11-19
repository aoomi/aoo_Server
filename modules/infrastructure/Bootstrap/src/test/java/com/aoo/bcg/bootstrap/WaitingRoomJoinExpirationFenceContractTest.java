package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused contract for the join/automatic-expiration fence.  The runtime
 * integration matrix supplies the real JDBC race; these assertions prevent a
 * future refactor from moving membership mutation before the durable fence or
 * restoring the old generic-403 race mapping.
 */
final class WaitingRoomJoinExpirationFenceContractTest {
    private static final Path AUTHORITY = Path.of(
            "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java");

    @Test
    void joinFencesRouteBeforeAdmittingAndPersistingSnapshot() throws Exception {
        String source = Files.readString(AUTHORITY);
        int join = source.indexOf("public Map<String, Object> join(Map<String, Object> command)");
        int fence = source.indexOf("touchWaitingRoomActivity(roomId, route.fencingToken())", join);
        int admit = source.indexOf("admit(session,accountId,seatNo", join);
        int snapshot = source.indexOf("stateStore.saveSnapshot(committedRoom, route.fencingToken())", join);
        assertTrue(join >= 0 && fence > join, "join must acquire the durable route fence");
        assertTrue(admit > fence, "membership mutation must follow the route fence");
        assertTrue(snapshot > admit, "snapshot persistence must follow admission");
    }

    @Test
    void fenceUsesDatabaseMutexAndMapsOnlyEndedRoutesToRoomEnded() throws Exception {
        String source = Files.readString(AUTHORITY);
        int start = source.indexOf("private void touchWaitingRoomActivity");
        int end = source.indexOf("private boolean expireInactiveRoom", start);
        String fence = source.substring(start, end);
        assertTrue(fence.contains("connection.setAutoCommit(false)"));
        assertTrue(fence.contains("selectForUpdate(connection, roomId)"));
        assertTrue(fence.contains("\"REMOVING\".equals(current.state())"));
        assertTrue(fence.contains("\"REMOVED\".equals(current.state())"));
        assertTrue(fence.contains("new RoomAuthorityBusinessError(409, \"ROOM_ENDED\""));
        assertTrue(fence.contains("new SecurityException(\"waiting room activity is not owned by this authority\")"));
    }

    @Test
    void expirationLosesCleanlyWhenJoinRefreshesActivityFirst() throws Exception {
        String source = Files.readString(AUTHORITY);
        int start = source.indexOf("private RoomAuthorityBusinessError expireAndClose");
        int end = source.indexOf("private boolean beginAutomaticRemoval", start);
        String expiration = source.substring(start, end);
        assertTrue(expiration.contains("if (!beginAutomaticRemoval"));
        assertTrue(expiration.contains("waitingRoomEndedIfRemoving(latest)"));
        int classifier = source.indexOf("private static RoomAuthorityBusinessError waitingRoomEndedIfRemoving");
        int classifierEnd = source.indexOf("private boolean expireInactiveRoom", classifier);
        String classification = source.substring(classifier, classifierEnd);
        assertTrue(classification.contains(": null"),
                "a stale expiration attempt must not close a route refreshed by join");
    }

    @Test
    void noHalfCommitPathExistsBeforeRouteFence() throws Exception {
        String source = Files.readString(AUTHORITY);
        int join = source.indexOf("public Map<String, Object> join(Map<String, Object> command)");
        int end = source.indexOf("private RoomRuntimeIdentityStore.Entry requireJoinRuntimeIdentity", join);
        String body = source.substring(join, end);
        assertTrue(body.indexOf("touchWaitingRoomActivity(roomId, route.fencingToken())")
                        < body.indexOf("admit(session,accountId,seatNo"));
        assertTrue(body.indexOf("touchWaitingRoomActivity(roomId, route.fencingToken())")
                        < body.indexOf("common.room.join_req"));
    }
}
