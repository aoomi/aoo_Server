package com.aoo.bcg.common.room;

import com.aoo.bcg.gamespi.RoomState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.common.turn.RoundSeatAuthority;
import java.util.List;

class AuthoritativeRoomTest {
    @Test void enforcesStateTransitionsAndSeatOwnership() {
        TestRoom room = new TestRoom();
        room.assign(0, 10);
        assertThrows(IllegalStateException.class, () -> room.assign(0, 11));
        room.transition(RoomState.CREATED, RoomState.WAITING);
        room.transition(RoomState.WAITING, RoomState.PLAYING);
        assertThrows(IllegalStateException.class, () -> room.transition(RoomState.PLAYING, RoomState.FINISHED));
    }
    @Test void preservesAndTransfersOwnerAcrossConnectivityAndRestore() {
        OwnedRoom room = new OwnedRoom();
        room.assign(0, 10); room.assign(1, 11); room.assign(2, 12);
        room.ownership().disconnected(10);
        assertEquals(10, room.ownership().ownerId());
        assertEquals(RoomOwnership.Status.OWNER_OFFLINE, room.ownership().status());
        RoomOwnership restored = RoomOwnership.restore(room.ownership().ownerId(), room.ownership().revision(), room.ownership().status());
        restored.reconnected(10);
        restored.transfer(10, 12, room.seats());
        assertEquals(12, restored.ownerId());
        assertThrows(SecurityException.class, () -> restored.dissolved(10));
        restored.dissolved(12);
        assertEquals(RoomOwnership.Status.DISSOLVED, restored.status());
    }
    @Test void appliesExplicitOwnerLeavePolicy() {
        OwnedRoom room = new OwnedRoom(); room.assign(0, 10); room.assign(2, 12); room.assign(1, 11);
        assertFalse(room.ownership().ownerLeaving(10, OwnerDeparturePolicy.TRANSFER_TO_LOWEST_OCCUPIED_SEAT, room.seats()));
        assertEquals(11, room.ownership().ownerId());
    }
    @Test void keepsDealerAndOperationSeatConsistentAcrossRoundLifecycle() {
        RoundSeatAuthority authority = new RoundSeatAuthority(List.of(0, 1, 2), 1);
        assertEquals(1, authority.beginDeal());
        assertEquals(2, authority.advanceFrom(1));
        authority.beginSettlement();
        authority.nextRound(0);
        RoundSeatAuthority restored = RoundSeatAuthority.restore(List.of(0, 1, 2), authority.dealerSeat(),
                authority.operationSeat(), authority.revision(), authority.phase());
        assertEquals(0, restored.dealerSeat());
        assertEquals(0, restored.beginDeal());
        assertThrows(IllegalStateException.class, () -> restored.advanceFrom(1));
    }
    @Test void clearsReadinessAtEveryLifecycleBoundaryWithoutReconnectDrift() {
        ReadyStateRegistry ready = new ReadyStateRegistry(List.of(0, 1, 2));
        ready.setReady(0, true); ready.setReady(1, true);
        ready.apply(ReadyLifecycleEvent.RECONNECTED, 0);
        assertTrue(ready.isReady(0));
        ready.apply(ReadyLifecycleEvent.LEFT, 0); assertFalse(ready.isReady(0));
        ready.setReady(0, true); ready.setReady(1, true); ready.apply(ReadyLifecycleEvent.ROUND_STARTED, null);
        assertTrue(ready.snapshot().values().stream().noneMatch(Boolean::booleanValue));
        ready.setReady(2, true); ready.apply(ReadyLifecycleEvent.ROUND_SETTLED, null); assertFalse(ready.isReady(2));
        ready.setReady(1, true); ready.apply(ReadyLifecycleEvent.TABLE_SWITCHED, 1); assertFalse(ready.isReady(1));
        ready.setReady(0, true); ready.apply(ReadyLifecycleEvent.ROOM_DISSOLVED, null); assertFalse(ready.isReady(0));
    }
    @Test void strongDefinitionRejectsInvalidLegacyRoomConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new RoomDefinition(1, 1, "v1", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new RoomDefinition(1, 1, "v1", 3, -1));
        RoomDefinition definition = new RoomDefinition(8, 62, "njpdk-v2", 3, 10);
        assertEquals(3, definition.seatCount());
    }
    private static final class TestRoom extends AuthoritativeRoom { private TestRoom() { super(1, 1, "v1", 3); } }
    private static final class OwnedRoom extends AuthoritativeRoom { private OwnedRoom() { super(2, 1, "v1", 3, 10); } }
}
