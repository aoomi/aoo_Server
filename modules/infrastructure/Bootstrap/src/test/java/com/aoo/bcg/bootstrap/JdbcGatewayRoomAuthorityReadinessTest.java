package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gateway.GatewayRuntimeProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcGatewayRoomAuthorityReadinessTest {
    @Test
    void selectOneReadinessDoesNotMutateSentinelState() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:readiness-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE readiness_sentinel (id INT PRIMARY KEY, marker VARCHAR(16))");
            statement.execute("INSERT INTO readiness_sentinel VALUES (1, 'unchanged')");
        }
        int before;
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM readiness_sentinel")) {
            rows.next();
            before = rows.getInt(1);
        }
        GatewayRuntimeProvider.Readiness result = JdbcGatewayRoomAuthority.probeReadiness(source, true);
        assertTrue(result.ready());
        assertEquals("READY", result.code());
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM readiness_sentinel")) {
            rows.next();
            assertEquals(before, rows.getInt(1));
        }
    }

    @Test
    void probeFailsClosedOnDatasourceFailureAndDoesNotOpenWhenRuntimeIsNotConstructed() {
        ThrowingDataSource source = new ThrowingDataSource();
        GatewayRuntimeProvider.Readiness failed = JdbcGatewayRoomAuthority.probeReadiness(source, true);
        assertFalse(failed.ready());
        assertEquals("ROOM_AUTHORITY_NOT_READY", failed.code());
        GatewayRuntimeProvider.Readiness absent = JdbcGatewayRoomAuthority.probeReadiness(source, false);
        assertFalse(absent.ready());
        assertEquals(1, source.connectionAttempts);
    }

    private static final class ThrowingDataSource implements DataSource {
        private int connectionAttempts;
        @Override public Connection getConnection() throws SQLException {
            connectionAttempts++;
            throw new SQLException("synthetic unavailable");
        }
        @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
