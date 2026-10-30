package core.server;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.RegionScope;

import java.util.List;

/** Executable convergence checks; no database or production runtime is started. */
public final class ProviderProjectionConvergenceSelfTest {
    private ProviderProjectionConvergenceSelfTest() { }

    public static void main(String[] args) {
        gameServerRequiresCompletedProjectionInput();
        gameServerInstallFailsClosedBeforePersistenceWhenCompletionIsUnavailable();
        compatibilityProviderCannotEnterProductionProjection();
        incompleteProjectionFailsClosed();
        conflictingCurrentIdentityFailsClosed();
        System.out.println("ProviderProjectionConvergenceSelfTest: PASS");
    }

    private static void gameServerRequiresCompletedProjectionInput() {
        RuntimeException failure = expectFailure(
                GameRegistryFactory::requireCompletedProductionProjection,
                "gameServer must not consume raw ServiceLoader projection");
        check(failure.getMessage().contains("PRODUCTION_COMPLETION_REQUIRED"),
                "completion requirement must be machine-readable");
        check(failure.getMessage().contains("source=bootstrap-catalog"),
                "completion source must be machine-readable");
    }

    private static void gameServerInstallFailsClosedBeforePersistenceWhenCompletionIsUnavailable() {
        RuntimeException failure = expectFailure(
                () -> GameServerUnifiedRuntime.install(null),
                "gameServer install must reject before persistence without completed projection");
        check(failure.getMessage().contains("PRODUCTION_COMPLETION_REQUIRED"),
                "gameServer install rejection must be machine-readable");
    }

    private static void compatibilityProviderCannotEnterProductionProjection() {
        GameProvider legacy = provider(new GameDescriptor(516, "chengdu1001", "成都麻将",
                GameCategory.MAHJONG, "mahjong-xue-zhan", RegionScope.CITY,
                "SC", "CD", "legacy-equivalent-1"));
        GameProvider current = provider(new GameDescriptor(1001, "chengdu1001", "成都麻将",
                GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                "SC", "CD", "chengdu1001-v1.0.0"));

        GameRegistry projection = GameRegistryFactory.productionFrom(List.of(legacy, current));
        check(projection.require(1001).descriptor().gameId() == 1001,
                "current identity must remain in production projection");
        expectFailure(() -> projection.require(516), "legacy identity must stay outside production projection");
    }

    private static void incompleteProjectionFailsClosed() {
        GameRegistry projection = GameRegistryFactory.productionFrom(List.of(provider(
                new GameDescriptor(1001, "chengdu1001", "成都麻将", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", "chengdu1001-v1.0.0"))));
        RuntimeException failure = expectFailure(
                () -> GameRegistryFactory.requireCompleteProductionProjection(projection),
                "incomplete current projection must fail closed");
        check(failure.getMessage().contains("currentCount=1"), "incomplete count must be machine-readable");
    }

    private static void conflictingCurrentIdentityFailsClosed() {
        RuntimeException failure = expectFailure(() -> GameRegistryFactory.productionFrom(List.of(provider(
                new GameDescriptor(1001, "wrong-code", "成都麻将", GameCategory.MAHJONG,
                        "mahjong:xue-zhan", RegionScope.CITY, "SC", "CD", "chengdu1001-v1.0.0")))),
                "conflicting current identity must fail closed");
        check(failure.getMessage().contains("PRODUCTION_IDENTITY_CONFLICT"),
                "identity conflict must be machine-readable");
    }

    private static GameProvider provider(GameDescriptor descriptor) {
        return new GameProvider() {
            @Override public GameDescriptor descriptor() { return descriptor; }
            @Override public GameRoomFactory roomFactory() { return context -> null; }
        };
    }

    private static RuntimeException expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label);
        } catch (RuntimeException expected) {
            return expected;
        }
    }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
