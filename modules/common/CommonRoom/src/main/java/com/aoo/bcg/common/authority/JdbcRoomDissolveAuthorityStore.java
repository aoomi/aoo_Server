package com.aoo.bcg.common.authority;

import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * JDBC authority for the durable dissolve aggregate.
 *
 * <p>Every mutation locks the runtime identity and room-lock projection in
 * one JDBC transaction, validates the complete canonical metadata, advances
 * the runtime state version with CAS, and writes the aggregate and command
 * receipt before commit.  No PDK or process-local vote state is consulted.</p>
 */
public final class JdbcRoomDissolveAuthorityStore implements RoomDissolveAuthorityStore {
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() { };
    private static final TypeReference<Map<String, Boolean>> VOTE_MAP = new TypeReference<>() { };

    private final DataSource dataSource;
    private final ObjectMapper json;
    private final Clock clock;

    public JdbcRoomDissolveAuthorityStore(DataSource dataSource, ObjectMapper json, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.json = Objects.requireNonNull(json, "json");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public RoomOrchestrator.DissolveResult apply(RoomOrchestrator.DissolveApplyCommand command) {
        Objects.requireNonNull(command, "command");
        return inTransaction(connection -> {
            RuntimeRow runtime = lockRuntime(connection, command.expectedIdentity());
            requireCanonicalMetadata(connection, command.expectedIdentity(), command.metadata());
            String requestHash = requestHash(command);
            ReceiptRow receipt = findReceipt(connection, command.expectedIdentity(), command.commandId());
            if (receipt != null) {
                requireReceiptMatches(receipt, requestHash, command.idempotencyKey(), "APPLY");
                return receipt.result(command.expectedIdentity(), runtime.identity());
            }
            requireExpectedState(runtime, command.expectedStateVersion());
            requireActiveRoom(command.roomState());
            if (command.permission() != RoomOrchestrator.DissolvePermission.APPLICANT) {
                throw new SecurityException("ROOM_DISSOLVE_APPLY_PERMISSION_DENIED");
            }
            if (!clock.instant().isBefore(command.deadline())) {
                throw new IllegalStateException("ROOM_DISSOLVE_APPLY_EXPIRED");
            }
            if (!command.applicantAgrees()) {
                throw new IllegalStateException("ROOM_DISSOLVE_APPLICANT_MUST_AGREE");
            }
            AggregateRow existing = findAggregate(connection, command.expectedIdentity(), command.voteId(), true);
            if (existing != null) {
                throw new IllegalStateException("ROOM_DISSOLVE_VOTE_ID_CONFLICT");
            }
            long nextStateVersion = nextStateVersion(runtime.stateVersion());
            Map<Long, Boolean> votes = Map.of(command.applicantAccountId(), true);
            RoomOrchestrator.DissolveDecision decision = command.eligibleMemberIds().size() == 1
                    ? RoomOrchestrator.DissolveDecision.APPROVED
                    : RoomOrchestrator.DissolveDecision.VOTING;
            boolean terminal = decision != RoomOrchestrator.DissolveDecision.VOTING;
            insertAggregate(connection, command, runtime.identity(), nextStateVersion, decision, terminal, votes);
            updateRuntime(connection, command.expectedIdentity(), runtime, nextStateVersion);
            RoomOrchestrator.RoomIdentity resultIdentity = identity(command.expectedIdentity(), runtime.identity(),
                    nextStateVersion);
            RoomOrchestrator.DissolveResult result = result(resultIdentity, command.voteId(),
                    command.applicantAccountId(), command.eligibleMemberIds(), command.deadline(),
                    command.approveOnTimeout(), votes, decision, terminal,
                    RoomOrchestrator.DissolveStatus.APPLIED, command.commandId(), command.idempotencyKey());
            insertReceipt(connection, command, requestHash, "APPLY", result);
            return result;
        });
    }

    @Override
    public RoomOrchestrator.DissolveResult vote(RoomOrchestrator.DissolveVoteCommand command) {
        Objects.requireNonNull(command, "command");
        return inTransaction(connection -> {
            RuntimeRow runtime = lockRuntime(connection, command.expectedIdentity());
            requireCanonicalMetadata(connection, command.expectedIdentity(), command.metadata());
            String requestHash = requestHash(command);
            ReceiptRow receipt = findReceipt(connection, command.expectedIdentity(), command.commandId());
            if (receipt != null) {
                requireReceiptMatches(receipt, requestHash, command.idempotencyKey(), "VOTE");
                return receipt.result(command.expectedIdentity(), runtime.identity());
            }
            requireExpectedState(runtime, command.expectedStateVersion());
            requireActiveRoom(command.roomState());
            if (command.permission() != RoomOrchestrator.DissolvePermission.ELIGIBLE_MEMBER) {
                throw new SecurityException("ROOM_DISSOLVE_VOTE_PERMISSION_DENIED");
            }
            AggregateRow aggregate = findAggregate(connection, command.expectedIdentity(), command.voteId(), true);
            if (aggregate == null) {
                throw new IllegalStateException("ROOM_DISSOLVE_NOT_FOUND");
            }
            if (aggregate.stateVersion() != runtime.stateVersion()) {
                throw new IllegalStateException("ROOM_DISSOLVE_AGGREGATE_STATE_VERSION_CONFLICT");
            }
            if (aggregate.terminalTombstone()) {
                throw new IllegalStateException("ROOM_DISSOLVE_ALREADY_TERMINAL");
            }
            if (!clock.instant().isBefore(aggregate.deadline())) {
                throw new IllegalStateException("ROOM_DISSOLVE_VOTE_EXPIRED");
            }
            if (!aggregate.eligibleMemberIds().contains(command.voterAccountId())) {
                throw new SecurityException("ROOM_DISSOLVE_VOTER_NOT_ELIGIBLE");
            }
            if (aggregate.votes().containsKey(command.voterAccountId())) {
                throw new IllegalStateException("ROOM_DISSOLVE_VOTE_ALREADY_CAST");
            }
            Map<Long, Boolean> votes = new TreeMap<>(aggregate.votes());
            votes.put(command.voterAccountId(), command.agree());
            RoomOrchestrator.DissolveDecision decision = decide(aggregate, votes);
            boolean terminal = decision != RoomOrchestrator.DissolveDecision.VOTING;
            long nextStateVersion = nextStateVersion(runtime.stateVersion());
            updateAggregate(connection, command, aggregate, runtime.stateVersion(), nextStateVersion,
                    decision, terminal, votes);
            updateRuntime(connection, command.expectedIdentity(), runtime, nextStateVersion);
            RoomOrchestrator.RoomIdentity resultIdentity = identity(command.expectedIdentity(), runtime.identity(),
                    nextStateVersion);
            RoomOrchestrator.DissolveResult result = result(resultIdentity, command.voteId(),
                    aggregate.applicantAccountId(), aggregate.eligibleMemberIds(), aggregate.deadline(),
                    aggregate.approveOnTimeout(), votes, decision, terminal,
                    RoomOrchestrator.DissolveStatus.VOTED, command.commandId(), command.idempotencyKey());
            insertReceipt(connection, command, requestHash, "VOTE", result);
            return result;
        });
    }

    @Override
    public RoomOrchestrator.DissolveResult recover(RoomOrchestrator.DissolveRecoverCommand command) {
        Objects.requireNonNull(command, "command");
        return inTransaction(connection -> {
            RuntimeRow runtime = lockRuntime(connection, command.expectedIdentity());
            requireCanonicalMetadata(connection, command.expectedIdentity(), command.metadata());
            if (runtime.stateVersion() < command.expectedIdentity().stateVersion()) {
                throw new IllegalStateException("ROOM_DISSOLVE_RECOVERY_STATE_VERSION_ROLLBACK");
            }
            AggregateRow aggregate = findAggregate(connection, command.expectedIdentity(), command.voteId(), true);
            if (aggregate == null) throw new IllegalStateException("ROOM_DISSOLVE_NOT_FOUND");
            if (aggregate.stateVersion() > runtime.stateVersion()) {
                throw new IllegalStateException("ROOM_DISSOLVE_RECOVERY_STATE_VERSION_CONFLICT");
            }
            RoomOrchestrator.RoomIdentity resultIdentity = identity(command.expectedIdentity(), runtime.identity(),
                    aggregate.stateVersion());
            return result(resultIdentity, aggregate.voteId(), aggregate.applicantAccountId(),
                    aggregate.eligibleMemberIds(), aggregate.deadline(), aggregate.approveOnTimeout(),
                    aggregate.votes(), aggregate.decision(), aggregate.terminalTombstone(),
                    RoomOrchestrator.DissolveStatus.RECOVERED, null, null);
        });
    }

    private <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                rollback(connection, failure);
                throw failure;
            } catch (Exception failure) {
                rollback(connection, failure);
                throw new IllegalStateException("ROOM_DISSOLVE_TRANSACTION_FAILED", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_DATABASE_FAILED", failure);
        }
    }

