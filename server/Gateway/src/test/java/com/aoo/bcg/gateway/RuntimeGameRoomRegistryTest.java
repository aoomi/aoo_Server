package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeGameRoomRegistryTest {
    @Test void creationAndResolutionUseTheSameHandle() {
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        GameProvider provider = new GameProvider() {
            private final GameDescriptor descriptor = new GameDescriptor(7, "test", "test",
                    GameCategory.POKER, "test", RegionScope.NATIONAL, "", "", "v1");
            @Override public GameDescriptor descriptor() { return descriptor; }
            @Override public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
                return context -> new GameRoomHandle(context.roomId(), 7, "v1", new Object());
            }
        };
        RoomCreationContext context = new RoomCreationContext(99, 11, Map.of());
        GameRoomHandle created = rooms.create(provider, context);
        assertSame(created, rooms.require(99));
        assertThrows(IllegalStateException.class, () -> rooms.create(provider, context));
        rooms.remove(99);
        assertThrows(IllegalArgumentException.class, () -> rooms.require(99));
    }
}
