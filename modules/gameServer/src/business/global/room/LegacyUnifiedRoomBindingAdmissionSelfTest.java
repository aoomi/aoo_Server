package business.global.room;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;

import java.util.Map;

/** Executable contract checks for machine-readable compatibility binding failures. */
public final class LegacyUnifiedRoomBindingAdmissionSelfTest {
    private LegacyUnifiedRoomBindingAdmissionSelfTest() { }

    public static void main(String[] args) {
        missingProviderIsMachineReadable();
        missingAuthorityFactoryIsMachineReadable();
        System.out.println("LegacyUnifiedRoomBindingAdmissionSelfTest: PASS");
    }

    private static void missingProviderIsMachineReadable() {
        RoomCreationContext.CompatibilityOrigin origin = historicalOrigin();
        RuntimeException failure = expectFailure(() -> LegacyUnifiedRoomBinding.requireProvider(
                GameRegistryFactory.compatibility(), 701L, origin));
        assertFields(failure, "COMPATIBILITY_PROVIDER_UNRESOLVED", 701L, "legacy-equivalent-1",
                "historical-compatibility");
    }

    private static void missingAuthorityFactoryIsMachineReadable() {
        RoomCreationContext.CompatibilityOrigin origin = historicalOrigin();
        GameProvider provider = new GameProvider() {
            @Override public GameDescriptor descriptor() { return origin.staticIdentity(); }
            @Override public com.aoo.bcg.gamespi.GameRoomFactory roomFactory() {
                return context -> new GameRoomHandle(context.roomId(), 618,
                        origin.staticIdentity().version(), Map.of());
            }
        };
        RoomCreationContext context = new RoomCreationContext(702L, 99L, Map.of(), origin);
        RuntimeException failure = expectFailure(() -> LegacyUnifiedRoomBinding.requireAuthority(provider, context));
        assertFields(failure, "COMPATIBILITY_AUTHORITY_FACTORY_MISSING", 702L,
                "legacy-equivalent-1", "historical-compatibility");
    }

    private static void assertFields(RuntimeException failure, String reason, long roomId,
                                     String playVersion, String source) {
        String message = failure.getMessage();
        check(message.contains("ROOM_ADMISSION_REJECTED|reason=" + reason), "reason");
        check(message.contains("roomId=" + roomId), "room id");
        check(message.contains("gameId=618"), "game id");
        check(message.contains("gameCode=xcpdk"), "game code");
        check(message.contains("playVersion=" + playVersion), "play version");
        check(message.contains("source=" + source), "source");
    }

    private static RoomCreationContext.CompatibilityOrigin historicalOrigin() {
        GameDescriptor descriptor = new GameDescriptor(618, "xcpdk", "宣城跑得快",
                GameCategory.POKER, "poker:pao-de-kuai", RegionScope.CITY, "AH", "XC",
                "legacy-equivalent-1");
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

    private static String hex() {
        return "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    }

    private static RuntimeException expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected machine-readable rejection");
        } catch (RuntimeException expected) {
            return expected;
        }
    }

    private static void check(boolean condition, String field) {
        if (!condition) throw new AssertionError("missing machine field: " + field);
    }
}
