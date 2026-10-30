package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.poker.PokerGameProvider;
import com.aoo.bcg.poker.ComparePokerFamily;
import com.aoo.bcg.poker.PokerRuleFamily;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Map;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;

public final class ZjhGameProvider implements PokerGameProvider {
    public static final String GAME_CODE = "quanguo2997";
    public static final String PLAY_VERSION = "zjh-v1.0.0";
    /** Explicit current-release provider identity, issued from this production provider. */
    public static final String PROVIDER_KEY = "zjh-native";
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            9, GAME_CODE, "金花", GameCategory.POKER,
            ComparePokerFamily.CODE, RegionScope.NATIONAL, "", "", PLAY_VERSION);
    private static final SecureRandom SEEDS = new SecureRandom();

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public Map<String, Object> defaultConfiguration() {
        return Map.of(
                "enabled", true,
                "provider", PROVIDER_KEY,
                "gameCode", GAME_CODE,
                "family", ComparePokerFamily.CODE,
                "playVersion", PLAY_VERSION);
    }
    @Override public GameRoomFactory roomFactory() { return ZjhGameProvider::createRoom; }
    @Override public PokerRuleFamily pokerFamily() { return new ComparePokerFamily(); }
    @Override public Optional<GameCommandHandler> commandHandler() { return Optional.of(new ZjhCommandHandler()); }
    // Production Gateway persists every authoritative command atomically through
    // JdbcGatewayGameCommandCommitter. Registering the legacy clark_game committer
    // here would perform a second write and requires an unavailable legacy DB runtime.
    @Override public GameCommandCommitter commandCommitter() { return GameCommandCommitter.noOp(); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->new ZjhReconnectViewService().build(room.requireLegacyRoom(ZjhTable.class),viewer));}
    @Override public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->new ZjhSettlementService().payload(room.requireLegacyRoom(ZjhTable.class),round,room.playVersion()));}
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return Optional.of(authority(context));
    }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
        return Optional.of(ZjhAuthoritativeSession.restore(state));
    }

    private static GameRoomHandle createRoom(RoomCreationContext context) {
        ZjhAuthoritativeSession authority = authority(context);
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(),
                authority, null, context.snapshotMetadataSource());
    }

    private static ZjhAuthoritativeSession authority(RoomCreationContext context) {
        ZjhRules rules = ZjhRules.from(context.immutableRules());
        long seed = SEEDS.nextLong();
        long seatSeed = SEEDS.nextLong();
        ZjhTable table = new ZjhTable(context.roomId(), context.ownerId(), rules, seed, seatSeed);
        return new ZjhAuthoritativeSession(table);
    }
}
