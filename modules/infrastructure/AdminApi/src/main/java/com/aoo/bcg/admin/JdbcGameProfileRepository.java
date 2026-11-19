package com.aoo.bcg.admin;

import com.aoo.bcg.config.GameProfilePublicationService;
import com.aoo.bcg.config.PublishedGameProfile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable game-profile versions with a transactionally replaceable active pointer. */
public final class JdbcGameProfileRepository implements GameProfilePublicationService.ProfileRepository {
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final ObjectMapper json;

    public JdbcGameProfileRepository(JdbcAdminResourceRepository.ConnectionFactory connections, ObjectMapper json) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public void insertImmutable(PublishedGameProfile profile, long operatorId, String reason) {
        String sql = "INSERT INTO game_profile_version(game_id,version,scope,profile_json,published_at,created_by,reason) VALUES(?,?,?,?,?,?,?)";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, profile.gameId()); statement.setString(2, profile.version());
            statement.setString(3, profile.scope().name()); statement.setString(4, json.writeValueAsString(profile));
            statement.setObject(5, profile.publishedAt()); statement.setLong(6, operatorId); statement.setString(7, reason);
            statement.executeUpdate();
        } catch (SQLException error) {
            if ("23000".equals(error.getSQLState())) throw new IllegalStateException("published profile version already exists", error);
            throw new IllegalStateException("cannot insert game profile", error);
        } catch (Exception error) {
            throw new IllegalStateException("cannot serialize game profile", error);
        }
    }

    @Override
    public void activate(long gameId, String version, long operatorId, String reason) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                if (!exists(connection, gameId, version)) throw new IllegalArgumentException("profile version not found");
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO game_profile_active(game_id,version,activated_by,reason,activated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) "
                                + "ON DUPLICATE KEY UPDATE version=VALUES(version),activated_by=VALUES(activated_by),reason=VALUES(reason),activated_at=VALUES(activated_at)")) {
                    statement.setLong(1, gameId); statement.setString(2, version);
                    statement.setLong(3, operatorId); statement.setString(4, reason); statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO game_profile_audit(audit_id,game_id,version,action,operator_id,reason,occurred_at) VALUES(?,?,?,'ACTIVATE_VERSION',?,?,CURRENT_TIMESTAMP(3))")) {
                    statement.setString(1,UUID.randomUUID().toString());statement.setLong(2, gameId); statement.setString(3, version);
                    statement.setLong(4, operatorId); statement.setString(5, reason); statement.executeUpdate();
                }
                insertProfileChangedOutbox(connection, gameId, version,
                        "activate:" + UUID.randomUUID());
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalStateException("cannot activate game profile", error);
        }
    }

    @Override
    public void publishAtomic(PublishedGameProfile profile, long operatorId, String reason, String requestId) {
        requireRequestId(requestId);
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                if (commandExists(connection, requestId, profile.gameId())) { connection.rollback(); return; }
                String payload = json.writeValueAsString(profile);
                insertCommand(connection, requestId, profile.gameId(), operatorId, payload);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO game_profile_version(game_id,version,scope,profile_json,published_at,created_by,reason) VALUES(?,?,?,?,?,?,?)")) {
                    statement.setLong(1, profile.gameId()); statement.setString(2, profile.version());
                    statement.setString(3, profile.scope().name()); statement.setString(4, payload);
                    statement.setObject(5, profile.publishedAt()); statement.setLong(6, operatorId);
                    statement.setString(7, reason); statement.executeUpdate();
                }
                insertAudit(connection, profile.gameId(), profile.version(), "INSERT_VERSION", operatorId, reason);
                activateInside(connection, profile.gameId(), profile.version(), operatorId, reason);
                insertProfileChangedOutbox(connection, profile.gameId(), profile.version(), requestId);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sql && "23000".equals(sql.getSQLState())
                        && commandExists(connection, requestId, profile.gameId())) return;
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            throw new IllegalStateException("cannot publish game profile atomically", error);
        }
    }

    @Override
    public void activateAtomic(long gameId, String version, long operatorId, String reason, String requestId) {
        requireRequestId(requestId);
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                if (commandExists(connection, requestId, gameId)) { connection.rollback(); return; }
                if (!exists(connection, gameId, version)) throw new IllegalArgumentException("profile version not found");
                String payload = json.writeValueAsString(find(gameId, version).orElseThrow());
                insertCommand(connection, requestId, gameId, operatorId, payload);
                activateInside(connection, gameId, version, operatorId, reason);
                insertProfileChangedOutbox(connection, gameId, version, requestId);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sql && "23000".equals(sql.getSQLState())
                        && commandExists(connection, requestId, gameId)) return;
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalStateException("cannot rollback game profile atomically", error);
        }
    }

    @Override
    public Optional<PublishedGameProfile> find(long gameId, String version) {
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT profile_json FROM game_profile_version WHERE game_id=? AND version=?")) {
            statement.setLong(1, gameId); statement.setString(2, version);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(json.readValue(row.getString(1), PublishedGameProfile.class)) : Optional.empty();
            }
        } catch (Exception error) {
            throw new IllegalStateException("cannot load game profile", error);
        }
    }

    @Override
    public List<PublishedGameProfile> active() {
        String sql = "SELECT v.profile_json FROM game_profile_active a JOIN game_profile_version v ON v.game_id=a.game_id AND v.version=a.version ORDER BY a.game_id";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<PublishedGameProfile> result = new ArrayList<>();
            while (rows.next()) result.add(json.readValue(rows.getString(1), PublishedGameProfile.class));
            return List.copyOf(result);
        } catch (Exception error) {
            throw new IllegalStateException("cannot load active profiles", error);
        }
    }

    private boolean exists(Connection connection, long gameId, String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM game_profile_version WHERE game_id=? AND version=?")) {
            statement.setLong(1, gameId); statement.setString(2, version);
            try (ResultSet row = statement.executeQuery()) { return row.next(); }
        }
    }

    private void activateInside(Connection connection, long gameId, String version,
            long operatorId, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO game_profile_active(game_id,version,activated_by,reason,activated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) "
                        + "ON DUPLICATE KEY UPDATE version=VALUES(version),activated_by=VALUES(activated_by),reason=VALUES(reason),activated_at=VALUES(activated_at)")) {
            statement.setLong(1, gameId); statement.setString(2, version);
            statement.setLong(3, operatorId); statement.setString(4, reason); statement.executeUpdate();
        }
        insertAudit(connection, gameId, version, "ACTIVATE_VERSION", operatorId, reason);
    }

    private boolean commandExists(Connection connection, String requestId, long gameId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT resource_type,resource_id FROM admin_command_dedup WHERE request_id=?")) {
            statement.setString(1, requestId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return false;
                if (!"game-profile".equals(row.getString(1))
                        || !Long.toString(gameId).equals(row.getString(2))) {
                    throw new IllegalArgumentException("requestId already used by another command");
                }
                return true;
            }
        }
    }

    private void insertAudit(Connection connection, long gameId, String version, String action,
            long operatorId, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO game_profile_audit(audit_id,game_id,version,action,operator_id,reason,occurred_at) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP(3))")) {
            statement.setString(1,UUID.randomUUID().toString());statement.setLong(2, gameId); statement.setString(3, version); statement.setString(4, action);
            statement.setLong(5, operatorId); statement.setString(6, reason); statement.executeUpdate();
        }
    }

    private void insertCommand(Connection connection, String requestId, long gameId,
            long operatorId, String payload) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO admin_command_dedup(request_id,resource_type,resource_id,operator_id,response_json,created_at) VALUES(?,'game-profile',?,?,?,CURRENT_TIMESTAMP(3))")) {
            statement.setString(1, requestId); statement.setString(2, Long.toString(gameId));
            statement.setLong(3, operatorId); statement.setString(4, payload); statement.executeUpdate();
        }
    }

    private void insertProfileChangedOutbox(Connection connection, long gameId, String version,
            String requestId) throws Exception {
        String eventId = sha256("game-profile:" + gameId + ":" + version + ":" + requestId);
        String payload = json.writeValueAsString(Map.of(
                "gameId", gameId,
                "version", version,
                "action", "GAME_PROFILE_ACTIVATED"));
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO aoo_outbox(event_id,aggregate_type,aggregate_id,event_type,payload,created_at) "
                        + "VALUES(?,'game-profile',?,'GAME_PROFILE_ACTIVATED',?,CURRENT_TIMESTAMP(3)) "
                        + "ON DUPLICATE KEY UPDATE event_id=event_id")) {
            statement.setString(1, eventId);
            statement.setLong(2, gameId);
            statement.setString(3, payload);
            statement.executeUpdate();
        }
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder(64);
        for (byte item : digest) text.append(String.format("%02x", item));
        return text.toString();
    }

    private void requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
            throw new IllegalArgumentException("requestId required");
        }
    }
}
