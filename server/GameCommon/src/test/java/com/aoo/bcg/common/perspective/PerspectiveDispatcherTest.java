package com.aoo.bcg.common.perspective;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.aoo.bcg.common.room.AuthoritativeRoom;

class PerspectiveDispatcherTest {
    @Test void privatePayloadOnlyReachesOwningPlayer() {
        var owner = ViewerContext.player(10, 1);
        var other = ViewerContext.player(10, 2);
        var spectator = new ViewerContext(10, 3, ViewerRole.SPECTATOR);
        var delivered = new ArrayList<Long>();
        var privateMessage = new PerspectiveMessage<>(10, 1, "hand", PerspectiveVisibility.PLAYER_PRIVATE, 1, "secret");
        PerspectiveDispatcher.dispatch(privateMessage, List.of(owner, other, spectator), (viewer, payload) -> delivered.add(viewer.authenticatedViewerId()));
        assertEquals(List.of(1L), delivered);
        delivered.clear();
        var publicMessage = new PerspectiveMessage<>(10, 2, "discard", PerspectiveVisibility.PUBLIC, 0, "public");
        PerspectiveDispatcher.dispatch(publicMessage, List.of(owner, other, spectator), (viewer, payload) -> delivered.add(viewer.authenticatedViewerId()));
        assertEquals(List.of(1L, 2L, 3L), delivered);
    }

    @Test void promotesSpectatorOnlyAtReservedRoundBoundary() {
        SpectatorAdmissionCoordinator admissions = new SpectatorAdmissionCoordinator();
        TestRoom room = new TestRoom();
        admissions.watch(10, 3, 4);
        admissions.reserveForNextRound(10, 3, 1, 4);
        PerspectiveMessage<String> privateMessage = new PerspectiveMessage<>(10, 3, "hand",
                PerspectiveVisibility.PLAYER_PRIVATE, 3, "secret");
        assertFalse(privateMessage.visibleTo(admissions.viewer(10, 3, 4)));
        assertThrows(IllegalStateException.class, () -> admissions.activate(room, 3, 4));
        admissions.activate(room, 3, 5);
        assertTrue(privateMessage.visibleTo(admissions.viewer(10, 3, 5)));
        assertEquals(3, room.seats().get(1).playerId());
    }
    private static final class TestRoom extends AuthoritativeRoom { private TestRoom() { super(10, 1, "v1", 3); } }
}
