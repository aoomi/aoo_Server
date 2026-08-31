package business.global.pk.njpdk;

import com.aoo.bcg.gamespi.GameCategory;
import org.junit.jupiter.api.Test;
import com.aoo.bcg.poker.PaoDeKuaiFamily;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NJPDKGameProviderTest {
    @Test
    void exposesPokerDescriptorAndRoomFactory() {
        NJPDKGameProvider provider = new NJPDKGameProvider();

        assertEquals(GameCategory.POKER, provider.descriptor().category());
        assertEquals("njpdk", provider.descriptor().code());
        assertNotNull(provider.roomFactory());
        assertEquals("poker:pao-de-kuai", provider.pokerFamily().familyCode());
        assertEquals(provider.pokerFamily().familyCode(), provider.descriptor().family());
        PaoDeKuaiFamily family=(PaoDeKuaiFamily)provider.pokerFamily();
        assertEquals(true, String.valueOf(family.rules().recognize(List.of(114,214,314), null))
                .contains("type=SPECIAL_TRIPLE_BOMB"));
        provider.validatePokerCategory();
    }

    @Test void exposesAuthoritativeTwoThreeAndFourPlayerLayouts() {
        NJPDKGameProvider provider = new NJPDKGameProvider();
        for (int count : List.of(2, 3, 4)) {
            var session = provider.createAuthoritativeSession(new RoomCreationContext(6200 + count, 10, Map.of("playerCount", count))).orElseThrow();
            assertEquals(count, session.viewFor(10).get("playerCount"));
            assertEquals(count, session.viewFor(10).get("seatLimit"));
            assertEquals("legacy-equivalent-1", session.viewFor(10).get("playVersion"));
            assertEquals(Map.of("addDouble", false, "robDoor", false, "openCard", false), session.viewFor(10).get("capabilities"));
        }
        assertThrows(IllegalArgumentException.class, () -> provider.createAuthoritativeSession(new RoomCreationContext(1, 10, Map.of("playerCount", 1))));
        assertThrows(IllegalArgumentException.class, () -> provider.createAuthoritativeSession(new RoomCreationContext(1, 10, Map.of("playerCount", 5))));
    }
}
