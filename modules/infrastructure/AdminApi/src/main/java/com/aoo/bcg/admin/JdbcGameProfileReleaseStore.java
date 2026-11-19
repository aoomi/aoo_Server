package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Durable and idempotent workflow state in the existing admin control-plane storage boundary. */
public final class JdbcGameProfileReleaseStore implements GameProfileReleaseWorkflow.Store {
    private static final String RELEASE = "game-profile-release";
    private static final String REQUEST = "game-profile-release-request";
    private static final String EVENT = "game-profile-release-event";
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final ObjectMapper json;

    public JdbcGameProfileReleaseStore(JdbcAdminResourceRepository.ConnectionFactory connections, ObjectMapper json) {
        this.connections = Objects.requireNonNull(connections);
        this.json = Objects.requireNonNull(json);
    }

    @Override public Optional<GameProfileReleaseWorkflow.Release> findRelease(String releaseId) {
        return find(RELEASE, releaseId, GameProfileReleaseWorkflow.Release.class);
    }

    @Override public Optional<GameProfileReleaseWorkflow.Release> findRequest(String requestId) {
        return find(REQUEST, requestId, GameProfileReleaseWorkflow.Release.class);
    }

    @Override public void save(String requestId, GameProfileReleaseWorkflow.Release before,
            GameProfileReleaseWorkflow.Release after, GameProfileReleaseWorkflow.Event event) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                GameProfileReleaseWorkflow.Release repeated = locked(connection, REQUEST, requestId,
                        GameProfileReleaseWorkflow.Release.class);
                if (repeated != null) {
                    if (!repeated.releaseId().equals(after.releaseId()) || repeated.gameId() != after.gameId()
                            || !repeated.version().equals(after.version()) || repeated.state() != after.state())
                        throw new IllegalArgumentException("requestId reused with another transition");
                    connection.rollback(); return;
                }
                GameProfileReleaseWorkflow.Release current = locked(connection, RELEASE, after.releaseId(),
                        GameProfileReleaseWorkflow.Release.class);
                if (!Objects.equals(current, before)) throw new IllegalStateException("release state changed concurrently");
                insert(connection, REQUEST, requestId, after, event.operatorId(), event.reason(), event.occurredAt());
                if (current == null) insert(connection, RELEASE, after.releaseId(), after,
                        event.operatorId(), event.reason(), event.occurredAt());
                else update(connection, RELEASE, after.releaseId(), after,
                        event.operatorId(), event.reason(), event.occurredAt());
                insert(connection, EVENT, requestId, event, event.operatorId(), event.reason(), event.occurredAt());
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
            finally { connection.setAutoCommit(true); }
        } catch (Exception error) { throw new IllegalStateException("cannot persist profile release workflow", error); }
    }

    @Override public List<GameProfileReleaseWorkflow.Event> history() {
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? ORDER BY updated_at,resource_id";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, EVENT); List<GameProfileReleaseWorkflow.Event> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(json.readValue(rows.getString(1), GameProfileReleaseWorkflow.Event.class));
            }
            return List.copyOf(result);
        } catch (Exception error) { throw new IllegalStateException("cannot read profile release history", error); }
    }

    private <T> Optional<T> find(String type, String id, Class<T> valueType) {
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? AND resource_id=?";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type); statement.setString(2, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(json.readValue(row.getString(1), valueType)) : Optional.empty();
            }
        } catch (Exception error) { throw new IllegalStateException("cannot read profile release workflow", error); }
    }

    private <T> T locked(Connection connection, String type, String id, Class<T> valueType) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payload_json FROM admin_control_resource WHERE resource_type=? AND resource_id=? FOR UPDATE")) {
            statement.setString(1, type); statement.setString(2, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? json.readValue(row.getString(1), valueType) : null;
            }
        }
    }
    private void insert(Connection connection, String type, String id, Object value, long operatorId,
            String reason, java.time.Instant at) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO admin_control_resource(resource_type,resource_id,payload_json,updated_by,reason,updated_at) "
                        + "VALUES(?,?,?,?,?,?)")) {
            bind(statement, type, id, value, operatorId, reason, at); statement.executeUpdate();
        }
    }
    private void update(Connection connection, String type, String id, Object value, long operatorId,
            String reason, java.time.Instant at) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE admin_control_resource SET payload_json=?,updated_by=?,reason=?,updated_at=? "
                        + "WHERE resource_type=? AND resource_id=?")) {
            statement.setString(1, json.writeValueAsString(value)); statement.setLong(2, operatorId);
            statement.setString(3, reason); statement.setObject(4, at); statement.setString(5, type);
            statement.setString(6, id); if (statement.executeUpdate() != 1) throw new IllegalStateException("release disappeared");
        }
    }
    private void bind(PreparedStatement statement, String type, String id, Object value, long operatorId,
            String reason, java.time.Instant at) throws Exception {
        statement.setString(1, type); statement.setString(2, id);
        statement.setString(3, json.writeValueAsString(value)); statement.setLong(4, operatorId);
        statement.setString(5, reason); statement.setObject(6, at);
    }
}
