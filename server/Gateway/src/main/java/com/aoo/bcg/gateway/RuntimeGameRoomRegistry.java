package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import com.aoo.bcg.gamespi.time.MonotonicTicker;

/** The single room-handle registry shared by creation, commands, reconnect and settlement. */
public final class RuntimeGameRoomRegistry implements GameRoomResolver {
    private static final RuntimeGameRoomRegistry GLOBAL = new RuntimeGameRoomRegistry();
    private final ConcurrentHashMap<Long, GameRoomHandle> rooms = new ConcurrentHashMap<>();
    private final AtomicReference<CreationEvidence> lastCreation = new AtomicReference<>();
    private final MonotonicTicker ticker;

    public RuntimeGameRoomRegistry() { this(MonotonicTicker.system()); }
    RuntimeGameRoomRegistry(MonotonicTicker ticker) { this.ticker = Objects.requireNonNull(ticker, "ticker"); }

    public static RuntimeGameRoomRegistry global() { return GLOBAL; }

    public GameRoomHandle create(GameProvider provider, RoomCreationContext context) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(context, "context");
        long started = ticker.mark();
        var descriptor = provider.descriptor();
        List<String> components = provider.ruleComponents().stream()
                .map(component -> component.ruleId() + "@" + component.componentVersion())
                .sorted().toList();
        String ruleHash = sha256(context.immutableRules());
        GameRoomHandle created = null;
        boolean registeredByThisAttempt = false;
        try {
            created = provider.roomFactory().create(context);
            if (created.roomId() != context.roomId() || created.gameId() != descriptor.gameId()
                    || !created.playVersion().equals(descriptor.version())) {
                throw new IllegalStateException("provider returned a mismatched room handle");
            }
            GameRoomHandle previous = rooms.putIfAbsent(created.roomId(), created);
            if (previous != null) throw new IllegalStateException("room already exists: " + created.roomId());
            registeredByThisAttempt = true;
            lastCreation.set(new CreationEvidence(context.roomId(), descriptor.gameId(), descriptor.code(),
                    descriptor.version(), components, ruleHash, ticker.elapsedSince(started).toNanos(),
                    true, true, ""));
            return created;
        } catch (RuntimeException error) {
            // The registry never exposes a half-created handle. Provider-owned
            // external resources still require a future compensating lifecycle SPI.
            if (registeredByThisAttempt && created != null) rooms.remove(context.roomId(), created);
            lastCreation.set(new CreationEvidence(context.roomId(), descriptor.gameId(), descriptor.code(),
                    descriptor.version(), components, ruleHash, ticker.elapsedSince(started).toNanos(),
                    true, false, error.getClass().getSimpleName()));
            throw error;
        }
    }

    public GameRoomHandle bind(GameRoomHandle room) {
        Objects.requireNonNull(room, "room");
        GameRoomHandle previous = rooms.putIfAbsent(room.roomId(), room);
        if (previous != null && previous != room) throw new IllegalStateException("room already bound: " + room.roomId());
        return previous == null ? room : previous;
    }

    @Override public GameRoomHandle require(long roomId) {
        GameRoomHandle room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("room does not exist: " + roomId);
        return room;
    }

    public void remove(long roomId) { rooms.remove(roomId); }
    public List<GameRoomHandle> snapshot() { return List.copyOf(rooms.values()); }
    public int size() { return rooms.size(); }
    public Optional<CreationEvidence> lastCreationEvidence() { return Optional.ofNullable(lastCreation.get()); }

    private static String sha256(java.util.Map<String, Object> rules) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            rules.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '=');
                digest.update(String.valueOf(entry.getValue()).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            });
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public record CreationEvidence(long roomId, int gameId, String gameCode, String playVersion,
            List<String> componentChain, String immutableRuleHash, long elapsedNanos,
            boolean registryIndexHit, boolean atomicallyRegistered, String failureType) {
        public CreationEvidence { componentChain = List.copyOf(componentChain); }
    }
}
