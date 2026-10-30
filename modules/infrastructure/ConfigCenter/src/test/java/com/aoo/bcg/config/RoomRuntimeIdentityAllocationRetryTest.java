package com.aoo.bcg.config;

import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused contract for retry-safe, fail-closed runtime identity acquisition. */
final class RoomRuntimeIdentityAllocationRetryTest {
    private JdbcDataSource dataSource;

    @BeforeEach
    void createAuthorityTable() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:room-runtime-allocation-retry;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS aoo_room_runtime_identity_authority");
            statement.executeUpdate("CREATE TABLE aoo_room_runtime_identity_authority ("
                    + "room_id BIGINT PRIMARY KEY, room_instance_id VARCHAR(128) NOT NULL,"
                    + "lifecycle_epoch BIGINT NOT NULL, fencing_token BIGINT NOT NULL,"
                    + "state_version BIGINT NOT NULL, lifecycle_state VARCHAR(16) NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "CONSTRAINT uq_generation UNIQUE(room_instance_id,lifecycle_epoch,fencing_token),"
                    + "CONSTRAINT valid_state CHECK(lifecycle_state IN ('ACTIVE','DESTROYED'))"
                    + ")");
        }
    }

    @Test
    void retriesReuseTheDurableActiveIdentityInsteadOfInsertingAnotherPrimaryKeyRow() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity first = new RoomRuntimeIdentity("first", 1, 10, 0);
        RoomRuntimeIdentity newlyIssuedOnRetry = new RoomRuntimeIdentity("second", 2, 11, 0);

        RoomRuntimeIdentityStore.Acquisition created = store.createOrReuse(1001, first);
        RoomRuntimeIdentityStore.Acquisition retried = store.createOrReuse(1001, newlyIssuedOnRetry);

        assertTrue(created.created());
        assertFalse(retried.created());
        assertEquals(first, retried.entry().identity());
        assertEquals(first, store.require(1001).identity());
    }

    @Test
    void destroyedTombstoneRejectsRetryWithoutRemountingOrReplacingTheIdentity() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity first = new RoomRuntimeIdentity("first", 1, 10, 0);
        store.create(1001, first);
        store.destroy(1001, first);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> store.createOrReuse(1001, new RoomRuntimeIdentity("replacement", 2, 11, 0)));

        assertEquals("ROOM_RUNTIME_IDENTITY_DESTROYED", failure.getMessage());
        assertFalse(store.require(1001).active());
        assertEquals(first, store.require(1001).identity());
    }

    @Test
    void generationConflictStillFailsClosedForASecondRoom() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity first = new RoomRuntimeIdentity("same-generation", 1, 10, 0);
        store.createOrReuse(1001, first);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> store.createOrReuse(1002,
                        new RoomRuntimeIdentity("same-generation", 1, 10, 0)));

        assertEquals("ROOM_RUNTIME_IDENTITY_CONFLICT", failure.getMessage());
        assertTrue(store.find(1002).isEmpty());
    }

    @Test
    void diagnosticAuditCarriesAuthorityFieldsWithoutChangingFailClosedResults() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity first = new RoomRuntimeIdentity("audit-instance", 3, 19, 0);
        RoomRuntimeIdentity retry = new RoomRuntimeIdentity("retry-instance", 4, 20, 0);
        RoomRuntimeIdentityStore.AuditContext context =
                new RoomRuntimeIdentityStore.AuditContext(
                        "attempt-1001", "JdbcGatewayRoomAuthority.create.requireOrCreateRuntime");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.err;
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            assertTrue(store.createOrReuse(1001, first, context).created());
            assertFalse(store.createOrReuse(1001, retry, context).created());
            store.destroy(1001, first, context);
        } finally {
            System.setErr(original);
        }
        String log = captured.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("event=identity_authority"));
        assertTrue(log.contains("attemptId=attempt-1001"));
        assertTrue(log.contains("roomId=1001"));
        assertTrue(log.contains("roomInstanceId=audit-instance"));
        assertTrue(log.contains("lifecycleEpoch=3"));
        assertTrue(log.contains("fencingToken=19"));
        assertTrue(log.contains("stateVersion=0"));
        assertTrue(log.contains("sqlOutcome=INSERTED"));
        assertTrue(log.contains("sqlOutcome=ACTIVE_REUSE"));
        assertTrue(log.contains("sqlOutcome=TOMBSTONED"));
        assertTrue(log.contains("retryCaller=JdbcGatewayRoomAuthority.create.requireOrCreateRuntime"));
        assertFalse(log.contains("accessToken"));
        assertFalse(log.contains("sessionId"));
        assertFalse(log.contains("password"));
    }
}
