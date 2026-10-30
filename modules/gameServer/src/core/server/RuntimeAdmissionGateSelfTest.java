package core.server;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.StatePayload;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;

import java.util.List;
import java.util.Map;

/** Executable gate checks; no database or production runtime is started. */
public final class RuntimeAdmissionGateSelfTest {
    private RuntimeAdmissionGateSelfTest() { }

    public static void main(String[] args) {
        currentIdentityUsesTypedAdmission();
        unregisteredRoomFailsClosedWithMachineFields();
        ambiguousIdentityFailsClosedWithMachineFields();
        explicitCompatibilityOriginAdmitsOnlyLegacyBoundary();
        compatibilityOriginRequiresCompleteHistoricalProof();
        System.out.println("RuntimeAdmissionGateSelfTest: PASS");
    }

    private static void currentIdentityUsesTypedAdmission() {
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        GameRoomHandle room = new GameRoomHandle(41L, 1001, "chengdu1001-v1.0.0",
                session(Map.of("players", Map.of("2", 7001L))),
                new RoomRuntimeIdentity("room-41", 1L, 9L, 0L));
        rooms.bind(room);
        RuntimeException missingRegistry = expectFailure(() -> GameServerUnifiedRuntime.requireAdmission(
                rooms, null, 41L, "chengdu1001-v1.0.0"));
        check(missingRegistry.getMessage().contains("reason=ROOM_CURRENT_IDENTITY_UNRESOLVED"),
                "missing production registry must fail closed");
        var productionGames = GameRegistryFactory.productionFrom(List.of(new GameProvider() {
            @Override public GameDescriptor descriptor() {
                return new GameDescriptor(1001, "chengdu1001", "成都血战",
                        com.aoo.bcg.gamespi.GameCategory.MAHJONG, "mahjong:xue-zhan", RegionScope.CITY,
                        "SC", "CD", "chengdu1001-v1.0.0");
            }
            @Override public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
                return context -> new GameRoomHandle(context.roomId(), 1001, "chengdu1001-v1.0.0", Map.of());
            }
        }));
        GameServerUnifiedRuntime.RoomAdmission admission = GameServerUnifiedRuntime.requireAdmission(
                rooms, productionGames, 41L, "chengdu1001-v1.0.0");
        check(admission.compatibilityOrigin() == null, "current identity must not carry CompatibilityOrigin");
        check(admission.room().runtimeIdentity() != null, "current identity must carry runtime fence");
    }

    private static void unregisteredRoomFailsClosedWithMachineFields() {
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        RuntimeException failure = expectFailure(() -> GameServerUnifiedRuntime.requireAdmission(
                rooms, null, 42L, "missing-v1"));
        check(failure.getMessage().contains("reason=ROOM_IDENTITY_NOT_REGISTERED"), "missing reason");
        check(failure.getMessage().contains("gameId=0"), "missing game id");
        check(failure.getMessage().contains("gameCode=unknown"), "missing game code");
        check(failure.getMessage().contains("playVersion=missing-v1"), "missing play version");
        check(failure.getMessage().contains("source=missing"), "missing source");
    }

    private static void ambiguousIdentityFailsClosedWithMachineFields() {
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        RoomCreationContext.CompatibilityOrigin origin = historicalOrigin();
        GameRoomHandle room = new GameRoomHandle(43L, 618, origin.staticIdentity().version(),
                session(Map.of("players", Map.of("0", 7001L))), origin, null,
                new RoomRuntimeIdentity("room-43", 1L, 10L, 0L));
        rooms.bind(room);
        RuntimeException failure = expectFailure(() -> GameServerUnifiedRuntime.requireAdmission(
                rooms, null, 43L, origin.staticIdentity().version()));
        check(failure.getMessage().contains("reason=ROOM_IDENTITY_AMBIGUOUS"), "ambiguous reason");
        check(failure.getMessage().contains("gameId=618"), "ambiguous game id");
        check(failure.getMessage().contains("gameCode=xcpdk"), "ambiguous game code");
        check(failure.getMessage().contains("source=historical-compatibility"), "ambiguous source");
    }

    private static void compatibilityOriginRequiresCompleteHistoricalProof() {
        RoomCreationContext.CompatibilityOrigin origin = historicalOrigin();
        check("historical-compatibility".equals(origin.source()), "historical source");
        check(origin.snapshotSource() != null, "snapshot source required");
        check(origin.snapshotSource().sourceHash().length() == 64, "source hash required");
        expectFailure(() -> new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE,
                origin.staticIdentity(), origin.providerKey(), origin.releaseId(),
                origin.contractVersion(), origin.snapshotSchemaVersion()));
    }

    private static void explicitCompatibilityOriginAdmitsOnlyLegacyBoundary() {
        RoomCreationContext.CompatibilityOrigin origin = historicalOrigin();
        ProductionRoomRealtimeService.requireCompatibilityAdmission(44L, origin.staticIdentity().version(), origin);
        ProductionRoomReconnectService.requireCompatibilityAdmission(44L, origin.staticIdentity().version(), origin);
        RuntimeException missingVersion = expectFailure(() -> ProductionRoomReconnectService
                .requireCompatibilityAdmission(44L, " ", origin));
        check(missingVersion.getMessage().contains("reason=COMPATIBILITY_PLAY_VERSION_REQUIRED"),
                "blank compatibility version must fail closed");
        check(missingVersion.getMessage().contains("gameId=618"), "blank version game id");
        check(missingVersion.getMessage().contains("gameCode=xcpdk"), "blank version game code");
        check(missingVersion.getMessage().contains("source=historical-compatibility"), "blank version source");
        RuntimeException failure = expectFailure(() -> ProductionRoomRealtimeService
                .requireCompatibilityAdmission(44L, origin.staticIdentity().version(), null));
        check(failure.getMessage().contains("reason=COMPATIBILITY_ORIGIN_REQUIRED"), "legacy origin required");
    }

    private static RoomCreationContext.CompatibilityOrigin historicalOrigin() {
        GameDescriptor descriptor = new GameDescriptor(618, "xcpdk", "宣城跑得快",
                com.aoo.bcg.gamespi.GameCategory.POKER, "poker:pao-de-kuai", RegionScope.CITY,
                "AH", "XC", "legacy-equivalent-1");
        var metadata = new RoomCreationContext.CompatibilityOrigin.SnapshotMetadataSource(
                descriptor, descriptor.version(), hex(), hex(), "card-v1", "event-v1", "protocol-v1",
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION, "index-v1", "AH/XC", hex(),
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION);
        return new RoomCreationContext.CompatibilityOrigin(
                RoomCreationContext.CompatibilityOrigin.HISTORICAL_SOURCE, descriptor,
                "business.global.pk.xcpdk.XCPDKGameProvider", descriptor.version(),
                RoomCreationContext.CompatibilityOrigin.CONTRACT_VERSION,
                RoomCreationContext.CompatibilityOrigin.SNAPSHOT_SCHEMA_VERSION, metadata);
    }

    private static String hex() { return "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; }

    private static AuthoritativeGameSession session(Map<String, Object> state) {
        return new AuthoritativeGameSession() {
            @Override public GameCommandResult execute(GameCommandRequest request) {
                return new GameCommandResult("test.response", request.requestId(), Map.of());
            }
            @Override public Map<String, Object> viewFor(long playerId) { return state; }
            @Override public Map<String, Object> authoritativeState() { return state; }
            @Override public long stateVersion() { return 0L; }
            @Override public OperationDeadline operationDeadline() { return OperationDeadline.none(); }
            @Override public OperationDeadlineArbiter deadlineArbiter() { return new OperationDeadlineArbiter(); }
            @Override public List<String> invariantViolations() { return List.of(); }
            @Override public SettlementPayload settlement(int roundNo, String playVersion) {
                return new SettlementPayload(1, roundNo, playVersion, Map.of());
            }
        };
    }

    private static RuntimeException expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected fail-closed rejection");
        } catch (RuntimeException expected) {
            return expected;
        }
    }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
