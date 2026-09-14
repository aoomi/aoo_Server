package com.aoo.bcg.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** MySQL implementation with durable command idempotency. */
public final class JdbcAdminResourceRepository implements AdminResourceRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ConnectionFactory connections;
    private final ObjectMapper json;
    private final Clock clock;

    public JdbcAdminResourceRepository(ConnectionFactory connections, ObjectMapper json, Clock clock) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.json = Objects.requireNonNull(json, "json");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<Map<String, Object>> list(String resourceType) {
        validateKey(resourceType);
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? ORDER BY resource_id";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resourceType);
            List<Map<String, Object>> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(AdminResourceStore.withEtag(json.readValue(rows.getString(1), MAP_TYPE)));
            }
            return List.copyOf(result);
        } catch (Exception error) {
            throw new IllegalStateException("cannot load admin resources", error);
        }
    }

    @Override
    public Map<String, Object> execute(String resourceType, String id, long operatorId,
            Map<String, Object> command, String forcedStatus, String ifMatch) {
        validateKey(resourceType);
        validateKey(id);
        String requestId = required(command, "requestId");
        String reason = required(command, "reason");
        if (operatorId <= 0) throw new IllegalArgumentException("operator required");
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                Map<String, Object> previous = findCommand(connection, requestId, resourceType, id);
                if (previous != null) {
                    connection.rollback();
                    return AdminResourceStore.withEtag(previous);
                }
                Map<String, Object> current = lockResource(connection, resourceType, id);
                AdminResourceStore.requireMatch(ifMatch, current);
                Map<String, Object> value = new LinkedHashMap<>(command);
                value.remove("requestId");
                value.put("id", id);
                value.put("reason", reason);
                value.put("updatedBy", operatorId);
                value.put("updatedAt", clock.instant().toString());
                if (forcedStatus != null) value.put("status", forcedStatus);
                String payload = json.writeValueAsString(value);
                insertCommand(connection, requestId, resourceType, id, operatorId, payload);
                upsertResource(connection, resourceType, id, operatorId, reason, payload);
                connection.commit();
                return AdminResourceStore.withEtag(Map.copyOf(value));
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
                    Map<String, Object> previous = findCommand(connection, requestId, resourceType, id);
                    if (previous != null) return AdminResourceStore.withEtag(previous);
                }
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            throw new IllegalStateException("cannot persist admin command", error);
        }
    }

    private Map<String, Object> findCommand(Connection connection, String requestId,
            String resourceType, String resourceId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT resource_type,resource_id,response_json FROM admin_command_dedup WHERE request_id=?")) {
            statement.setString(1, requestId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                if (!resourceType.equals(row.getString(1)) || !resourceId.equals(row.getString(2))) {
                    throw new IllegalArgumentException("requestId already used by another command");
                }
                return json.readValue(row.getString(3), MAP_TYPE);
            }
        }
    }

    private void insertCommand(Connection connection, String requestId, String type, String id,
            long operatorId, String payload) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO admin_command_dedup(request_id,resource_type,resource_id,operator_id,response_json,created_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP(3))")) {
            statement.setString(1, requestId); statement.setString(2, type); statement.setString(3, id);
            statement.setLong(4, operatorId); statement.setString(5, payload); statement.executeUpdate();
        }
    }

    private Map<String, Object> lockResource(Connection connection, String type, String id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payload_json FROM admin_control_resource WHERE resource_type=? AND resource_id=? FOR UPDATE")) {
            statement.setString(1, type); statement.setString(2, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? json.readValue(row.getString(1), MAP_TYPE) : null;
            }
        }
    }

    private void upsertResource(Connection connection, String type, String id, long operatorId,
            String reason, String payload) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO admin_control_resource(resource_type,resource_id,payload_json,updated_by,reason,updated_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP(3)) "
                        + "ON DUPLICATE KEY UPDATE payload_json=VALUES(payload_json),updated_by=VALUES(updated_by),reason=VALUES(reason),updated_at=VALUES(updated_at)")) {
            statement.setString(1, type); statement.setString(2, id); statement.setString(3, payload);
            statement.setLong(4, operatorId); statement.setString(5, reason); statement.executeUpdate();
        }
    }

    private String required(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(field + " required");
        return text;
    }

    private void validateKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128 || !value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid resource key");
        }
    }

    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
}
