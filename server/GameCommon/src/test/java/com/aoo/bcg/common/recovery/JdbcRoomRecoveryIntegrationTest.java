package com.aoo.bcg.common.recovery;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.aoo.bcg.common.reconnect.JdbcPerspectiveRoomEventJournal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AOO_DB_IT_URL", matches = ".+")
class JdbcRoomRecoveryIntegrationTest {
    @Test void crossNodeTakeoverFencesOldOwnerAndPreservesPlayerPerspective() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),
                System.getenv("AOO_DB_IT_USER"), System.getenv("AOO_DB_IT_PASSWORD"));
        var mapper = new ObjectMapper();
        var leases = new JdbcRoomLeaseStore(dataSource, Clock.systemUTC());
        var snapshots = new JdbcRoomSnapshotStore(dataSource, mapper);
        var journal = new JdbcPerspectiveRoomEventJournal(dataSource, mapper);
        long roomId = System.currentTimeMillis();
        RoomLease first = leases.acquire(roomId, "node-a", Duration.ofMillis(50));
        snapshots.save(new RoomSnapshot(roomId, 516, "v1", "c1", first.fencingToken(), 1,
                Instant.now(), Map.of("score", 10)));
        journal.appendPublic(roomId, 2, "PLAY", Map.of("seat", 1));
        journal.appendPrivate(roomId, 2, 11, "DRAW", Map.of("card", 23));
        journal.appendPrivate(roomId, 2, 12, "DRAW", Map.of("card", 47));
        journal.appendPrivate(roomId, 2, 12, "DRAW", Map.of("card", 47));
        assertThrows(IllegalStateException.class,
                () -> journal.appendPrivate(roomId, 2, 12, "DRAW", Map.of("card", 99)));
        Thread.sleep(70);
        RoomLease second = leases.acquire(roomId, "node-b", Duration.ofSeconds(5));
        assertTrue(second.fencingToken() > first.fencingToken());
        snapshots.save(new RoomSnapshot(roomId, 516, "v1", "c1", second.fencingToken(), 2,
                Instant.now(), Map.of("score", 20)));
        assertThrows(IllegalStateException.class, () -> snapshots.save(new RoomSnapshot(roomId, 516,
                "v1", "c1", first.fencingToken(), 99, Instant.now(), Map.of("score", 999))));
        var player11 = journal.after(roomId, 11, 1, 10);
        var player12 = journal.after(roomId, 12, 1, 10);
        assertEquals(2, player11.size());
        assertEquals(2, player12.size());
        assertTrue(player11.stream().anyMatch(event -> event.payload().toString().contains("23")));
        assertFalse(player11.stream().anyMatch(event -> event.payload().toString().contains("47")));
        assertFalse(player12.stream().anyMatch(event -> event.payload().toString().contains("23")));
    }
}
