package com.aoo.bcg.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeGameRoomIdentityContractTest {
    @Test
    void createAndRuntimeLookupRequireExplicitIdentity() {
        var rooms = new RuntimeGameRoomRegistry();
        var identity = new RoomRuntimeIdentity("instance-1", 2L, 7L, 0L);
        GameRoomHandle room = new GameRoomHandle(41L, 62, "v1", new Object(), identity);
        assertSame(room, rooms.bindRuntime(room));
        assertSame(room, rooms.requireRuntime(41L));
        assertEquals(identity, rooms.requireRuntime(41L).runtimeIdentity());
        assertThrows(IllegalStateException.class,
                () -> rooms.bindRuntime(new GameRoomHandle(42L, 62, "v1", new Object())));
        assertThrows(IllegalStateException.class,
                () -> rooms.bind(new GameRoomHandle(43L, 62, "v1", new Object())));
    }

    @Test
    void providerCannotCreateAnUntypedRuntimeHandle() {
        var rooms = new RuntimeGameRoomRegistry();
        GameProvider provider = new GameProvider() {
            private final GameDescriptor descriptor = new GameDescriptor(62, "test", "test",
                    GameCategory.POKER, "test", RegionScope.NATIONAL, "", "", "v1");

            @Override public GameDescriptor descriptor() { return descriptor; }

            @Override public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
                return context -> new GameRoomHandle(context.roomId(), 62, "v1", new Object());
            }
        };
        assertThrows(IllegalStateException.class,
                () -> rooms.create(provider, new RoomCreationContext(44L, 1L, Map.of())));
        assertEquals(0, rooms.size());
    }

    @Test
    void duplicateOrConflictingGenerationCannotReplaceTheRegisteredHandle() {
        var rooms = new RuntimeGameRoomRegistry();
        var current = new GameRoomHandle(42L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-1", 2L, 7L, 4L));
        rooms.bindRuntime(current);

        var sameIdentity = new GameRoomHandle(42L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-1", 2L, 7L, 4L));
        assertSame(current, rooms.bindRuntime(sameIdentity));

        var stale = new GameRoomHandle(42L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-1", 2L, 7L, 3L));
        assertThrows(IllegalStateException.class, () -> rooms.updateRuntime(stale));

        var differentGeneration = new GameRoomHandle(42L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-2", 3L, 8L, 0L));
        assertThrows(IllegalStateException.class, () -> rooms.updateRuntime(differentGeneration));
    }

    @Test
    void onlyStateVersionMayAdvanceWithinTheSameGeneration() {
        var rooms = new RuntimeGameRoomRegistry();
        var initial = new GameRoomHandle(43L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-1", 2L, 7L, 4L));
        rooms.bindRuntime(initial);
        var advanced = new GameRoomHandle(43L, 62, "v1", new Object(),
                new RoomRuntimeIdentity("instance-1", 2L, 7L, 5L));
        assertSame(advanced, rooms.updateRuntime(advanced));
        assertSame(advanced, rooms.requireRuntime(43L));
    }
}
