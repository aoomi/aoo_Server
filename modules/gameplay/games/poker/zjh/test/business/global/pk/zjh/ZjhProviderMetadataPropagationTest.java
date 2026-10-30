package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZjhProviderMetadataPropagationTest {
    @Test
    void roomFactoryCarriesExplicitSnapshotMetadataSource() {
        ZjhGameProvider provider = new ZjhGameProvider();
        RoomSnapshotMetadataSource source = new RoomSnapshotMetadataSource(
                1L,
                "a".repeat(64),
                "b".repeat(64),
                "card-v1",
                "event-v1",
                "protocol-v1",
                "snapshot-v1",
                1L,
                "NATIONAL",
                "c".repeat(64));

        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(
                2997001L, 2001L, Map.of(), null, source));

        assertSame(source, room.requireRoomSnapshotMetadataSource());
    }

    @Test
    void missingSnapshotMetadataRemainsFailClosed() {
        ZjhGameProvider provider = new ZjhGameProvider();
        GameRoomHandle room = provider.roomFactory().create(
                new RoomCreationContext(2997002L, 2002L, Map.of()));

        assertThrows(IllegalStateException.class, room::requireRoomSnapshotMetadataSource);
    }
}
