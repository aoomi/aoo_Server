package business.global.mj.cdxzmj;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.AuthoritativeSessionCommandHandler;
import com.aoo.bcg.gamespi.BridgedGameRoom;
import com.aoo.bcg.gamespi.GameCapability;
import com.aoo.bcg.gamespi.GameCapabilityManifest;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.GameServiceLauncher;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.mahjong.MahjongAuthoritativeSession;
import com.aoo.bcg.mahjong.MahjongGameProvider;
import com.aoo.bcg.mahjong.MahjongRuleFamily;
import com.aoo.bcg.mahjong.XueZhanMahjongFamily;
import core.server.cdxzmj.CDXZMJAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

/** Explicit historical 516 CDXZMJ compatibility provider. */
public final class CDXZMJCompatibilityProvider implements MahjongGameProvider {
    private static final SecureRandom SEEDS = new SecureRandom();
    private static final MahjongRuleFamily FAMILY = new XueZhanMahjongFamily();
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            516, "chengdu1001", "成都血战麻将", GameCategory.MAHJONG,
            "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD", "legacy-equivalent-1");

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public GameRoomFactory roomFactory() { return CDXZMJCompatibilityProvider::createLegacyRoom; }
    @Override public MahjongRuleFamily mahjongFamily() { return FAMILY; }
    @Override public Optional<GameCommandHandler> commandHandler() { return Optional.of(new AuthoritativeSessionCommandHandler()); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider() {
        return Optional.of((viewer, room) -> room.requireAuthoritativeSession().viewFor(viewer));
    }
    @Override public Optional<SettlementProvider> settlementProvider() {
        return Optional.of((room, round) -> room.requireAuthoritativeSession().settlement(round, room.playVersion()));
    }
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return Optional.of(new MahjongAuthoritativeSession(context.roomId(), context.ownerId(),
                intRule(context, "seatLimit", 4), SEEDS.nextLong(), FAMILY, DESCRIPTOR.version()));
    }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
        return Optional.of(MahjongAuthoritativeSession.restore(state, FAMILY));
    }
    @Override public Optional<GameServiceLauncher> serviceLauncher() { return Optional.of(CDXZMJAPP::launch); }
    @Override public GameCapabilityManifest capabilityManifest() {
        return new GameCapabilityManifest(Map.of(
                GameCapability.ROOM_SHUFFLE, "legacy room opXiPai; only while RoomState.Init"));
    }

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
        CDXZMJRoom room = new CDXZMJRoom((BaseRoomConfigure) configuration, roomKey, context.ownerId());
        AuthoritativeGameSession authority = new MahjongAuthoritativeSession(context.roomId(), context.ownerId(),
                intRule(context, "seatLimit", 4), SEEDS.nextLong(), FAMILY, DESCRIPTOR.version());
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(),
                new BridgedGameRoom(room, authority, null, context.snapshotMetadataSource()),
                null, context.snapshotMetadataSource());
    }

    private static int intRule(RoomCreationContext context, String key, int fallback) {
        Object value = context.immutableRules().get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
