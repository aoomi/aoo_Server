package core.server;

import business.player.Player;
import business.player.PlayerMgr;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gateway.ConnectionSession;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.gateway.UnifiedGameRuntime;
import com.aoo.bcg.gateway.JdbcRiskAdmissionAuthority;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.ddm.server.common.Config;
import com.ddm.server.protocol.v2.ProtocolV2AuthorityRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import com.aoo.bcg.common.id.DistributedIdGenerator;
import com.aoo.bcg.common.event.OutboxRelay;
import com.aoo.bcg.common.event.RocketMqOutboxPublisher;
import com.aoo.bcg.common.event.ScheduledOutboxRelay;

/** Single production assembly for V2 commands, persistence, settlement and recovery dependencies. */
final class GameServerUnifiedRuntime {
    private static final Clock CLOCK = Clock.systemUTC();
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final DistributedIdGenerator IDS = new DistributedIdGenerator(Integer.toUnsignedLong(Config.ServerID())&0x3ffL,CLOCK);

    private GameServerUnifiedRuntime() { }

    static void install(DataSource dataSource) {
        GameRegistry games = GameRegistryFactory.requireCompletedProductionProjection();
        var persistence = ProductionPersistenceAssembly.create(dataSource, MAPPER, IDS, CLOCK);
        String nodeId = System.getenv().getOrDefault("AOO_NODE_ID", Config.ServerIDStr());
        var durableCommitter = new ProductionRoomCommandCommitter(persistence.roomEvents(),
                persistence.roomSnapshots(), persistence.roomLeases(), CLOCK, nodeId);
        UnifiedGameRuntime runtime = new UnifiedGameRuntime(games, RuntimeGameRoomRegistry.global(),
                persistence.idempotency(), persistence.settlements(), CLOCK,
                Duration.ofSeconds(30), Duration.ofHours(24), durableCommitter,
                new JdbcRiskAdmissionAuthority(dataSource,CLOCK));

        new ProductionRoomRecoveryCoordinator(games, RuntimeGameRoomRegistry.global(),
                persistence.roomSnapshots(), persistence.roomLeases(),persistence.roomEvents(), CLOCK, nodeId).recoverExpired(1000);

        startOutboxRelay(persistence,nodeId);

        var roomRealtime = new ProductionRoomRealtimeService(persistence.idempotency(), MAPPER, CLOCK, games, dataSource);
        ProtocolV2AuthorityRuntime.install(command -> dispatch(runtime, roomRealtime, games, command));
    }

