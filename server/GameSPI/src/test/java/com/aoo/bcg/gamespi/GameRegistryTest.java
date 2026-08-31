package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GameRegistryTest {
    @Test void rejectsDuplicateIdAndCode() {
        GameRegistry registry = new GameRegistry();
        registry.register(provider(1, "one"));
        assertThrows(IllegalStateException.class, () -> registry.register(provider(1, "two")));
        assertThrows(IllegalStateException.class, () -> registry.register(provider(2, "one")));
    }
    @Test void rejectsUnknownGame() { assertThrows(IllegalArgumentException.class, () -> new GameRegistry().require(404)); }
    @Test void pinsRoomsToCoexistingProviderVersions(){
        GameRegistry registry=new GameRegistry();registry.register(provider(1,"one","v1"));registry.register(provider(1,"one","v2"));
        assertEquals("v1",registry.require(1,"v1").descriptor().version());
        assertEquals("v2",registry.require(1,"v2").descriptor().version());
        assertThrows(IllegalStateException.class,()->registry.require(1));
    }
    private static GameProvider provider(int id, String code) {
        return provider(id,code,"1");
    }
    private static GameProvider provider(int id,String code,String version){
        GameDescriptor descriptor = new GameDescriptor(id, code, code, GameCategory.POKER, "test", RegionScope.NATIONAL, "", "", version);
        return new GameProvider() {
            public GameDescriptor descriptor() { return descriptor; }
            public GameRoomFactory roomFactory() { return context -> new GameRoomHandle(context.roomId(), id, version, Map.of()); }
        };
    }
}
