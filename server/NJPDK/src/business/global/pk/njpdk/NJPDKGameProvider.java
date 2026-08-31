package business.global.pk.njpdk;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.poker.PokerGameProvider;
import com.aoo.bcg.poker.PokerRuleFamily;
import com.aoo.bcg.poker.PaoDeKuaiConfig;
import com.aoo.bcg.poker.PaoDeKuaiFamily;
import com.aoo.bcg.poker.PdkRuleProfiles;
import com.aoo.bcg.poker.PdkPublishedRuleOptions;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.GameServiceLauncher;
import core.server.njpdk.NJPDKAPP;
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
import java.util.Set;

public final class NJPDKGameProvider implements PokerGameProvider {
    private static final Set<String> LEGACY_WAITING_KEYS = Set.of(
            "c86a274df7a866391408989c4a2d4824863951c784e04fd2c9eb0d43b0e9bb52");
    private static final PaoDeKuaiConfig BASE_CONFIG = new PaoDeKuaiConfig(5, true, true,
            false, null, true, 14, true, true, 1, 3, 2, true);
    private static final PaoDeKuaiFamily FAMILY = family(BASE_CONFIG);
    private static final SecureRandom SEEDS = new SecureRandom();
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            629, "njpdk", "内江跑得快", GameCategory.POKER,
            PaoDeKuaiFamily.CODE, RegionScope.CITY, "SC", "NJ", "legacy-equivalent-1");

    @Override
    public GameDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public GameRoomFactory roomFactory() {
        return NJPDKGameProvider::createLegacyRoom;
    }
    @Override public PokerRuleFamily pokerFamily() { return FAMILY; }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->room.requireAuthoritativeSession().viewFor(viewer));}
    @Override public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->room.requireAuthoritativeSession().settlement(round,room.playVersion()));}
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return Optional.of(createAuthority(context));
    }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String,Object> state) {
        Map<String,Object> options = ruleOptions(state);
        PaoDeKuaiFamily family = family(PdkPublishedRuleOptions.apply(options, BASE_CONFIG), options);
        return Optional.of(PokerAuthoritativeSession.restore(
                PdkPublishedRuleOptions.migrateWaitingSnapshotIdentity(
                        state, family, LEGACY_WAITING_KEYS), family));
    }

    @Override public Optional<GameServiceLauncher> serviceLauncher() { return Optional.of(NJPDKAPP::launch); }

    @SuppressWarnings("unchecked")
    private static GameRoomHandle createLegacyRoom(RoomCreationContext context) {
        Object value = context.immutableRules().get("baseRoomConfigure");
        if (value == null) return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(), createAuthority(context));
        if (!(value instanceof BaseRoomConfigure<?> configuration)) {
            throw new IllegalArgumentException("baseRoomConfigure is required");
        }
        if (configuration.getGameType() == null || configuration.getGameType().getId() != DESCRIPTOR.gameId()) {
            throw new IllegalArgumentException("baseRoomConfigure gameId must be " + DESCRIPTOR.gameId());
        }
        String roomKey = String.valueOf(context.immutableRules().getOrDefault("roomKey", context.roomId()));
        NJPDKRoom room = new NJPDKRoom((BaseRoomConfigure) configuration, roomKey, context.ownerId());
        var authority=createAuthority(context);
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(), new BridgedGameRoom(room,authority));
    }
    private static AuthoritativeGameSession createAuthority(RoomCreationContext context) {
        int playerCount = supportedPlayerCount(intRule(context, "playerCount", 4));
        int roundCount = intRule(context, "roundCount", 8);
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(context.immutableRules(), BASE_CONFIG);
        return new PokerAuthoritativeSession(context.roomId(), context.ownerId(),
                playerCount, SEEDS.nextLong(), family(config, context.immutableRules()), roundCount);
    }
    private static PaoDeKuaiFamily family(PaoDeKuaiConfig config){return family(config,Map.of());}
    private static PaoDeKuaiFamily family(PaoDeKuaiConfig config,Map<String,Object> rules){var base=PdkRuleProfiles.flexibleTwoToFourPlayers("legacy-equivalent-1",config.requiredFirstCard());return new PaoDeKuaiFamily(config,PdkPublishedRuleOptions.profile("legacy-equivalent-1",rules,config,base));}
    @SuppressWarnings("unchecked") private static Map<String,Object> ruleOptions(Map<String,Object> state){Object value=state.get("pdkRuleOptions");if(!(value instanceof Map<?,?> raw))throw new IllegalStateException("missing immutable PDK rule options");return (Map<String,Object>)raw;}
    private static int supportedPlayerCount(int value){if(value<2||value>4)throw new IllegalArgumentException("NJPDK playerCount must be 2, 3 or 4");return value;}
    private static int intRule(RoomCreationContext c,String key,int fallback){Object v=c.immutableRules().get(key);return v instanceof Number n?n.intValue():fallback;}
    private static long longRule(RoomCreationContext c,String key,long fallback){Object v=c.immutableRules().get(key);return v instanceof Number n?n.longValue():fallback;}
}
