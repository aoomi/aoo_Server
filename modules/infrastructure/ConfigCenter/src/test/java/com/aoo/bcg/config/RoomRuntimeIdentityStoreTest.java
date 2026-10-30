package com.aoo.bcg.config;

import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomRuntimeIdentityStoreTest {
    private JdbcDataSource dataSource;

    @BeforeEach
    void createAuthorityTable() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:room-runtime-authority;MODE=MySQL;DB_CLOSE_DELAY=-1");
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
    void createsAndReadsOnlyExplicitIdentity() {
        RoomRuntimeIdentity identity = identity(0);
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);

        assertEquals(identity, store.create(41, identity).identity());
        assertEquals(identity, store.require(41).identity());
        assertTrue(store.require(41).active());
    }

    @Test
    void compareAndSetIsAtomicAndRejectsStaleOrRegressedState() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity first = identity(0);
        RoomRuntimeIdentity second = identity(1);
        store.create(42, first);

        assertEquals(second, store.compareAndSet(42, first, second).identity());
        assertThrows(IllegalStateException.class, () -> store.compareAndSet(42, first, identity(2)));
        assertThrows(IllegalArgumentException.class, () -> store.compareAndSet(42, second, identity(-1)));
        assertThrows(IllegalArgumentException.class, () -> store.compareAndSet(42, second,
                new RoomRuntimeIdentity("instance-42", 6, 11, 2)));
        assertThrows(IllegalArgumentException.class, () -> store.compareAndSet(42, second,
                new RoomRuntimeIdentity("instance-42", 7, 10, 2)));
        assertThrows(IllegalArgumentException.class, () -> store.compareAndSet(42, second,
                new RoomRuntimeIdentity("instance-42", 0, 99, 2)));
    }

    @Test
    void destroyLeavesTombstoneAndRejectsOldRemount() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity identity = identity(0);
        store.create(43, identity);

        RoomRuntimeIdentityStore.Entry destroyed = store.destroy(43, identity);
        assertEquals(RoomRuntimeIdentityStore.LifecycleState.DESTROYED, destroyed.state());
        assertThrows(IllegalStateException.class, () -> store.create(43, identity));
        assertThrows(IllegalStateException.class, () -> store.compareAndSet(43, identity, identity(1)));
        assertThrows(IllegalStateException.class, () -> store.destroy(43, identity));
    }

    @Test
    void duplicateGenerationAndMissingRowsFailClosed() {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        RoomRuntimeIdentity identity = identity(0);
        store.create(44, identity);
        assertThrows(IllegalStateException.class, () -> store.create(44, identity));
        assertThrows(IllegalStateException.class, () -> store.create(45, identity));
        assertTrue(store.find(999).isEmpty());
        assertEquals("ROOM_RUNTIME_IDENTITY_NOT_FOUND",
                assertThrows(IllegalStateException.class, () -> store.require(999)).getMessage());
    }

    @Test
    void rollbackFailureDoesNotReplaceOriginalConflictOrCreateIdentity() {
        RoomRuntimeIdentity identity = identity(0);
        new RoomRuntimeIdentityStore(dataSource).create(44, identity);
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(rollbackFailingDataSource(dataSource));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> store.createOrReuse(45, identity));

        assertEquals("ROOM_RUNTIME_IDENTITY_CONFLICT", failure.getMessage());
        assertTrue(failure.getCause() instanceof SQLException);
        assertTrue(store.find(45).isEmpty());
    }

    @Test
    void callerTransactionOwnsCommitAndRollbackVisibility() throws Exception {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        store.create(42, identity(0));
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            store.compareAndSet(connection, 42, identity(0), identity(1));
            assertEquals(identity(0), store.require(42).identity());
            connection.rollback();
            assertEquals(identity(0), store.require(42).identity());
            store.compareAndSet(connection, 42, identity(0), identity(1));
            connection.commit();
            assertEquals(identity(1), store.require(42).identity());
        }
    }

    @Test
    void callerConnectionMustBeTransactionalAndRetainsIdentityGuards() throws Exception {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        store.create(42, identity(0));
        try (Connection connection = dataSource.getConnection()) {
            assertThrows(IllegalArgumentException.class,
                    () -> store.compareAndSet(connection, 42, identity(0), identity(1)));
            connection.setAutoCommit(false);
            assertThrows(IllegalArgumentException.class,
                    () -> store.compareAndSet(connection, 42, identity(0), identity(0)));
            assertThrows(IllegalArgumentException.class,
                    () -> store.compareAndSet(connection, 42, identity(0),
                            new RoomRuntimeIdentity("other-instance", 7, 11, 1)));
            connection.rollback();
            assertEquals(identity(0), store.require(42).identity());
        }
    }

    @Test
    void concurrentTransactionsHaveExactlyOneCasWinner() throws Exception {
        RoomRuntimeIdentityStore store = new RoomRuntimeIdentityStore(dataSource);
        store.create(42, identity(0));
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                try (Connection connection = dataSource.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        store.compareAndSet(connection, 42, identity(0), identity(1));
                        connection.commit();
                        return true;
                    } catch (IllegalStateException conflict) {
                        connection.rollback();
                        assertEquals("ROOM_RUNTIME_IDENTITY_CAS_CONFLICT", conflict.getMessage());
                        return false;
                    }
                }
            };
            var outcomes = workers.invokeAll(java.util.List.of(attempt, attempt));
            int wins = 0;
            for (var result : outcomes) if (result.get()) wins++;
            assertEquals(1, wins);
            assertEquals(identity(1), store.require(42).identity());
        }
    }

    private static DataSource rollbackFailingDataSource(DataSource delegate) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return rollbackFailingConnection(delegate.getConnection());
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return rollbackFailingConnection(delegate.getConnection(username, password));
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return delegate.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return delegate.isWrapperFor(iface);
            }

            @Override
            public java.io.PrintWriter getLogWriter() throws SQLException {
                return delegate.getLogWriter();
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {
                delegate.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                delegate.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return delegate.getLoginTimeout();
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getLogger("test");
            }
        };
    }

    private static Connection rollbackFailingConnection(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
                RoomRuntimeIdentityStoreTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("rollback") && method.getParameterCount() == 0) {
                        throw new SQLException("forced rollback failure");
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private static RoomRuntimeIdentity identity(long stateVersion) {
        return new RoomRuntimeIdentity("instance-42", 7, 11, stateVersion);
    }
}