    private static void startOutboxRelay(ProductionPersistenceAssembly persistence,String nodeId){
        String nameserver=System.getenv("AOO_MQ_NAMESRV_ADDR");if(nameserver==null||nameserver.isBlank())return;
        try{
            var publisher=new RocketMqOutboxPublisher(nameserver,"aoo-room-outbox-"+nodeId,
                    System.getenv().getOrDefault("AOO_MQ_ROOM_EVENT_TOPIC","AOO_ROOM_EVENTS"),MAPPER);
            var scheduled=new ScheduledOutboxRelay(new OutboxRelay(persistence.outbox(),publisher,CLOCK,Duration.ofSeconds(5)),
                    persistence.outbox(),CLOCK,Duration.ofSeconds(1),100,
                    health->{if(health.alertRequired())com.ddm.server.common.CommLogD.warn("Room outbox alert deadLetters:{}, failure:{}",health.deadLetters(),health.relayFailure());});
            scheduled.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{scheduled.close();publisher.close();},"aoo-room-outbox-shutdown"));
        }catch(Exception error){throw new IllegalStateException("cannot start room outbox relay",error);}
    }

    private static Object dispatch(UnifiedGameRuntime runtime, ProductionRoomRealtimeService roomRealtime,
                                   GameRegistry productionGames,
                                   ProtocolV2AuthorityRuntime.Command command) {
        Player player = PlayerMgr.getInstance().getPlayerByAccountID(command.accountId());
        if (player == null) throw new SecurityException("authenticated account has no player");
        long roomId = positiveRoomId(command.roomId());
        RoomAdmission admission = requireAdmission(RuntimeGameRoomRegistry.global(), productionGames, roomId,
                command.playVersion());
        if (admission.compatibilityOrigin() != null) {
            return roomRealtime.dispatch(player, command, admission.compatibilityOrigin()).body().asMap();
        }
        int seatId = typedSeat(admission, player);
        var session = new ConnectionSession(Long.toString(player.getPid()), command.roomId(),
                seatId, command.playVersion(), command.sequence() - 1);
        var frame = new WebSocketFrame(command.msgId(), command.requestId(), command.sequence(),
                command.roomId(), command.roundNo(), command.playVersion(), command.timestamp(), command.body());
        return runtime.route(session, frame).result();
    }

    /**
     * Admission must be resolved from the registry before any old RoomMgr lookup.  The
     * production caller supplies a current production registry when it has one; the
     * transport hook intentionally uses the handle's explicit runtime fence and fails
     * closed when the current descriptor cannot be resolved.
     */
    static RoomAdmission requireAdmission(RuntimeGameRoomRegistry rooms, GameRegistry productionGames,
                                          long roomId, String requestedPlayVersion) {
        GameRoomHandle room;
        try {
            room = rooms.require(roomId);
        } catch (RuntimeException failure) {
            throw admissionRejected("ROOM_IDENTITY_NOT_REGISTERED", roomId, 0, "unknown",
                    requestedPlayVersion, "missing", failure);
        }
        String handleVersion = room.playVersion();
        RoomCreationContext.CompatibilityOrigin origin = room.compatibilityOrigin();
        if (origin != null) {
            GameDescriptor identity = origin.staticIdentity();
            String source = origin.source();
            if (room.runtimeIdentity() != null) {
                throw admissionRejected("ROOM_IDENTITY_AMBIGUOUS", roomId, room.gameId(), identity.code(),
                        handleVersion, source);
            }
            if (identity.gameId() != room.gameId() || !identity.version().equals(handleVersion)
                    || !identity.version().equals(requestedPlayVersion)) {
                throw admissionRejected("COMPATIBILITY_IDENTITY_MISMATCH", roomId, room.gameId(), identity.code(),
                        handleVersion, source);
            }
            return new RoomAdmission(room, origin, identity.code());
        }
        if (room.runtimeIdentity() == null) {
            throw admissionRejected("ROOM_IDENTITY_REQUIRED", roomId, room.gameId(), "unknown",
                    handleVersion, "missing");
        }
        if (!handleVersion.equals(requestedPlayVersion)) {
            throw admissionRejected("ROOM_PLAY_VERSION_MISMATCH", roomId, room.gameId(), "unknown",
                    handleVersion, "current-production");
        }
        if (productionGames != null) {
            try {
                GameDescriptor descriptor = productionGames.require(room.gameId(), handleVersion).descriptor();
                if (!descriptor.version().equals(handleVersion)) {
                    throw admissionRejected("ROOM_CURRENT_IDENTITY_MISMATCH", roomId, room.gameId(),
                            descriptor.code(), handleVersion, "current-production");
                }
                return new RoomAdmission(room, null, descriptor.code());
            } catch (RuntimeException failure) {
                if (failure instanceof IllegalStateException state
                        && state.getMessage() != null
                        && state.getMessage().startsWith("ROOM_ADMISSION_REJECTED|")) throw state;
                throw admissionRejected("ROOM_CURRENT_IDENTITY_UNRESOLVED", roomId, room.gameId(), "unknown",
                        handleVersion, "current-production", failure);
            }
        }
        throw admissionRejected("ROOM_CURRENT_IDENTITY_UNRESOLVED", roomId, room.gameId(), "unknown",
                handleVersion, "current-production");
    }

    private static int typedSeat(RoomAdmission admission, Player player) {
        try {
            Object rawPlayers = admission.room().requireAuthoritativeSession().authoritativeState().get("players");
            if (!(rawPlayers instanceof Map<?, ?> players)) {
                throw admissionRejected("ROOM_TYPED_SEAT_INDEX_MISSING", admission.room().roomId(),
                        admission.room().gameId(), admission.gameCode(), admission.room().playVersion(),
                        "current-production");
            }
            for (Map.Entry<?, ?> entry : players.entrySet()) {
                if (Long.parseLong(String.valueOf(entry.getValue())) == player.getPid()) {
                    return Integer.parseInt(String.valueOf(entry.getKey()));
                }
            }
        } catch (IllegalStateException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw admissionRejected("ROOM_TYPED_SEAT_INDEX_INVALID", admission.room().roomId(),
                    admission.room().gameId(), admission.gameCode(), admission.room().playVersion(),
                    "current-production", failure);
        }
        throw admissionRejected("ROOM_PLAYER_NOT_SEATED", admission.room().roomId(), admission.room().gameId(),
                admission.gameCode(), admission.room().playVersion(), "current-production");
    }

    private static long positiveRoomId(String value) {
        try {
            long roomId = Long.parseLong(value);
            if (roomId <= 0) throw new NumberFormatException("non-positive");
            return roomId;
        } catch (RuntimeException failure) {
            throw admissionRejected("ROOM_ID_INVALID", 0, 0, "unknown", String.valueOf(value), "missing", failure);
        }
    }

    static IllegalStateException admissionRejected(String reason, long roomId, int gameId,
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

    record RoomAdmission(GameRoomHandle room,
                         RoomCreationContext.CompatibilityOrigin compatibilityOrigin,
                         String gameCode) { }
}