    private static void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private RuntimeRow lockRuntime(Connection connection, RoomOrchestrator.RoomIdentity expected)
            throws SQLException {
        String sql = "SELECT room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state"
                + " FROM aoo_room_runtime_identity_authority WHERE room_id=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expected.roomId());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalStateException("ROOM_DISSOLVE_RUNTIME_NOT_FOUND");
                RuntimeRow row = new RuntimeRow(rows.getLong("room_id"),
                        new RoomRuntimeIdentity(rows.getString("room_instance_id"),
                                rows.getLong("lifecycle_epoch"), rows.getLong("fencing_token"),
                                rows.getLong("state_version")), rows.getString("lifecycle_state"));
                if (rows.next()) throw new IllegalStateException("ROOM_DISSOLVE_RUNTIME_DUPLICATE");
                if (row.roomId() != expected.roomId()
                        || !row.identity().sameGeneration(new RoomRuntimeIdentity(expected.roomInstanceId(),
                        expected.lifecycleEpoch(), expected.fencingToken(), row.identity().stateVersion()))) {
                    throw new SecurityException("ROOM_DISSOLVE_RUNTIME_IDENTITY_CONFLICT");
                }
                if (!"ACTIVE".equals(row.lifecycleState())) {
                    throw new IllegalStateException("ROOM_DISSOLVE_RUNTIME_NOT_ACTIVE");
                }
                return row;
            }
        }
    }

    private void requireCanonicalMetadata(Connection connection, RoomOrchestrator.RoomIdentity expected,
                                          RoomSnapshotMetadataSource requested) throws SQLException {
        String sql = "SELECT game_id,region_code,play_version,index_generation,release_id,rule_hash,"
                + "component_hash,card_codec_version,event_interpreter_version,protocol_version,source_hash,"
                + "snapshot_schema_version FROM aoo_room_rule_lock WHERE room_id=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expected.roomId());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalStateException("ROOM_DISSOLVE_CANONICAL_METADATA_NOT_FOUND");
                String sourceHash = row.getString("source_hash");
                String snapshotSchema = row.getString("snapshot_schema_version");
                if (sourceHash == null || snapshotSchema == null) {
                    throw new IllegalStateException("ROOM_DISSOLVE_CANONICAL_METADATA_INCOMPLETE");
                }
                if (row.getLong("game_id") != expected.gameId()
                        || !expected.playVersion().equals(row.getString("play_version"))
                        || row.getLong("index_generation") != requested.indexGeneration()
                        || row.getLong("release_id") != requested.releaseId()
                        || !requested.regionCode().equals(row.getString("region_code"))
                        || !requested.ruleContentHash().equals(row.getString("rule_hash"))
                        || !requested.componentContentHash().equals(row.getString("component_hash"))
                        || !requested.cardCodecVersion().equals(row.getString("card_codec_version"))
                        || !requested.eventInterpreterVersion().equals(row.getString("event_interpreter_version"))
                        || !requested.protocolVersion().equals(row.getString("protocol_version"))
                        || !requested.sourceHash().equals(sourceHash)
                        || !requested.snapshotSchemaVersion().equals(snapshotSchema)
                        || row.next()) {
                    throw new SecurityException("ROOM_DISSOLVE_CANONICAL_METADATA_CONFLICT");
                }
            }
        }
    }

    private static void requireExpectedState(RuntimeRow runtime, long expectedStateVersion) {
        if (runtime.stateVersion() != expectedStateVersion) {
            throw new IllegalStateException("ROOM_DISSOLVE_STATE_VERSION_CONFLICT");
        }
    }

    private static void requireActiveRoom(com.aoo.bcg.gamespi.RoomState roomState) {
        if (roomState == com.aoo.bcg.gamespi.RoomState.FINISHED
                || roomState == com.aoo.bcg.gamespi.RoomState.DISSOLVED) {
            throw new IllegalStateException("ROOM_DISSOLVE_ROOM_STATE_REJECTED");
        }
    }

    private static long nextStateVersion(long current) {
        try {
            return Math.addExact(current, 1L);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("ROOM_DISSOLVE_STATE_VERSION_OVERFLOW", overflow);
        }
    }

    private AggregateRow findAggregate(Connection connection, RoomOrchestrator.RoomIdentity expected,
                                       String voteId, boolean forUpdate) throws SQLException {
        String sql = "SELECT room_id,room_instance_id,lifecycle_epoch,fencing_token,state_version,"
                + "expected_state_version,vote_id,applicant_account_id,eligible_member_ids,deadline_at,"
                + "approve_on_timeout,votes,decision,terminal_tombstone FROM aoo_room_dissolve_authority"
                + " WHERE room_id=? AND lifecycle_epoch=? AND vote_id=?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expected.roomId());
            statement.setLong(2, expected.lifecycleEpoch());
            statement.setString(3, voteId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? decodeAggregate(row) : null;
            }
        }
    }

    private ReceiptRow findReceipt(Connection connection, RoomOrchestrator.RoomIdentity expected,
                                   String commandId) throws SQLException {
        String sql = "SELECT room_id,lifecycle_epoch,command_id,idempotency_key,operation,request_hash,"
                + "result_status,vote_id,applicant_account_id,eligible_member_ids,deadline_at,"
                + "approve_on_timeout,votes,decision,result_state_version,terminal_tombstone"
                + " FROM aoo_room_dissolve_command_receipt WHERE room_id=? AND lifecycle_epoch=? AND command_id=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expected.roomId());
            statement.setLong(2, expected.lifecycleEpoch());
            statement.setString(3, commandId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? decodeReceipt(row) : null;
            }
        }
    }

    private static void requireReceiptMatches(ReceiptRow receipt, String hash, String idempotencyKey,
                                              String operation) {
        if (!receipt.requestHash().equals(hash) || !receipt.idempotencyKey().equals(idempotencyKey)
                || !receipt.operation().equals(operation)) {
            throw new SecurityException("ROOM_DISSOLVE_IDEMPOTENCY_CONFLICT");
        }
    }

    private void insertAggregate(Connection connection, RoomOrchestrator.DissolveApplyCommand command,
                                 RoomRuntimeIdentity identity, long stateVersion,
                                 RoomOrchestrator.DissolveDecision decision, boolean terminal,
                                 Map<Long, Boolean> votes) throws SQLException {
        String sql = "INSERT INTO aoo_room_dissolve_authority(room_id,room_instance_id,lifecycle_epoch,"
                + "fencing_token,state_version,expected_state_version,vote_id,applicant_account_id,"
                + "eligible_member_ids,deadline_at,approve_on_timeout,votes,decision,terminal_tombstone,terminal_at)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.expectedIdentity().roomId());
            statement.setString(2, identity.roomInstanceId());
            statement.setLong(3, identity.lifecycleEpoch());
            statement.setLong(4, identity.fencingToken());
            statement.setLong(5, stateVersion);
            statement.setLong(6, command.expectedStateVersion());
            statement.setString(7, command.voteId());
            statement.setLong(8, command.applicantAccountId());
            statement.setString(9, toEligibleJson(command.eligibleMemberIds()));
            statement.setTimestamp(10, java.sql.Timestamp.from(command.deadline()));
            statement.setBoolean(11, command.approveOnTimeout());
            statement.setString(12, toVotesJson(votes));
            statement.setString(13, decision.name());
            statement.setBoolean(14, terminal);
            if (terminal) statement.setTimestamp(15, java.sql.Timestamp.from(clock.instant()));
            else statement.setNull(15, java.sql.Types.TIMESTAMP);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("ROOM_DISSOLVE_AGGREGATE_WRITE_FAILED");
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_AGGREGATE_WRITE_CONFLICT", failure);
        }
    }

    private void updateAggregate(Connection connection, RoomOrchestrator.DissolveVoteCommand command,
                                 AggregateRow aggregate, long expectedStateVersion, long stateVersion,
                                 RoomOrchestrator.DissolveDecision decision, boolean terminal,
                                 Map<Long, Boolean> votes) throws SQLException {
        String sql = "UPDATE aoo_room_dissolve_authority SET state_version=?,expected_state_version=?,"
                + "votes=?,decision=?,terminal_tombstone=?,terminal_at=?"
                + " WHERE room_id=? AND lifecycle_epoch=? AND vote_id=? AND state_version=?"
                + " AND terminal_tombstone=0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stateVersion);
            statement.setLong(2, expectedStateVersion);
            statement.setString(3, toVotesJson(votes));
            statement.setString(4, decision.name());
            statement.setBoolean(5, terminal);
            if (terminal) statement.setTimestamp(6, java.sql.Timestamp.from(clock.instant()));
            else statement.setNull(6, java.sql.Types.TIMESTAMP);
            statement.setLong(7, command.expectedIdentity().roomId());
            statement.setLong(8, command.expectedIdentity().lifecycleEpoch());
            statement.setString(9, command.voteId());
            statement.setLong(10, expectedStateVersion);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("ROOM_DISSOLVE_AGGREGATE_CAS_CONFLICT");
        }
    }

    private void updateRuntime(Connection connection, RoomOrchestrator.RoomIdentity expected,
                               RuntimeRow runtime, long stateVersion) throws SQLException {
        String sql = "UPDATE aoo_room_runtime_identity_authority SET state_version=?,updated_at=CURRENT_TIMESTAMP(3)"
                + " WHERE room_id=? AND room_instance_id=? AND lifecycle_epoch=? AND fencing_token=?"
                + " AND state_version=? AND lifecycle_state='ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stateVersion);
            statement.setLong(2, expected.roomId());
            statement.setString(3, runtime.identity().roomInstanceId());
            statement.setLong(4, runtime.identity().lifecycleEpoch());
            statement.setLong(5, runtime.identity().fencingToken());
            statement.setLong(6, runtime.stateVersion());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("ROOM_DISSOLVE_RUNTIME_CAS_CONFLICT");
        }
    }

    private void insertReceipt(Connection connection, RoomOrchestrator.DissolveApplyCommand command,
                               String hash, String operation, RoomOrchestrator.DissolveResult result)
            throws SQLException {
        insertReceipt(connection, command.expectedIdentity().roomId(), command.expectedIdentity().lifecycleEpoch(),
                command.commandId(), command.idempotencyKey(), hash, operation, result);
    }

    private void insertReceipt(Connection connection, RoomOrchestrator.DissolveVoteCommand command,
                               String hash, String operation, RoomOrchestrator.DissolveResult result)
            throws SQLException {
        insertReceipt(connection, command.expectedIdentity().roomId(), command.expectedIdentity().lifecycleEpoch(),
                command.commandId(), command.idempotencyKey(), hash, operation, result);
    }

    private void insertReceipt(Connection connection, long roomId, long lifecycleEpoch, String commandId,
                               String idempotencyKey, String hash, String operation,
                               RoomOrchestrator.DissolveResult result) throws SQLException {
        String sql = "INSERT INTO aoo_room_dissolve_command_receipt(room_id,lifecycle_epoch,command_id,"
                + "idempotency_key,operation,request_hash,result_status,vote_id,applicant_account_id,"
                + "eligible_member_ids,deadline_at,approve_on_timeout,votes,decision,result_state_version,"
                + "terminal_tombstone) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setLong(2, lifecycleEpoch);
            statement.setString(3, commandId);
            statement.setString(4, idempotencyKey);
            statement.setString(5, operation);
            statement.setString(6, hash);
            statement.setString(7, result.status().name());
            statement.setString(8, result.voteId());
            statement.setLong(9, result.applicantAccountId());
            statement.setString(10, toEligibleJson(result.eligibleMemberIds()));
            statement.setTimestamp(11, java.sql.Timestamp.from(result.deadline()));
            statement.setBoolean(12, result.approveOnTimeout());
            statement.setString(13, toVotesJson(result.votes()));
            statement.setString(14, result.decision().name());
            statement.setLong(15, result.identity().stateVersion());
            statement.setBoolean(16, result.terminalTombstone());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("ROOM_DISSOLVE_RECEIPT_WRITE_FAILED");
        } catch (SQLException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_RECEIPT_CONFLICT", failure);
        }
    }

    private RoomOrchestrator.DissolveResult result(RoomOrchestrator.RoomIdentity identity, String voteId,
                                                   long applicant, Set<Long> eligible, Instant deadline,
                                                   boolean approveOnTimeout, Map<Long, Boolean> votes,
                                                   RoomOrchestrator.DissolveDecision decision, boolean terminal,
                                                   RoomOrchestrator.DissolveStatus status, String commandId,
                                                   String idempotencyKey) {
        return new RoomOrchestrator.DissolveResult(identity, voteId, applicant, eligible, deadline,
                approveOnTimeout, votes, decision, terminal, status, commandId, idempotencyKey);
    }

    private static RoomOrchestrator.RoomIdentity identity(RoomOrchestrator.RoomIdentity requested,
                                                          RoomRuntimeIdentity runtime, long stateVersion) {
        return new RoomOrchestrator.RoomIdentity(requested.roomId(), runtime.roomInstanceId(),
                requested.gameId(), requested.gameCode(), requested.playVersion(), runtime.lifecycleEpoch(),
                runtime.fencingToken(), stateVersion);
    }

    private static RoomOrchestrator.DissolveDecision decide(AggregateRow aggregate,
                                                            Map<Long, Boolean> votes) {
        if (votes.values().stream().anyMatch(value -> !value)) return RoomOrchestrator.DissolveDecision.REJECTED;
        if (votes.keySet().containsAll(aggregate.eligibleMemberIds())) {
            return RoomOrchestrator.DissolveDecision.APPROVED;
        }
        return RoomOrchestrator.DissolveDecision.VOTING;
    }

    private AggregateRow decodeAggregate(ResultSet row) throws SQLException {
        try {
            return new AggregateRow(row.getLong("room_id"), row.getString("room_instance_id"),
                    row.getLong("lifecycle_epoch"), row.getLong("fencing_token"),
                    row.getLong("state_version"), row.getLong("expected_state_version"),
                    row.getString("vote_id"), row.getLong("applicant_account_id"),
                    parseEligible(row.getString("eligible_member_ids")),
                    row.getTimestamp("deadline_at").toInstant(), row.getBoolean("approve_on_timeout"),
                    parseVotes(row.getString("votes")),
                    RoomOrchestrator.DissolveDecision.valueOf(row.getString("decision")),
                    row.getBoolean("terminal_tombstone"));
        } catch (RuntimeException | JsonProcessingException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_AGGREGATE_INVALID", failure);
        }
    }

    private ReceiptRow decodeReceipt(ResultSet row) throws SQLException {
        try {
            return new ReceiptRow(row.getLong("room_id"), row.getLong("lifecycle_epoch"),
                    row.getString("command_id"), row.getString("idempotency_key"),
                    row.getString("operation"), row.getString("request_hash"),
                    RoomOrchestrator.DissolveStatus.valueOf(row.getString("result_status")),
                    row.getString("vote_id"), row.getLong("applicant_account_id"),
                    parseEligible(row.getString("eligible_member_ids")),
                    row.getTimestamp("deadline_at").toInstant(), row.getBoolean("approve_on_timeout"),
                    parseVotes(row.getString("votes")),
                    RoomOrchestrator.DissolveDecision.valueOf(row.getString("decision")),
                    row.getLong("result_state_version"), row.getBoolean("terminal_tombstone"));
        } catch (RuntimeException | JsonProcessingException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_RECEIPT_INVALID", failure);
        }
    }

    private Set<Long> parseEligible(String value) throws JsonProcessingException {
        return Set.copyOf(json.readValue(value, LONG_LIST));
    }

    private Map<Long, Boolean> parseVotes(String value) throws JsonProcessingException {
        Map<String, Boolean> values = json.readValue(value, VOTE_MAP);
        Map<Long, Boolean> result = new TreeMap<>();
        values.forEach((key, vote) -> result.put(Long.parseLong(key), Boolean.TRUE.equals(vote)));
        return Map.copyOf(result);
    }

    private String toEligibleJson(Set<Long> values) {
        try {
            return json.writeValueAsString(values.stream().sorted().toList());
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_ELIGIBLE_SERIALIZE_FAILED", failure);
        }
    }

    private String toVotesJson(Map<Long, Boolean> values) {
        try {
            return json.writeValueAsString(new TreeMap<>(values));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("ROOM_DISSOLVE_VOTES_SERIALIZE_FAILED", failure);
        }
    }

    private static String requestHash(RoomOrchestrator.DissolveApplyCommand command) {
        return sha256("APPLY|" + command.expectedIdentity() + "|" + command.staticIdentity() + "|"
                + command.sessionId() + "|" + command.applicantAccountId() + "|" + command.permission()
                + "|" + command.roomState() + "|" + command.voteId() + "|"
                + command.eligibleMemberIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(","))
                + "|" + command.deadline() + "|" + command.approveOnTimeout() + "|"
                + command.applicantAgrees() + "|" + command.expectedStateVersion() + "|"
                + command.idempotencyKey() + "|" + command.metadata());
    }

    private static String requestHash(RoomOrchestrator.DissolveVoteCommand command) {
        return sha256("VOTE|" + command.expectedIdentity() + "|" + command.staticIdentity() + "|"
                + command.sessionId() + "|" + command.voterAccountId() + "|" + command.permission()
                + "|" + command.roomState() + "|" + command.voteId() + "|" + command.agree() + "|"
                + command.expectedStateVersion() + "|" + command.idempotencyKey() + "|" + command.metadata());
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> { T apply(Connection connection) throws Exception; }

    private record RuntimeRow(long roomId, RoomRuntimeIdentity identity, String lifecycleState) {
        long stateVersion() { return identity.stateVersion(); }
    }

    private record AggregateRow(long roomId, String roomInstanceId, long lifecycleEpoch,
                                long fencingToken, long stateVersion, long expectedStateVersion,
                                String voteId, long applicantAccountId, Set<Long> eligibleMemberIds,
                                Instant deadline, boolean approveOnTimeout, Map<Long, Boolean> votes,
                                RoomOrchestrator.DissolveDecision decision, boolean terminalTombstone) { }

    private record ReceiptRow(long roomId, long lifecycleEpoch, String commandId,
                              String idempotencyKey, String operation, String requestHash,
                              RoomOrchestrator.DissolveStatus status, String voteId,
                              long applicantAccountId, Set<Long> eligibleMemberIds, Instant deadline,
                              boolean approveOnTimeout, Map<Long, Boolean> votes,
                              RoomOrchestrator.DissolveDecision decision, long resultStateVersion,
                              boolean terminalTombstone) {
        RoomOrchestrator.DissolveResult result(RoomOrchestrator.RoomIdentity requested,
                                                RoomRuntimeIdentity runtime) {
            RoomOrchestrator.RoomIdentity resultIdentity = identity(requested, runtime, resultStateVersion);
            return new RoomOrchestrator.DissolveResult(resultIdentity, voteId, applicantAccountId,
                    eligibleMemberIds, deadline, approveOnTimeout, votes, decision, terminalTombstone,
                    status, commandId, idempotencyKey);
        }
    }
}
