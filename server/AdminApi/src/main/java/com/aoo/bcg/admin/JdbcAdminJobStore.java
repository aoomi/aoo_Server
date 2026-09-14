package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Transactional multi-node job state using row locks and monotonic fencing tokens. */
public final class JdbcAdminJobStore implements AdminJobCoordinator.Store {
    private static final String TYPE = "admin-job-run";
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final ObjectMapper json;

    public JdbcAdminJobStore(JdbcAdminResourceRepository.ConnectionFactory connections, ObjectMapper json) {
        this.connections = Objects.requireNonNull(connections);
        this.json = Objects.requireNonNull(json);
    }

    @Override public void create(AdminJobCoordinator.Run run) {
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO admin_control_resource(resource_type,resource_id,payload_json,updated_by,reason,updated_at) "
                        + "VALUES(?,?,?,?,?,?)")) {
            statement.setString(1, TYPE); statement.setString(2, run.runId());
            statement.setString(3, json.writeValueAsString(run)); statement.setLong(4, run.requestedBy());
            statement.setString(5, run.reason()); statement.setObject(6, run.updatedAt()); statement.executeUpdate();
        } catch (Exception error) { throw new IllegalStateException("cannot create admin job run", error); }
    }

    @Override public Optional<AdminJobCoordinator.Run> find(String runId) {
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? AND resource_id=?";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE); statement.setString(2, runId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(decode(row.getString(1))) : Optional.empty();
            }
        } catch (Exception error) { throw new IllegalStateException("cannot read admin job run", error); }
    }

    @Override public Optional<AdminJobCoordinator.Run> findSucceeded(AdminJobCoordinator.JobKey key) {
        return all().stream().filter(run -> run.key().equals(key)
                && run.state() == AdminJobCoordinator.State.SUCCEEDED).findFirst();
    }

    @Override public Optional<AdminJobCoordinator.Lease> acquire(String runId, String owner,
            Instant now, Instant leaseUntil) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                AdminJobCoordinator.Run current = locked(connection, runId);
                if (current == null || current.state() == AdminJobCoordinator.State.RUNNING
                        && current.leaseUntil().isAfter(now) && !current.leaseOwner().equals(owner)) {
                    connection.rollback(); return Optional.empty();
                }
                long token = Math.addExact(current.fencingToken(), 1);
                AdminJobCoordinator.Run leased = new AdminJobCoordinator.Run(current.runId(), current.key(),
                        current.state(), current.attempt(), current.maxAttempts(), owner, token, leaseUntil,
                        current.nextAttemptAt(), current.result(), current.error(), current.rerunOf(),
                        current.requestedBy(), current.approvedBy(), current.reason(), now);
                update(connection, leased); connection.commit();
                return Optional.of(new AdminJobCoordinator.Lease(runId, owner, token, leaseUntil));
            } catch (Exception error) { connection.rollback(); throw error; }
            finally { connection.setAutoCommit(true); }
        } catch (Exception error) { throw new IllegalStateException("cannot acquire admin job lease", error); }
    }

    @Override public boolean saveFenced(AdminJobCoordinator.Run run, String owner, long token) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                AdminJobCoordinator.Run current = locked(connection, run.runId());
                if (current == null || current.fencingToken() != token || !current.leaseOwner().equals(owner)) {
                    connection.rollback(); return false;
                }
                update(connection, run); connection.commit(); return true;
            } catch (Exception error) { connection.rollback(); throw error; }
            finally { connection.setAutoCommit(true); }
        } catch (Exception error) { throw new IllegalStateException("cannot save fenced admin job run", error); }
    }

    @Override public List<AdminJobCoordinator.Run> all() {
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? ORDER BY updated_at,resource_id";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE); List<AdminJobCoordinator.Run> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(decode(rows.getString(1))); }
            return List.copyOf(result);
        } catch (Exception error) { throw new IllegalStateException("cannot list admin job runs", error); }
    }

    private AdminJobCoordinator.Run locked(Connection connection, String runId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payload_json FROM admin_control_resource WHERE resource_type=? AND resource_id=? FOR UPDATE")) {
            statement.setString(1, TYPE); statement.setString(2, runId);
            try (ResultSet row = statement.executeQuery()) { return row.next() ? decode(row.getString(1)) : null; }
        }
    }
    private void update(Connection connection, AdminJobCoordinator.Run run) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE admin_control_resource SET payload_json=?,updated_by=?,reason=?,updated_at=? "
                        + "WHERE resource_type=? AND resource_id=?")) {
            statement.setString(1, json.writeValueAsString(run)); statement.setLong(2, run.requestedBy());
            statement.setString(3, run.reason()); statement.setObject(4, run.updatedAt());
            statement.setString(5, TYPE); statement.setString(6, run.runId());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("admin job run disappeared");
        }
    }
    private AdminJobCoordinator.Run decode(String value) throws Exception {
        return json.readValue(value, AdminJobCoordinator.Run.class);
    }
}
