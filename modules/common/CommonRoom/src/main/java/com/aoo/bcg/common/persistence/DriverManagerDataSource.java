package com.aoo.bcg.common.persistence;

import com.alibaba.druid.pool.DruidDataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Bounded JDBC pool shared by the independently deployed Aoo services.
 *
 * The historic class name is retained as a source-compatible boundary. Creating a
 * physical MySQL connection for every repository method made one room command pay
 * several TCP/TLS handshakes and exhausted CPU under normal table traffic.
 */
public final class DriverManagerDataSource implements DataSource, AutoCloseable {
    private static final String MYSQL_CONNECT_TIMEOUT = "connectTimeout=5000";
    private static final String MYSQL_SOCKET_TIMEOUT = "socketTimeout=15000";
    private final String url;
    private final String user;
    private final String password;
    private final DruidDataSource delegate;

    public DriverManagerDataSource(String url, String user, String password) {
        this.url = boundedUrl(Objects.requireNonNull(url, "url"));
        this.user = Objects.requireNonNull(user, "user");
        this.password = Objects.requireNonNull(password, "password");
        this.delegate = new DruidDataSource();
        delegate.setUrl(this.url);
        delegate.setUsername(this.user);
        delegate.setPassword(this.password);
        delegate.setInitialSize(0);
        delegate.setMinIdle(0);
        delegate.setMaxActive(16);
        // A cold Docker Desktop wake-up can take several seconds even though
        // steady-state borrows are sub-millisecond. Keep the pool bounded by the
        // JDBC socket timeout without turning a transient cold start into a dead service.
        delegate.setMaxWait(15_000);
        delegate.setTestWhileIdle(true);
        delegate.setValidationQuery("SELECT 1");
        delegate.setTimeBetweenEvictionRunsMillis(30_000);
        delegate.setMinEvictableIdleTimeMillis(60_000);
        delegate.setConnectionInitSqls(List.of("SET time_zone='+00:00'"));
        System.out.println("[AooJdbcPool] initialized maxActive=16 maxWaitMs=15000");
    }

    @Override public Connection getConnection() throws SQLException { return delegate.getConnection(); }
    @Override public Connection getConnection(String username, String password) throws SQLException {
        if (!user.equals(username) || !this.password.equals(password)) {
            throw new SQLException("per-call database credentials are not supported by the managed pool");
        }
        return delegate.getConnection();
    }
    @Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
    @Override public Logger getParentLogger() { return Logger.getLogger("java.sql"); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        if (iface.isInstance(delegate)) return iface.cast(delegate);
        throw new SQLException("not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this) || iface.isInstance(delegate);
    }
    @Override public void close() { delegate.close(); }
    static String boundedUrl(String value) {
        if (!value.startsWith("jdbc:mysql:")) return value;
        String result = value;
        if (!hasParameter(result, "connectTimeout")) result = append(result, MYSQL_CONNECT_TIMEOUT);
        if (!hasParameter(result, "socketTimeout")) result = append(result, MYSQL_SOCKET_TIMEOUT);
        return result;
    }
    private static boolean hasParameter(String value, String name) {
        return value.matches("(?i).*([?&])" + java.util.regex.Pattern.quote(name) + "=[^&]*.*");
    }
    private static String append(String value, String parameter) {
        return value + (value.contains("?") ? "&" : "?") + parameter;
    }
}
