package core.replay;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayCodeAccessValidityTest {
    @Test void noGrantStillDeniesANonParticipant() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "DATEADD('SECOND',60,CURRENT_TIMESTAMP(3))");
            assertFalse(f.repo.mayView(88001, 901, 0));
        }
    }

    @Test void activeFutureAndNullExpiryGrantsPermitNonParticipants() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "DATEADD('SECOND',60,CURRENT_TIMESTAMP(3))");
            f.grant();
            assertTrue(f.repo.mayView(88001, 901, 0));
            f.execute("UPDATE replay_short_code SET expires_at=NULL WHERE short_code='123456'");
            assertTrue(f.repo.mayView(88001, 901, 0));
        }
    }

    @Test void expiredStatusRevokesTheObservedFutureExpiryGrant() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("EXPIRED", "DATEADD('SECOND',60,CURRENT_TIMESTAMP(3))");
            f.grant();
            assertFalse(f.repo.mayView(88001, 901, 0));
        }
    }

    @Test void activeButElapsedExpiryDoesNotAuthorizeTheGrant() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            f.grant();
            assertFalse(f.repo.mayView(88001, 901, 0));
            f.execute("UPDATE replay_short_code SET expires_at=CURRENT_TIMESTAMP(3) WHERE short_code='123456'");
            assertFalse(f.repo.mayView(88001, 901, 0));
        }
    }

    @Test void participantAndArchivedParticipantRetainTheirOwnAccessWhenCodeIsInvalid() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("EXPIRED", "DATEADD('SECOND',-60,CURRENT_TIMESTAMP(3))");
            f.grant();
            f.execute("INSERT INTO replay_participant VALUES(901,0,88002)");
            f.execute("INSERT INTO replay_participant_archive VALUES(901,0,88003)");
            assertFalse(f.repo.mayView(88001, 901, 0));
            assertTrue(f.repo.mayView(88002, 901, 0));
            assertTrue(f.repo.mayView(88003, 901, 0));
        }
    }

    @Test void anActiveGrantRemainsBoundToItsPlayerRoomAndSet() throws Exception {
        try (Fixture f = new Fixture()) {
            f.code("ACTIVE", "NULL");
            f.grant();
            assertFalse(f.repo.mayView(88002, 901, 0));
            assertFalse(f.repo.mayView(88001, 902, 0));
            assertFalse(f.repo.mayView(88001, 901, 1));
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final JdbcDataSource source = new JdbcDataSource();
        private final Connection keeper;
        private final JdbcReplayCodeRepository repo;

        Fixture() throws Exception {
            source.setURL("jdbc:h2:mem:replay-access-" + UUID.randomUUID() + ";MODE=MySQL");
            keeper = source.getConnection();
            execute("SET TIME ZONE 'UTC'");
            execute("CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT)");
            execute("CREATE TABLE replay_participant_archive(room_id BIGINT,set_id INT,player_id BIGINT)");
            execute("CREATE TABLE replay_short_code(short_code VARCHAR(16) PRIMARY KEY,room_id BIGINT,set_id INT,status VARCHAR(16),expires_at TIMESTAMP(3) NULL)");
            execute("CREATE TABLE replay_short_code_access(short_code VARCHAR(16),player_id BIGINT,granted_at TIMESTAMP(3))");
            repo = new JdbcReplayCodeRepository(source);
        }

        void code(String status, String expiryExpression) throws Exception {
            // Constants belong only to this synthetic in-memory fixture, never to external input.
            execute("INSERT INTO replay_short_code VALUES('123456',901,0,'" + status + "'," + expiryExpression + ")");
        }

        void grant() throws Exception {
            execute("INSERT INTO replay_short_code_access VALUES('123456',88001,CURRENT_TIMESTAMP(3))");
        }

        void execute(String sql) throws Exception {
            try (var statement = keeper.createStatement()) { statement.execute(sql); }
        }

        @Override public void close() throws Exception { keeper.close(); }
    }
}
