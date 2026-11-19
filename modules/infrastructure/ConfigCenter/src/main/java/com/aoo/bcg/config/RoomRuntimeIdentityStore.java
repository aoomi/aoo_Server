package com.aoo.bcg.config;

import com.aoo.bcg.gamespi.RoomRuntimeIdentity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;
import java.util.Optional;

/**
 * The durable, dynamic authority for a room's runtime generation.
 *
 * <p>This store never derives an identity. A caller must provide the complete
 * v2.03 {@link RoomRuntimeIdentity} issued by an authority. The DESTROYED
 * tombstone is retained, so an old generation cannot be mounted again for the
 * same room id.</p>
 */
public final class RoomRuntimeIdentityStore {
    public enum LifecycleState { ACTIVE, DESTROYED }

    public record Entry(long roomId, RoomRuntimeIdentity identity, LifecycleState state) {
        public Entry {
            if (roomId <= 0 || identity == null || state == null) {
                throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_ENTRY_INVALID");
            }
        }

        public boolean active() {
            return state == LifecycleState.ACTIVE;
        }
    }

    private final DataSource dataSource;

    /**
     * Optional diagnostic context supplied by the authority caller.  It is
     * deliberately not part of the persisted identity and cannot influence
     * allocation, CAS, or tombstone decisions.
     */
    public record AuditContext(String attemptId, String retryCaller) {
        public AuditContext {
            if (attemptId == null || attemptId.isBlank()) {
                throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_AUDIT_ATTEMPT_REQUIRED");
            }
            if (retryCaller == null || retryCaller.isBlank()) {
                throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_AUDIT_CALLER_REQUIRED");
            }
        }
    }

