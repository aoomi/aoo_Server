package business.global.mj.cdxzmj;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.EventReplayProvider;
import com.aoo.bcg.gamespi.GameCapabilityManifest;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameServiceLauncher;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry;
import com.aoo.bcg.mahjong.MahjongGameProvider;
import com.aoo.bcg.mahjong.MahjongRuleFamily;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical current Chengdu Xue-Zhan Mahjong provider.
 *
 * <p>The old native CDXZMJ room remains available only through the explicit
 * {@link CDXZMJCompatibilityProvider} and its compatibility registry. The
 * current provider delegates to the catalog-backed family runtime so its
 * descriptor and room implementation cannot drift from gameId 1001.</p>
 */
public final class CDXZMJGameProvider implements MahjongGameProvider {
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            1001, "chengdu1001", "成都麻将", GameCategory.MAHJONG,
            "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", "chengdu1001-v1.0.0");

    private static MahjongGameProvider delegate() {
        GameProvider provider = MahjongCatalogRuntimeRegistry.providerFor(DESCRIPTOR)
                .orElseThrow(() -> new IllegalStateException("CURRENT_CDXZMJ_PROVIDER_UNAVAILABLE"));
        if (!(provider instanceof MahjongGameProvider mahjong)) {
            throw new IllegalStateException("CURRENT_CDXZMJ_PROVIDER_INVALID");
        }
        return mahjong;
    }

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public GameRoomFactory roomFactory() { return delegate().roomFactory(); }
    @Override public MahjongRuleFamily mahjongFamily() { return delegate().mahjongFamily(); }
    @Override public void validateMahjongCategory() {
        if (descriptor().category() != GameCategory.MAHJONG
                || !descriptor().family().replace(':', '-').equals(mahjongFamily().familyCode())) {
            throw new IllegalStateException("mahjong descriptor and runtime family must match");
        }
    }
    @Override public Optional<GameCommandHandler> commandHandler() { return delegate().commandHandler(); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider() { return delegate().reconnectViewProvider(); }
    @Override public Optional<SettlementProvider> settlementProvider() { return delegate().settlementProvider(); }
    @Override public Optional<EventReplayProvider> eventReplayProvider() { return delegate().eventReplayProvider(); }
    @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
        return delegate().createAuthoritativeSession(context);
    }
    @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
        return delegate().restoreAuthoritativeSession(state);
    }
    @Override public List<RuleComponent<GameCommandRequest>> ruleComponents() { return delegate().ruleComponents(); }
    @Override public Map<String, Object> defaultConfiguration() { return delegate().defaultConfiguration(); }
    @Override public Optional<GameServiceLauncher> serviceLauncher() { return delegate().serviceLauncher(); }
    @Override public GameCapabilityManifest capabilityManifest() { return delegate().capabilityManifest(); }
}
