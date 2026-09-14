package business.global.room;

import business.global.room.base.AbsBaseRoom;
import com.aoo.bcg.gamespi.BridgedGameRoom;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.ddm.server.common.CommLogD;
import java.util.Map;
import java.util.ServiceLoader;

/** Binds a real legacy room to one unified authority; never creates a shadow room. */
final class LegacyUnifiedRoomBinding {
    private static final GameRegistry PROVIDERS = loadProviders();

    private LegacyUnifiedRoomBinding() { }

    static void bind(AbsBaseRoom room, Object baseRoomConfigure, int seatLimit) {
        int gameId = room.getGameRoomBO().getGameType();
        GameProvider provider = PROVIDERS.require(gameId);
        RoomCreationContext context = new RoomCreationContext(room.getRoomID(), room.getOwnerID(), Map.of(
                "roomKey", room.getRoomKey(), "seatLimit", seatLimit));
        var authority = provider.createAuthoritativeSession(context).orElseThrow(() ->
                new IllegalStateException("provider has no legacy authority factory: " + provider.descriptor().code()));
        RuntimeGameRoomRegistry.global().bind(new GameRoomHandle(room.getRoomID(), gameId,
                provider.descriptor().version(), new BridgedGameRoom(room, authority)));
        CommLogD.info("Unified authority bound roomId:{}, gameId:{}, provider:{}",
                room.getRoomID(), gameId, provider.descriptor().code());
    }

    static void remove(long roomId) { RuntimeGameRoomRegistry.global().remove(roomId); }

    private static GameRegistry loadProviders() {
        GameRegistry registry = new GameRegistry();
        ServiceLoader.load(GameProvider.class).forEach(registry::register);
        return registry;
    }
}
