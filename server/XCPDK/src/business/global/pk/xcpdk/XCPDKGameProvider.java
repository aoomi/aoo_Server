package business.global.pk.xcpdk;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.poker.PokerGameProvider;
import com.aoo.bcg.poker.PokerRuleFamily;
import com.aoo.bcg.poker.PaoDeKuaiConfig;
import com.aoo.bcg.poker.PaoDeKuaiFamily;
import com.aoo.bcg.poker.PdkRuleProfiles;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.GameServiceLauncher;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import com.aoo.bcg.gamespi.AuthoritativeSessionCommandHandler;
import com.aoo.bcg.gamespi.BridgedGameRoom;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.poker.PokerAuthoritativeSession;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Map;

public final class XCPDKGameProvider implements PokerGameProvider {
    private static final PaoDeKuaiFamily FAMILY = new PaoDeKuaiFamily(new PaoDeKuaiConfig(5, true, true, true, null, true), PdkRuleProfiles.flexibleTwoToFourPlayers("legacy-equivalent-1", null));
    private static final SecureRandom SEEDS = new SecureRandom();
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            618, "xcpdk", "宣城跑得快", GameCategory.POKER,
            PaoDeKuaiFamily.CODE, RegionScope.CITY, "AH", "XC", "legacy-equivalent-1");

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public GameRoomFactory roomFactory() { return XCPDKGameProvider::createLegacyRoom; }
    @Override public PokerRuleFamily pokerFamily() { return FAMILY; }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->room.requireAuthoritativeSession().viewFor(viewer));}
    @Override public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->room.requireAuthoritativeSession().settlement(round,room.playVersion()));}
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return Optional.of(createAuthority(context));
    }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String,Object> state) { return Optional.of(PokerAuthoritativeSession.restore(state,FAMILY)); }
    @Override public Optional<GameServiceLauncher> serviceLauncher() { return Optional.of(XCPDKAPP::launch); }

    @SuppressWarnings("unchecked")
    private static GameRoomHandle createLegacyRoom(RoomCreationContext context) {
        Object value = context.immutableRules().get("baseRoomConfigure");
        if (!(value instanceof BaseRoomConfigure<?> configuration)) {
            throw new IllegalArgumentException("baseRoomConfigure is required");
        }
        if (configuration.getGameType() == null || configuration.getGameType().getId() != DESCRIPTOR.gameId()) {
            throw new IllegalArgumentException("baseRoomConfigure gameId must be " + DESCRIPTOR.gameId());
        }
        String roomKey = String.valueOf(context.immutableRules().getOrDefault("roomKey", context.roomId()));
        XCPDKRoom room = new XCPDKRoom((BaseRoomConfigure) configuration, roomKey, context.ownerId());
        var authority=createAuthority(context);
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(), new BridgedGameRoom(room,authority));
    }
    private static AuthoritativeGameSession createAuthority(RoomCreationContext context) {
        int playerCount = supportedPlayerCount(intRule(context, "playerCount", intRule(context, "seatLimit", 4)));
        int roundCount = intRule(context, "roundCount", 8);
        return new PokerAuthoritativeSession(context.roomId(), context.ownerId(),
                playerCount, SEEDS.nextLong(), FAMILY, roundCount);
    }
    private static int supportedPlayerCount(int value){if(value<2||value>4)throw new IllegalArgumentException("XCPDK playerCount must be 2, 3 or 4");return value;}
    private static int intRule(RoomCreationContext c,String key,int fallback){Object v=c.immutableRules().get(key);return v instanceof Number n?n.intValue():fallback;}
    private static long longRule(RoomCreationContext c,String key,long fallback){Object v=c.immutableRules().get(key);return v instanceof Number n?n.longValue():fallback;}
}
