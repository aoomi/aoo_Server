package com.aoo.bcg.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public final class JdbcAdminAuthorizationService implements AdminAuthorizationService {
    private final JdbcAdminResourceRepository.ConnectionFactory connections;

    public JdbcAdminAuthorizationService(JdbcAdminResourceRepository.ConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public boolean allowed(long operatorId, String permission) {
        if (operatorId <= 0 || permission == null || permission.isBlank()) return false;
        String sql = "SELECT 1 FROM admin_operator_permission WHERE operator_id=? AND permission_code=? AND enabled=1 UNION ALL SELECT 1 FROM admin_operator_role assigned JOIN admin_role role ON role.role_code=assigned.role_code AND role.enabled=1 JOIN admin_role_permission permission ON permission.role_code=assigned.role_code WHERE assigned.operator_id=? AND assigned.enabled=1 AND permission.permission_code=? LIMIT 1";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, operatorId);
            statement.setString(2, permission);
            statement.setLong(3,operatorId);statement.setString(4,permission);
            try (ResultSet row = statement.executeQuery()) { return row.next(); }
        } catch (Exception error) {
            throw new IllegalStateException("cannot verify admin permission", error);
        }
    }
}
