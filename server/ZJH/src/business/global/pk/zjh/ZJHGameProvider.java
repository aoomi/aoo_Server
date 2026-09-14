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

public final class ZJHGameProvider implements PokerGameProvider {
    public static final String PLAY_VERSION = "zjh-v1.0.0";
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            9, "zjh", "欢乐比牌", GameCategory.POKER,
            ComparePokerFamily.CODE, RegionScope.NATIONAL, "", "", PLAY_VERSION);
    private static final SecureRandom SEEDS = new SecureRandom();

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public GameRoomFactory roomFactory() { return ZJHGameProvider::createRoom; }
    @Override public PokerRuleFamily pokerFamily() { return new ComparePokerFamily(); }
    @Override public Optional<GameCommandHandler> commandHandler() { return Optional.of(new ZJHCommandHandler()); }
    @Override public GameCommandCommitter commandCommitter() { return new ZJHProductionCommitter(); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->new ZJHReconnectViewService().build(room.requireLegacyRoom(ZJHTable.class),viewer));}
    @Override public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->new ZJHSettlementService().payload(room.requireLegacyRoom(ZJHTable.class),round,room.playVersion()));}

    private static GameRoomHandle createRoom(RoomCreationContext context) {
        int seatLimit = intRule(context, "seatLimit", 5);
        long seed = SEEDS.nextLong();
        ZJHTable table = new ZJHTable(context.roomId(), context.ownerId(), seatLimit, seed);
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(), table);
    }

    private static int intRule(RoomCreationContext context, String key, int fallback) {
        Object value = context.immutableRules().get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static long longRule(RoomCreationContext context, String key, long fallback) {
        Object value = context.immutableRules().get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
