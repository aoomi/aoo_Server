package business.global.room;

import business.global.room.base.AbsBaseRoom;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.BridgedGameRoom;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.ddm.server.common.CommLogD;
import java.util.Map;

/** Binds a real legacy room to one unified authority; never creates a shadow room. */
final class LegacyUnifiedRoomBinding {
    private static final GameRegistry PROVIDERS = loadProviders();

    private LegacyUnifiedRoomBinding() { }

    static void bind(AbsBaseRoom room, Object baseRoomConfigure, int seatLimit,
                     RoomCreationContext.CompatibilityOrigin origin) {
        int roomGameId = room.getGameRoomBO().getGameType();
        String roomGameCode = room.getBaseRoomConfigure() == null || room.getBaseRoomConfigure().getGameType() == null
                ? "unknown" : room.getBaseRoomConfigure().getGameType().getName();
        if (origin == null) {
            throw admissionRejected("COMPATIBILITY_ORIGIN_REQUIRED", room.getRoomID(), roomGameId,
                    roomGameCode, "missing", "missing");
        }
        if (roomGameId != origin.staticIdentity().gameId()) {
            throw admissionRejected("COMPATIBILITY_ORIGIN_ROOM_IDENTITY_MISMATCH", room.getRoomID(), roomGameId,
                    origin.staticIdentity().code(), origin.staticIdentity().version(), origin.source());
        }
        GameProvider provider = requireProvider(PROVIDERS, room.getRoomID(), origin);
        try {
            origin.requireMatches(provider.descriptor(), provider.getClass().getName());
        } catch (RuntimeException failure) {
            throw admissionRejected("COMPATIBILITY_ORIGIN_PROVIDER_MISMATCH", room.getRoomID(), roomGameId,
                    origin.staticIdentity().code(), origin.staticIdentity().version(), origin.source(), failure);
        }
        RoomCreationContext context = new RoomCreationContext(room.getRoomID(), room.getOwnerID(), Map.of(
                "baseRoomConfigure", baseRoomConfigure,
                "roomKey", room.getRoomKey(), "seatLimit", seatLimit), origin);
        var authority = requireAuthority(provider, context);
        RuntimeGameRoomRegistry.global().bind(new GameRoomHandle(room.getRoomID(), roomGameId,
                provider.descriptor().version(), new BridgedGameRoom(room, authority, origin), origin));
        CommLogD.info("Unified authority bound roomId:{}, gameId:{}, provider:{}",
                room.getRoomID(), roomGameId, provider.descriptor().code());
    }

    static void remove(long roomId) { RuntimeGameRoomRegistry.global().remove(roomId); }

    /** Resolve a compatibility provider without leaking a non-machine registry error. */
    static GameProvider requireProvider(GameRegistry providers, long roomId,
                                        RoomCreationContext.CompatibilityOrigin origin) {
        try {
            return providers.require(origin.staticIdentity().gameId(), origin.staticIdentity().version());
        } catch (RuntimeException failure) {
            throw admissionRejected("COMPATIBILITY_PROVIDER_UNRESOLVED", roomId,
                    origin.staticIdentity().gameId(), origin.staticIdentity().code(),
                    origin.staticIdentity().version(), origin.source(), failure);
        }
    }

    /** Resolve the legacy authority factory while preserving the admission root cause. */
    static AuthoritativeGameSession requireAuthority(GameProvider provider, RoomCreationContext context) {
        RoomCreationContext.CompatibilityOrigin origin = context.requireCompatibilityOrigin();
        try {
            return provider.createAuthoritativeSession(context).orElseThrow(() -> admissionRejected(
                    "COMPATIBILITY_AUTHORITY_FACTORY_MISSING", context.roomId(),
                    origin.staticIdentity().gameId(), origin.staticIdentity().code(),
                    origin.staticIdentity().version(), origin.source()));
        } catch (RuntimeException failure) {
            if (failure.getMessage() != null && failure.getMessage().startsWith("ROOM_ADMISSION_REJECTED|")) {
                throw failure;
            }
            throw admissionRejected("COMPATIBILITY_AUTHORITY_CREATE_FAILED", context.roomId(),
                    origin.staticIdentity().gameId(), origin.staticIdentity().code(),
                    origin.staticIdentity().version(), origin.source(), failure);
        }
    }

    private static IllegalStateException admissionRejected(String reason, long roomId, int gameId,
                                                           String gameCode, String playVersion, String source) {
        return admissionRejected(reason, roomId, gameId, gameCode, playVersion, source, null);
    }

    private static IllegalStateException admissionRejected(String reason, long roomId, int gameId,
                                                           String gameCode, String playVersion, String source,
                                                           Throwable cause) {
        String message = "ROOM_ADMISSION_REJECTED|reason=" + token(reason)
                + "|roomId=" + roomId + "|gameId=" + gameId
                + "|gameCode=" + token(gameCode) + "|playVersion=" + token(playVersion)
                + "|source=" + token(source);
        return cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
    }

    private static String token(String value) {
        if (value == null || value.isBlank()) return "missing";
        return value.replace("|", "%7C");
    }

    private static GameRegistry loadProviders() {
        try {
            return GameRegistryFactory.compatibilityFromServiceLoader();
        } catch (RuntimeException unavailable) {
            // Keep the binding class loadable so the room-scoped admission wrapper can
            // report the missing provider with identity fields; an empty registry never
            // admits a room and therefore cannot manufacture a fallback provider.
            return GameRegistryFactory.compatibility();
        }
    }
}
