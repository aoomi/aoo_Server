package business.global.pk.xcpdk;

import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import cenum.PrizeType;
import com.aoo.bcg.gamespi.RoomCreationContext;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XCPDKGameProviderTest {
    private final XCPDKGameProvider provider = new XCPDKGameProvider();

    @Test void bindsExecutablePaodekuaiFamily() {
        assertEquals("poker:pao-de-kuai", provider.pokerFamily().familyCode());
        assertEquals(provider.pokerFamily().familyCode(), provider.descriptor().family());
        provider.validatePokerCategory();
    }

    @Test void rejectsMissingConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(1, 2, Map.of())));
    }

    @Test void rejectsConfigurationForAnotherGame() {
        BaseRoomConfigure<CXCPDK_CreateRoom> configuration = new BaseRoomConfigure<>(
                PrizeType.None, new GameType(999, "wrong", 2), new CXCPDK_CreateRoom());
        assertThrows(IllegalArgumentException.class, () -> provider.roomFactory().create(
                new RoomCreationContext(1, 2, Map.of("baseRoomConfigure", configuration))));
    }

    @Test void productionServiceDescriptorLoadsProviderWithLegacyProtocolTypes() throws Exception {
        assertNotNull(Class.forName("jsproto.c2s.cclass.room.BaseRoomConfigure"));
        long count = ServiceLoader.load(com.aoo.bcg.gamespi.GameProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(candidate -> candidate.getClass().equals(XCPDKGameProvider.class))
                .count();
        assertEquals(1, count, "XCPDK must have exactly one production ServiceLoader descriptor");
    }

    @Test void createsServerAuthoritativeSession() {
        var authority = provider.createAuthoritativeSession(
                new RoomCreationContext(7001, 9001, Map.of("seatLimit", 4)));
        assertTrue(authority.isPresent(), "XCPDK must expose its server-authoritative state");
        assertTrue(authority.orElseThrow().invariantViolations().isEmpty());
    }

    @Test void validatesAuthoritativePlayerCount() {
        for (int count : java.util.List.of(2, 3, 4)) {
            var session = provider.createAuthoritativeSession(new RoomCreationContext(7100 + count, 9001, Map.of("playerCount", count))).orElseThrow();
            assertEquals(count, session.viewFor(9001).get("playerCount"));
        }
        assertThrows(IllegalArgumentException.class, () -> provider.createAuthoritativeSession(new RoomCreationContext(1, 2, Map.of("playerCount", 1))));
        assertThrows(IllegalArgumentException.class, () -> provider.createAuthoritativeSession(new RoomCreationContext(1, 2, Map.of("playerCount", 5))));
    }
}
