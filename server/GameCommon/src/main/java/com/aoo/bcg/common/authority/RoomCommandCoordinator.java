package com.aoo.bcg.common.authority;

import com.aoo.bcg.common.idempotency.IdempotencyKey;
import com.aoo.bcg.common.idempotency.IdempotencyStore;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-room linearization boundary for authority checks, idempotency, connection fencing and commit.
 * Different rooms never share a monitor; all writes for one room do.
 */
public final class RoomCommandCoordinator {
    public record PlayerBinding(String authenticatedUserId, int seatId, String connectionId,
                                long connectionVersion) {
        public PlayerBinding {
            if (authenticatedUserId == null || authenticatedUserId.isBlank() || seatId < 0
                    || connectionId == null || connectionId.isBlank() || connectionVersion <= 0)
                throw new IllegalArgumentException("invalid player binding");
        }
    }

    private final ConcurrentHashMap<Long, RoomAuthority> rooms = new ConcurrentHashMap<>();
    private final IdempotencyStore<GameCommandResult> idempotency;
    private final AuthoritativeTimeSource time;
    private final Duration retention;
    private final int receiptLimitPerPlayer;

    public RoomCommandCoordinator(IdempotencyStore<GameCommandResult> idempotency,
                                  AuthoritativeTimeSource time, Duration retention,
                                  int receiptLimitPerPlayer) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.time = Objects.requireNonNull(time, "time");
        this.retention = Objects.requireNonNull(retention, "retention");
        if (retention.isNegative() || retention.isZero() || receiptLimitPerPlayer < 1)
            throw new IllegalArgumentException("positive authority retention and receipt limit required");
        this.receiptLimitPerPlayer = receiptLimitPerPlayer;
    }

    public void register(GameRoomHandle room, int roundNo, long fencingToken) {
        Objects.requireNonNull(room, "room");
        if (roundNo < 0 || fencingToken <= 0) throw new IllegalArgumentException("invalid room authority");
        RoomAuthority created = new RoomAuthority(room, roundNo, fencingToken);
        if (rooms.putIfAbsent(room.roomId(), created) != null)
            throw new IllegalStateException("room authority already registered");
    }

    public void bind(long roomId, PlayerBinding binding) {
        RoomAuthority room = requireRoom(roomId);
        synchronized (room) {
            PlayerState existing = room.players.get(binding.authenticatedUserId());
            if (existing != null) {
                if (existing.binding.seatId() != binding.seatId())
                    throw new IllegalStateException("authenticated player cannot change seats on reconnect");
                if (binding.connectionVersion() <= existing.binding.connectionVersion())
                    throw new SecurityException("connection version must increase on takeover");
                existing.binding = binding;
                return;
            }
            String occupant = room.seats.get(binding.seatId());
            if (occupant != null) throw new IllegalStateException("seat already has an authenticated owner");
            room.seats.put(binding.seatId(), binding.authenticatedUserId());
            room.players.put(binding.authenticatedUserId(), new PlayerState(binding));
        }
    }

    public void advanceRound(long roomId, int expectedRoundNo, int nextRoundNo) {
        RoomAuthority room = requireRoom(roomId);
        synchronized (room) {
            if (room.roundNo != expectedRoundNo || nextRoundNo != Math.addExact(expectedRoundNo, 1))
                throw new IllegalStateException("room round must advance exactly once");
            room.roundNo = nextRoundNo;
        }
    }

    public void takeover(long roomId, long expectedFencingToken, long nextFencingToken,
                         GameRoomHandle restoredRoom) {
        RoomAuthority room = requireRoom(roomId);
        synchronized (room) {
            if (!room.active) throw new SecurityException("room authority is closed");
            if (room.fencingToken != expectedFencingToken || nextFencingToken <= expectedFencingToken)
                throw new SecurityException("stale room takeover");
            if (restoredRoom.roomId() != roomId || restoredRoom.gameId() != room.handle.gameId()
                    || !restoredRoom.playVersion().equals(room.handle.playVersion()))
                throw new IllegalArgumentException("restored room identity mismatch");
            room.handle = restoredRoom;
            room.fencingToken = nextFencingToken;
            room.recoveryRequired = false;
        }
    }

    public GameCommandResult execute(long roomId, long fencingToken, String connectionId,
                                     long connectionVersion, GameCommandRequest request,
                                     GameCommandHandler handler, GameCommandCommitter committer) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(committer, "committer");
        RoomAuthority room = requireRoom(roomId);
        synchronized (room) {
            PlayerState player = requireCurrentAuthority(room, roomId, fencingToken,
                    connectionId, connectionVersion, request);
            String fingerprint = fingerprint(request);
            String previousFingerprint = player.receipts.get(request.requestId());
            if (previousFingerprint != null && !previousFingerprint.equals(fingerprint))
                throw new SecurityException("requestId was reused with a different command");

            IdempotencyKey key = new IdempotencyKey(request.authenticatedUserId(), request.msgId(),
                    request.roomId(), request.roundNo(), request.requestId());
            var previous = idempotency.find(key);
            if (previous.isPresent()) {
                if (previousFingerprint == null)
                    throw new SecurityException("idempotent result has no matching authenticated receipt");
                return previous.orElseThrow();
            }
            if (request.sequence() != Math.addExact(player.lastSequence, 1))
                throw new SecurityException("command sequence must be continuous");
            if (!idempotency.acquire(key, retention))
                throw new IllegalStateException("request is already in progress");

            boolean mutationStarted = false;
            try {
                mutationStarted = true;
                GameCommandResult result = Objects.requireNonNull(handler.handle(room.handle, request),
                        "game command result");
                if (!result.requestId().equals(request.requestId()))
                    throw new IllegalStateException("command result requestId mismatch");
                GameCommandResult timed = result.serverTimeEpochMillis() == 0
                        ? result.withTiming(time.epochMillis(), room.handle.requireAuthoritativeSession().operationDeadline())
                        : result;
                committer.commit(room.handle, request, timed);
                player.lastSequence = request.sequence();
                remember(player.receipts, request.requestId(), fingerprint);
                idempotency.save(key, timed, retention);
                return timed;
            } catch (RuntimeException | Error failure) {
                if (mutationStarted) {
                    // A handler may have mutated its in-process authority before the durable
                    // commit failed. Its outcome is therefore unknown: retain the reservation
                    // and quarantine the room until a fenced snapshot/event recovery replaces it.
                    idempotency.markUnknown(key);
                    room.recoveryRequired = true;
                } else {
                    idempotency.release(key);
                }
                throw failure;
            }
        }
    }

    public int roomCount() { return rooms.size(); }
    public void remove(long roomId, long fencingToken) {
        RoomAuthority room = requireRoom(roomId);
        synchronized (room) {
            if (room.fencingToken != fencingToken) throw new SecurityException("stale room removal");
            room.active = false;
            rooms.remove(roomId, room);
        }
    }

    private PlayerState requireCurrentAuthority(RoomAuthority room, long roomId, long fencingToken,
                                                String connectionId, long connectionVersion,
                                                GameCommandRequest request) {
        if (room.handle.roomId() != roomId || request.roomId() != roomId)
            throw new SecurityException("room identity mismatch");
        if (!room.active) throw new SecurityException("room authority is closed");
        if (room.recoveryRequired) throw new SecurityException("room authority requires recovery");
        if (room.fencingToken != fencingToken) throw new SecurityException("stale room writer");
        if (request.roundNo() != room.roundNo) throw new SecurityException("stale room round");
        if (!request.playVersion().equals(room.handle.playVersion()))
            throw new SecurityException("play version drift");
        PlayerState player = room.players.get(request.authenticatedUserId());
        if (player == null || player.binding.seatId() != request.seatId())
            throw new SecurityException("authenticated user does not own the requested seat");
        if (!player.binding.connectionId().equals(connectionId)
                || player.binding.connectionVersion() != connectionVersion)
            throw new SecurityException("connection was superseded");
        return player;
    }

    private RoomAuthority requireRoom(long roomId) {
        RoomAuthority room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("room authority not registered");
        return room;
    }

    private void remember(LinkedHashMap<String, String> receipts, String requestId, String fingerprint) {
        receipts.put(requestId, fingerprint);
        while (receipts.size() > receiptLimitPerPlayer) receipts.remove(receipts.firstEntry().getKey());
    }

    private static String fingerprint(GameCommandRequest request) {
        StringBuilder canonical = new StringBuilder();
        appendCanonical(canonical, request.msgId());
        appendCanonical(canonical, request.sequence());
        appendCanonical(canonical, request.body().asMap());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void appendCanonical(StringBuilder out, Object value) {
        if (value == null) { out.append('N'); return; }
        if (value instanceof String text) { appendToken(out, 'S', text); return; }
        if (value instanceof Character character) { appendToken(out, 'C', character.toString()); return; }
        if (value instanceof Boolean flag) { out.append(flag ? "B1" : "B0"); return; }
        if (value instanceof Number number) {
            if ((number instanceof Double && !Double.isFinite(number.doubleValue()))
                    || (number instanceof Float && !Float.isFinite(number.floatValue())))
                throw new IllegalArgumentException("non-finite command number cannot be fingerprinted");
            appendToken(out, 'D', number.toString());
            return;
        }
        if (value instanceof Enum<?> constant) { appendToken(out, 'E', constant.name()); return; }
        if (value instanceof UUID || value instanceof TemporalAccessor || value instanceof TemporalAmount) {
            appendToken(out, 'V', value.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> {
                if (!(key instanceof String text))
                    throw new IllegalArgumentException("command map keys must be strings");
                sorted.put(text, item);
            });
            out.append('M').append(sorted.size()).append('{');
            sorted.forEach((key, item) -> {
                appendCanonical(out, key);
                appendCanonical(out, item);
            });
            out.append('}');
            return;
        }
        if (value instanceof Collection<?> values) {
            out.append('L').append(values.size()).append('[');
            values.forEach(item -> appendCanonical(out, item));
            out.append(']');
            return;
        }
        throw new IllegalArgumentException("unsupported command fingerprint value: " + value.getClass().getName());
    }

    private static void appendToken(StringBuilder out, char type, String value) {
        out.append(type).append(value.length()).append(':').append(value);
    }

    private static final class RoomAuthority {
        private GameRoomHandle handle;
        private int roundNo;
        private long fencingToken;
        private boolean active = true;
        private boolean recoveryRequired;
        private final Map<String, PlayerState> players = new HashMap<>();
        private final Map<Integer, String> seats = new HashMap<>();
        private RoomAuthority(GameRoomHandle handle, int roundNo, long fencingToken) {
            this.handle = handle;
            this.roundNo = roundNo;
            this.fencingToken = fencingToken;
        }
    }

    private static final class PlayerState {
        private PlayerBinding binding;
        private long lastSequence;
        private final LinkedHashMap<String, String> receipts = new LinkedHashMap<>();
        private PlayerState(PlayerBinding binding) { this.binding = binding; }
    }
}
