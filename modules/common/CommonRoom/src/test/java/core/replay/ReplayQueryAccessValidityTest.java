package core.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayQueryAccessValidityTest {
    @Test void activeFutureAndNullExpiryCodesPermitTheNonParticipantReplay() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "DATEADD('SECOND',60,CURRENT_TIMESTAMP(3))");
            f.assertChunk(188001);
            f.execute("UPDATE replay_short_code SET expires_at=NULL");
            f.assertChunk(188001);
        }
    }

    @Test void expiredFutureAndActiveElapsedCodesDenyTheRealReplayPath() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("EXPIRED", "DATEADD('SECOND',60,CURRENT_TIMESTAMP(3))");
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1901, 0, 0, 10));
            f.execute("UPDATE replay_short_code SET status='ACTIVE',expires_at=DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1901, 0, 0, 10));
            f.execute("UPDATE replay_short_code SET expires_at=CURRENT_TIMESTAMP(3)");
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1901, 0, 0, 10));
        }
    }

    @Test void participantsAndArchivedParticipantsKeepOwnAccessAfterCodeExpiry() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("EXPIRED", "DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            f.execute("INSERT INTO replay_participant_archive VALUES(1901,0,188003,1)");
            f.assertChunk(188002);
            f.assertChunk(188003);
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1901, 0, 0, 10));
        }
    }

    @Test void codeGrantCannotCrossPlayerRoomOrSet() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "NULL");
            assertThrows(SecurityException.class, () -> f.service.replay(188004, 1901, 0, 0, 10));
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1902, 0, 0, 10));
            assertThrows(SecurityException.class, () -> f.service.replay(188001, 1901, 1, 0, 10));
        }
    }

    @Test void nullSetAuthorizationKeepsRoomWideScopeButRequiresValidCode() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "NULL");
            f.authorizeRoom(188001, 1901);
            assertThrows(SecurityException.class, () -> f.authorizeRoom(188001, 1902));
            f.execute("UPDATE replay_short_code SET expires_at=DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            assertThrows(SecurityException.class, () -> f.authorizeRoom(188001, 1901));
            f.authorizeRoom(188002, 1901);
        }
    }

    @Test void sharedPerspectiveIndependentlyRejectsAnElapsedCode() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            var method = RecordReplayQueryService.class.getDeclaredMethod("replayPerspective", Connection.class, long.class, long.class, int.class);
            method.setAccessible(true);
            try (Connection connection = f.source.getConnection()) {
                InvocationTargetException denied = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(f.service, connection, 188001L, 1901L, 0));
                assertInstanceOf(SecurityException.class, denied.getCause());
            }
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final JdbcDataSource source = new JdbcDataSource();
        private final Connection keeper;
        private final RecordReplayQueryService service;
        Fixture() throws Exception {
            source.setURL("jdbc:h2:mem:query-validity-" + UUID.randomUUID() + ";MODE=MySQL");
            keeper = source.getConnection();
            execute("SET TIME ZONE 'UTC'");
            execute("CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT,seat_id INT)");
            execute("CREATE TABLE replay_participant_archive(room_id BIGINT,set_id INT,player_id BIGINT,seat_id INT)");
            execute("CREATE TABLE replay_short_code(short_code VARCHAR(16) PRIMARY KEY,room_id BIGINT,set_id INT,status VARCHAR(16),expires_at TIMESTAMP(3))");
            execute("CREATE TABLE replay_short_code_access(short_code VARCHAR(16),player_id BIGINT)");
            String eventFields = "(room_id BIGINT,set_id INT,event_sequence BIGINT,visibility VARCHAR(32),owner_player_id BIGINT,message_id VARCHAR(128),schema_version INT,play_version VARCHAR(32),payload VARBINARY(1024),content_hash VARCHAR(64))";
            execute("CREATE TABLE perspective_replay_event" + eventFields);
            execute("CREATE TABLE perspective_replay_event_archive" + eventFields);
            execute("INSERT INTO replay_participant VALUES(1901,0,188002,0)");
            execute("INSERT INTO replay_short_code_access VALUES('234561',188001)");
            try (var q = keeper.prepareStatement("INSERT INTO perspective_replay_event VALUES(1901,0,1,'PUBLIC',0,'fixture.terminal',1,'fixture-v1',?,?)")) {
                q.setBytes(1, "{\"finished\":true}".getBytes(StandardCharsets.UTF_8));
                q.setString(2, "0".repeat(64)); // Authorization fixture only; not an integrity proof.
                q.executeUpdate();
            }
            service = new RecordReplayQueryService(source, new ObjectMapper());
        }
        void code(String status, String expires) throws Exception {
            execute("INSERT INTO replay_short_code VALUES('234561',1901,0,'" + status + "'," + expires + ")");
        }
        void execute(String sql) throws Exception { try (var q = keeper.createStatement()) { q.execute(sql); } }
        void assertChunk(long playerId) {
            var chunk = service.replay(playerId, 1901, 0, 0, 10);
            assertEquals(1, chunk.events().size());
            assertEquals("fixture.terminal", chunk.events().getFirst().messageId());
        }
        void authorizeRoom(long playerId, long roomId) throws Exception {
            var method = RecordReplayQueryService.class.getDeclaredMethod("authorize", Connection.class, long.class, long.class, Integer.class);
            method.setAccessible(true);
            try (Connection c = source.getConnection()) {
                try { method.invoke(service, c, playerId, roomId, null); }
                catch (InvocationTargetException error) {
                    if (error.getCause() instanceof SecurityException denied) throw denied;
                    throw error;
                }
            }
        }
        @Override public void close() throws Exception { keeper.close(); }
    }
}
