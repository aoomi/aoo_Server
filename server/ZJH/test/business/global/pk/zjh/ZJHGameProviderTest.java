package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RoomCreationContext;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.ServiceLoader;
import static org.junit.jupiter.api.Assertions.*;

class ZJHGameProviderTest {
    @Test void providerCreatesServerSeededAuthoritativeTable() {
        ZJHGameProvider provider = new ZJHGameProvider();
        ZJHTable table = provider.roomFactory().create(new RoomCreationContext(10, 20, Map.of("seatLimit", 3, "randomSeed", 88L))).requireLegacyRoom(ZJHTable.class);
        table.join(0, 20); table.join(1, 21); table.join(2, 22);
        table.ready(0, true); table.ready(1, true); table.ready(2, true); table.start();
        assertEquals(ZJHTable.State.PLAYING, table.state());
        assertEquals(3, table.handView(20, 0).size());
        assertEquals(java.util.List.of(0, 0, 0), table.handView(20, 1));
        assertNotEquals(88L, table.randomSeed(), "client/room rules must not choose the shuffle seed");
    }

    @Test void serviceLoaderDiscoversZjh() {
        assertTrue(ServiceLoader.load(GameProvider.class).stream().anyMatch(p -> p.get().descriptor().gameId() == 9));
    }
}
