package com.aoo.bcg.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Persists the hash chain as insert-only rows in the existing admin control-plane table. */
public final class JdbcAdminChangeJournalStore implements AdminChangeJournal.Store {
    private static final String TYPE = "admin-change-journal";
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final ObjectMapper json;

    public JdbcAdminChangeJournalStore(JdbcAdminResourceRepository.ConnectionFactory connections, ObjectMapper json) {
        this.connections = Objects.requireNonNull(connections);
        this.json = Objects.requireNonNull(json);
    }

    @Override public void append(AdminChangeJournal.Change change) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                String latestHash = latestHashForUpdate(connection);
                if (!latestHash.equals(change.previousHash()))
                    throw new IllegalStateException("admin audit hash chain changed concurrently");
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO admin_control_resource(resource_type,resource_id,payload_json,updated_by,reason,updated_at) "
                                + "VALUES(?,?,?,?,?,?)")) {
                    statement.setString(1, TYPE); statement.setString(2, change.changeId());
                    statement.setString(3, json.writeValueAsString(change)); statement.setLong(4, change.operatorId());
                    statement.setString(5, change.reason()); statement.setObject(6, change.occurredAt());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback(); throw error;
            } finally { connection.setAutoCommit(true); }
        } catch (Exception error) {
            throw new IllegalStateException("cannot append immutable admin change", error);
        }
    }

    @Override public List<AdminChangeJournal.Change> all() {
        String sql = "SELECT payload_json FROM admin_control_resource WHERE resource_type=? ORDER BY updated_at,resource_id";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE);
            List<AdminChangeJournal.Change> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(json.readValue(rows.getString(1), AdminChangeJournal.Change.class));
            }
            return List.copyOf(result);
        } catch (Exception error) { throw new IllegalStateException("cannot read admin change journal", error); }
    }

    private String latestHashForUpdate(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payload_json FROM admin_control_resource WHERE resource_type=? "
                        + "ORDER BY updated_at DESC,resource_id DESC LIMIT 1 FOR UPDATE")) {
            statement.setString(1, TYPE);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? json.readValue(row.getString(1), AdminChangeJournal.Change.class).hash() : "GENESIS";
            }
        }
    }
}
