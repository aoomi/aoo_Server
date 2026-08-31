package com.aoo.bcg.common.persistence;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Minimal non-pooling adapter for control-plane processes; game nodes use Druid. */
public final class DriverManagerDataSource implements DataSource {
    private final String url;
    private final String user;
    private final String password;

    public DriverManagerDataSource(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.user = Objects.requireNonNull(user, "user");
        this.password = Objects.requireNonNull(password, "password");
    }

    @Override public Connection getConnection() throws SQLException { return utc(DriverManager.getConnection(url, user, password)); }
    @Override public Connection getConnection(String username, String password) throws SQLException {
        return utc(DriverManager.getConnection(url, username, password));
    }
    @Override public PrintWriter getLogWriter() { return DriverManager.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) { DriverManager.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) { DriverManager.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() { return DriverManager.getLoginTimeout(); }
    @Override public Logger getParentLogger() { return Logger.getLogger("java.sql"); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
    private static Connection utc(Connection connection)throws SQLException {
        try(var statement=connection.createStatement()){statement.execute("SET time_zone='+00:00'");return connection;}
        catch(SQLException error){try{connection.close();}catch(SQLException suppressed){error.addSuppressed(suppressed);}throw error;}
    }
}