    public RoomRuntimeIdentityStore(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /** Atomically creates the first explicit identity for a room. */
    public Entry create(long roomId, RoomRuntimeIdentity identity) {
        requireRoomId(roomId);
        Objects.requireNonNull(identity, "identity");
        String sql = "INSERT INTO aoo_room_runtime_identity_authority"
                + "(room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state)"
                + " VALUES(?,?,?,?,?,'ACTIVE')";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            statement.setLong(1, roomId);
            statement.setString(2, identity.roomInstanceId());
            statement.setLong(3, identity.lifecycleEpoch());
            statement.setLong(4, identity.fencingToken());
            statement.setLong(5, identity.stateVersion());
            statement.executeUpdate();
            connection.commit();
            return new Entry(roomId, identity, LifecycleState.ACTIVE);
        } catch (SQLIntegrityConstraintViolationException conflict) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT", conflict);
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CREATE_FAILED", failure);
        }
    }

    /**
     * Atomically obtains the single identity for a room during create/retry.
     *
     * <p>A failed create may leave an ACTIVE identity before the runtime handle
     * is durable.  Retrying must use that exact authority row instead of
     * issuing a second identity and attempting another primary-key insert.  A
     * DESTROYED row remains a terminal tombstone and is never remounted.  A
     * concurrent insert still fails closed; this method never overwrites or
     * silently replaces an identity.</p>
     */
    public Acquisition createOrReuse(long roomId, RoomRuntimeIdentity requested) {
        return createOrReuse(roomId, requested, null);
    }

    /**
     * Diagnostic-context overload for the authority path.  The context is
     * write-only observability data; all fail-closed allocation behavior is
     * shared with the two-argument API.
     */
    public Acquisition createOrReuse(long roomId, RoomRuntimeIdentity requested,
                                     AuditContext auditContext) {
        requireRoomId(roomId);
        Objects.requireNonNull(requested, "requested");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Entry current = selectForUpdate(connection, roomId);
                if (current != null) {
                    if (!current.active()) {
                        audit(auditContext, "create_or_reuse", roomId, current.identity(),
                                "REJECTED_DESTROYED", "ROOM_RUNTIME_IDENTITY_DESTROYED");
                        throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROYED");
                    }
                    connection.commit();
                    audit(auditContext, "create_or_reuse", roomId, current.identity(),
                            "ACTIVE_REUSE", "NONE");
                    return new Acquisition(current, false);
                }
                String sql = "INSERT INTO aoo_room_runtime_identity_authority"
                        + "(room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state)"
                        + " VALUES(?,?,?,?,?,'ACTIVE')";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, roomId);
                    statement.setString(2, requested.roomInstanceId());
                    statement.setLong(3, requested.lifecycleEpoch());
                    statement.setLong(4, requested.fencingToken());
                    statement.setLong(5, requested.stateVersion());
                    statement.executeUpdate();
                }
                connection.commit();
                audit(auditContext, "create_or_reuse", roomId, requested,
                        "INSERTED", "NONE");
                return new Acquisition(new Entry(roomId, requested, LifecycleState.ACTIVE), true);
            } catch (SQLIntegrityConstraintViolationException conflict) {
                rollback(connection);
                audit(auditContext, "create_or_reuse", roomId, requested,
                        "INSERT_CONFLICT", "ROOM_RUNTIME_IDENTITY_CONFLICT");
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT", conflict);
            } catch (RuntimeException failure) {
                rollback(connection);
                throw failure;
            } catch (SQLException failure) {
                rollback(connection);
                audit(auditContext, "create_or_reuse", roomId, requested,
                        "CREATE_FAILED", "ROOM_RUNTIME_IDENTITY_CREATE_FAILED");
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CREATE_FAILED", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CREATE_FAILED", failure);
        }
    }

    public record Acquisition(Entry entry, boolean created) {
        public Acquisition {
            Objects.requireNonNull(entry, "entry");
            if (!entry.active()) throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_ACQUISITION_INACTIVE");
        }
    }

    /** Reads the complete authority row; malformed or duplicated rows fail closed. */
    public Optional<Entry> find(long roomId) {
        requireRoomId(roomId);
        String sql = "SELECT room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state"
                + " FROM aoo_room_runtime_identity_authority WHERE room_id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                Entry entry = decode(rows);
                if (rows.next()) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DUPLICATE");
                return Optional.of(entry);
            }
        } catch (IllegalStateException | IllegalArgumentException failure) {
            throw failure;
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_READ_FAILED", failure);
        }
    }

    public Entry require(long roomId) {
        return find(roomId).orElseThrow(
                () -> new IllegalStateException("ROOM_RUNTIME_IDENTITY_NOT_FOUND"));
    }

    /**
     * Atomically advances stateVersion within the exact current generation.
     * Generation changes are intentionally a separate authority operation and
     * cannot be smuggled through a state update.
     */
    public Entry compareAndSet(long roomId, RoomRuntimeIdentity expected,
                               RoomRuntimeIdentity replacement) {
        requireIdentityTransition(roomId, expected, replacement);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Entry result = compareAndSet(connection, roomId, expected, replacement);
                connection.commit();
                return result;
            } catch (Exception failure) {
                connection.rollback();
                if (failure instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_FAILED", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_FAILED", failure);
        }
    }

    /** Mutates only inside the caller's transaction; its owner commits or rolls it back. */
    public Entry compareAndSet(Connection connection, long roomId, RoomRuntimeIdentity expected,
                               RoomRuntimeIdentity replacement) throws SQLException {
        requireIdentityTransition(roomId, expected, replacement);
        Objects.requireNonNull(connection, "connection");
        if (connection.getAutoCommit()) {
            throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_TRANSACTION_REQUIRED");
        }
        Entry current = selectForUpdate(connection, roomId);
        if (current == null) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_NOT_FOUND");
        if (!current.active()) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROYED");
        if (!current.identity().equals(expected)) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
        }
        String update = "UPDATE aoo_room_runtime_identity_authority"
                + " SET state_version=?,updated_at=CURRENT_TIMESTAMP(3)"
                + " WHERE room_id=? AND room_instance_id=? AND lifecycle_epoch=?"
                + " AND fencing_token=? AND state_version=? AND lifecycle_state='ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setLong(1, replacement.stateVersion());
            statement.setLong(2, roomId);
            statement.setString(3, expected.roomInstanceId());
            statement.setLong(4, expected.lifecycleEpoch());
            statement.setLong(5, expected.fencingToken());
            statement.setLong(6, expected.stateVersion());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
            }
        }
        return new Entry(roomId, replacement, LifecycleState.ACTIVE);
    }

    private static void requireIdentityTransition(long roomId, RoomRuntimeIdentity expected,
                                                   RoomRuntimeIdentity replacement) {
        requireRoomId(roomId);
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
        if (!expected.sameGeneration(replacement)) {
            throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_GENERATION_CHANGE_FORBIDDEN");
        }
        if (replacement.stateVersion() <= expected.stateVersion()) {
            throw new IllegalArgumentException("ROOM_RUNTIME_IDENTITY_STATE_VERSION_ROLLBACK");
        }
    }

    /** Atomically seals the current generation without deleting its tombstone. */
    public Entry destroy(long roomId, RoomRuntimeIdentity expected) {
        return destroy(roomId, expected, null);
    }

    /** Diagnostic-context overload; persistence semantics are unchanged. */
    public Entry destroy(long roomId, RoomRuntimeIdentity expected, AuditContext auditContext) {
        try(Connection connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try { Entry result=destroy(connection,roomId,expected,auditContext);connection.commit();return result; }
            catch(Exception failure){connection.rollback();if(failure instanceof RuntimeException runtime)throw runtime;throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROY_FAILED",failure);}
        }catch(SQLException failure){throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROY_FAILED",failure);}
    }

    /** Caller owns the transaction; identity and its business receipt can commit together. */
    public Entry destroy(Connection connection,long roomId,RoomRuntimeIdentity expected,AuditContext auditContext)throws SQLException {
        requireRoomId(roomId);Objects.requireNonNull(expected);
        if(connection.getAutoCommit())throw new IllegalArgumentException("caller transaction required");
                Entry current = selectForUpdate(connection, roomId);
                if (current == null) {
                    audit(auditContext, "destroy", roomId, null,
                            "TOMBSTONE_NOT_FOUND", "ROOM_RUNTIME_IDENTITY_NOT_FOUND");
                    throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_NOT_FOUND");
                }
                if (!current.active()) {
                    audit(auditContext, "destroy", roomId, current.identity(),
                            "TOMBSTONE_ALREADY_DESTROYED", "ROOM_RUNTIME_IDENTITY_DESTROYED");
                    throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DESTROYED");
                }
                if (!current.identity().equals(expected)) {
                    audit(auditContext, "destroy", roomId, current.identity(),
                            "TOMBSTONE_CAS_CONFLICT", "ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
                    throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
                }
                String update = "UPDATE aoo_room_runtime_identity_authority"
                        + " SET lifecycle_state='DESTROYED',updated_at=CURRENT_TIMESTAMP(3)"
                        + " WHERE room_id=? AND room_instance_id=? AND lifecycle_epoch=?"
                        + " AND fencing_token=? AND state_version=? AND lifecycle_state='ACTIVE'";
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setLong(1, roomId);
                    statement.setString(2, expected.roomInstanceId());
                    statement.setLong(3, expected.lifecycleEpoch());
                    statement.setLong(4, expected.fencingToken());
                    statement.setLong(5, expected.stateVersion());
                    if (statement.executeUpdate() != 1) {
                        audit(auditContext, "destroy", roomId, current.identity(),
                                "TOMBSTONE_CAS_CONFLICT", "ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
                        throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CAS_CONFLICT");
                    }
                }
        return new Entry(roomId,expected,LifecycleState.DESTROYED);
    }

    private static Entry selectForUpdate(Connection connection, long roomId) throws SQLException {
        String sql = "SELECT room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state"
                + " FROM aoo_room_runtime_identity_authority WHERE room_id=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                Entry entry = decode(rows);
                if (rows.next()) throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_DUPLICATE");
                return entry;
            }
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // exception-policy: rollback is best-effort cleanup; its failure must not mask or replace the original fail-closed identity error, create/reuse/remount an identity, or cause success.
        }
    }

    private static void audit(AuditContext context, String operation, long roomId,
                              RoomRuntimeIdentity identity, String sqlOutcome,
                              String errorCode) {
        if (context == null) return;
        try {
            System.err.println("room-runtime-identity-audit"
                    + " event=identity_authority"
                    + " operation=" + safe(operation)
                    + " attemptId=" + safe(context.attemptId())
                    + " roomId=" + roomId
                    + " roomInstanceId=" + identityValue(identity, "roomInstanceId")
                    + " lifecycleEpoch=" + identityValue(identity, "lifecycleEpoch")
                    + " fencingToken=" + identityValue(identity, "fencingToken")
                    + " stateVersion=" + identityValue(identity, "stateVersion")
                    + " sqlOutcome=" + safe(sqlOutcome)
                    + " retryCaller=" + safe(context.retryCaller())
                    + " errorCode=" + safe(errorCode));
        } catch (RuntimeException ignored) {
            // Diagnostics must never alter the fail-closed business result.
        }
    }

    private static String identityValue(RoomRuntimeIdentity identity, String field) {
        if (identity == null) return "<absent>";
        return switch (field) {
            case "roomInstanceId" -> safe(identity.roomInstanceId());
            case "lifecycleEpoch" -> Long.toString(identity.lifecycleEpoch());
            case "fencingToken" -> Long.toString(identity.fencingToken());
            case "stateVersion" -> Long.toString(identity.stateVersion());
            default -> "<absent>";
        };
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "<absent>";
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            result.append(character == ' ' || character == '\t'
                    || character == '\r' || character == '\n' ? '_' : character);
        }
        return result.toString();
    }

    private static Entry decode(ResultSet row) throws SQLException {
        String rawState = row.getString("lifecycle_state");
        final LifecycleState state;
        try {
            state = LifecycleState.valueOf(rawState);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_STATE_INVALID", failure);
        }
        try {
            return new Entry(row.getLong("room_id"), new RoomRuntimeIdentity(
                    row.getString("room_instance_id"), row.getLong("lifecycle_epoch"),
                    row.getLong("fencing_token"), row.getLong("state_version")), state);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_INVALID", failure);
        }
    }

    private static void requireRoomId(long roomId) {
        if (roomId <= 0) throw new IllegalArgumentException("ROOM_RUNTIME_ID_REQUIRED");
    }
}
