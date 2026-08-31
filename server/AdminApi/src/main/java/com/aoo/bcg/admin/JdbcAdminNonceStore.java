package com.aoo.bcg.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

/** Atomic, cross-instance nonce consumption backed by the admin database. */
public final class JdbcAdminNonceStore implements AdminNonceStore {
    private final JdbcAdminResourceRepository.ConnectionFactory connections;

    public JdbcAdminNonceStore(JdbcAdminResourceRepository.ConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections);
    }

    @Override public boolean consume(String namespace, String nonce, Instant expiresAt) {
        if (namespace == null || !namespace.matches("[a-z-]{1,32}") || nonce == null
                || !nonce.matches("[A-Za-z0-9_-]{16,128}") || expiresAt == null)
            throw new IllegalArgumentException("invalid nonce consumption");
        try (Connection connection = connections.open()) {
            try (PreparedStatement purge = connection.prepareStatement(
                    "DELETE FROM admin_security_nonce WHERE expires_at < CURRENT_TIMESTAMP(3) LIMIT 1000")) {
                purge.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO admin_security_nonce(nonce_namespace,nonce_value,expires_at,consumed_at) "
                            + "VALUES(?,?,?,CURRENT_TIMESTAMP(3))")) {
                statement.setString(1, namespace); statement.setString(2, nonce);
                statement.setObject(3, expiresAt); statement.executeUpdate(); return true;
            }
        } catch (SQLException error) {
            if ("23000".equals(error.getSQLState())) return false;
            throw new IllegalStateException("cannot atomically consume admin nonce", error);
        }
    }
}
