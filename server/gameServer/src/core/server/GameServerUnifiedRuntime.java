package core.server;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import business.player.PlayerMgr;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
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
import java.util.ServiceLoader;
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
        GameRegistry games = new GameRegistry();
        ServiceLoader.load(GameProvider.class).forEach(games::register);
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
        ProtocolV2AuthorityRuntime.install(command -> dispatch(runtime, roomRealtime, command));
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
                                   ProtocolV2AuthorityRuntime.Command command) {
        Player player = PlayerMgr.getInstance().getPlayerByAccountID(command.accountId());
        if (player == null) throw new SecurityException("authenticated account has no player");
        if (roomRealtime.supports(command.msgId())) return roomRealtime.dispatch(player, command).body().asMap();
        long roomId = Long.parseLong(command.roomId());
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomId);
        int seatId;
        if (room != null) {
            AbsRoomPos position = room.getRoomPosMgr().getPosByPid(player.getPid());
            if (position == null) throw new SecurityException("player is not seated in room");
            seatId = position.getPosID();
        } else {
            Object rawPlayers = RuntimeGameRoomRegistry.global().require(roomId)
                    .requireAuthoritativeSession().authoritativeState().get("players");
            if (!(rawPlayers instanceof java.util.Map<?,?> players)) throw new SecurityException("room has no seats");
            seatId = players.entrySet().stream().filter(entry -> Long.parseLong(String.valueOf(entry.getValue())) == player.getPid())
                    .map(entry -> Integer.parseInt(String.valueOf(entry.getKey()))).findFirst()
                    .orElseThrow(() -> new SecurityException("player is not seated in authority"));
        }
        var session = new ConnectionSession(Long.toString(player.getPid()), command.roomId(),
                seatId, command.playVersion(), command.sequence() - 1);
        var frame = new WebSocketFrame(command.msgId(), command.requestId(), command.sequence(),
                command.roomId(), command.roundNo(), command.playVersion(), command.timestamp(), command.body());
        return runtime.route(session, frame).result();
    }
}
