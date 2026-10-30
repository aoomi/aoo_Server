package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionValidator;
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
    private static final AtomicReference<RuntimeGameRoomRegistry> GLOBAL = new AtomicReference<>();
    private final ConcurrentHashMap<Long, GameRoomHandle> rooms = new ConcurrentHashMap<>();
    private final AtomicReference<CreationEvidence> lastCreation = new AtomicReference<>();
    private final MonotonicTicker ticker;
    private final RuntimeMetadataProjection runtimeMetadataProjection;

    public RuntimeGameRoomRegistry() { this(MonotonicTicker.system(), null); }
    public RuntimeGameRoomRegistry(RuntimeMetadataProjection runtimeMetadataProjection) {
        this(MonotonicTicker.system(), runtimeMetadataProjection);
    }
    RuntimeGameRoomRegistry(MonotonicTicker ticker) { this(ticker, null); }
    RuntimeGameRoomRegistry(MonotonicTicker ticker, RuntimeMetadataProjection runtimeMetadataProjection) {
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.runtimeMetadataProjection = runtimeMetadataProjection == null ? null
                : RuntimeMetadataProjectionValidator.validate(runtimeMetadataProjection);
    }

    /** Returns the production registry only after Bootstrap installs its immutable projection. */
    public static RuntimeGameRoomRegistry global() {
        RuntimeGameRoomRegistry registry = GLOBAL.get();
        if (registry == null) throw new IllegalStateException("RUNTIME_METADATA_GLOBAL_PROJECTION_REQUIRED");
        return registry;
    }

    /** Installs the single production global; a different projection is a hard conflict. */
    public static RuntimeGameRoomRegistry configureGlobal(RuntimeMetadataProjection projection) {
        RuntimeGameRoomRegistry candidate = new RuntimeGameRoomRegistry(
                Objects.requireNonNull(projection, "runtime metadata projection"));
        RuntimeGameRoomRegistry existing = GLOBAL.get();
        if (existing == null && GLOBAL.compareAndSet(null, candidate)) return candidate;
        existing = GLOBAL.get();
        if (existing.runtimeMetadataProjection == null
                || !existing.runtimeMetadataProjection.equals(candidate.runtimeMetadataProjection)) {
            throw new IllegalStateException("RUNTIME_METADATA_GLOBAL_PROJECTION_CONFLICT");
        }
        return existing;
    }

    public GameRoomHandle create(GameProvider provider, RoomCreationContext context) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(context, "context");
        long started = ticker.mark();
        var descriptor = provider.descriptor();
        requireRuntimeMetadata(descriptor);
        List<String> components = provider.ruleComponents().stream()
                .map(component -> component.ruleId() + "@" + component.componentVersion())
                .sorted().toList();
        String ruleHash = sha256(context.immutableRules());
        GameRoomHandle created = null;
        boolean registeredByThisAttempt = false;
        try {
            created = provider.roomFactory().create(context);
            requireRuntimeIdentity(created);
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
        requireRuntimeIdentity(room);
        requireRuntimeMetadata(room);
        GameRoomHandle previous = rooms.putIfAbsent(room.roomId(), room);
        if (previous != null && previous != room) {
            if (sameRuntimeIdentity(previous, room)) return previous;
            throw new IllegalStateException("room identity conflict: " + room.roomId());
        }
        return previous == null ? room : previous;
    }

    /** Binds a handle from the new runtime; an untyped handle is never accepted. */
    public GameRoomHandle bindRuntime(GameRoomHandle room) {
        return bind(room);
    }

    /** Returns only a handle carrying the explicit runtime fence. */
    public GameRoomHandle requireRuntime(long roomId) {
        GameRoomHandle room = require(roomId);
        requireRuntimeIdentity(room);
        return room;
    }

    /**
     * Advances state within one authority generation. A different instance,
     * lifecycle epoch, or fencing token is a conflict, and an older state
     * revision is stale; neither can replace the registered handle.
     */
    public GameRoomHandle updateRuntime(GameRoomHandle replacement) {
        requireRuntimeIdentity(replacement);
        return rooms.compute(replacement.roomId(), (roomId, current) -> {
            if (current == null) throw new IllegalStateException("room does not exist: " + roomId);
            requireRuntimeIdentity(current);
            RoomRuntimeIdentity before = current.runtimeIdentity();
            RoomRuntimeIdentity after = replacement.runtimeIdentity();
            if (!before.sameGeneration(after)) {
                throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_CONFLICT");
            }
            if (after.stateVersion() < before.stateVersion()) {
                throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_STALE");
            }
            if (after.stateVersion() == before.stateVersion() && current != replacement) {
                throw new IllegalStateException("ROOM_RUNTIME_STATE_VERSION_CONFLICT");
            }
            return replacement;
        });
    }

    /** Publishes a freshly restored durable session against the exact failed handle.
     * Caller holds its room's exclusive recovery gate and has verified one durable identity/snapshot.
     * The ordinary monotonic update contract is deliberately unchanged. */
    public GameRoomHandle restoreRuntimeFromDurable(GameRoomHandle expected, GameRoomHandle restored) {
        Objects.requireNonNull(expected, "expected"); requireRuntimeIdentity(expected); requireRuntimeIdentity(restored);
        if (expected.roomId()!=restored.roomId() || expected.gameId()!=restored.gameId()
                || !expected.playVersion().equals(restored.playVersion())
                || !Objects.equals(expected.snapshotMetadataSource(),restored.snapshotMetadataSource())
                || !Objects.equals(expected.compatibilityOrigin(),restored.compatibilityOrigin()))
            throw new IllegalStateException("ROOM_DURABLE_RESTORE_STATIC_IDENTITY_CONFLICT");
        RoomRuntimeIdentity before=expected.runtimeIdentity(), after=restored.runtimeIdentity();
        if (!before.sameGeneration(after)) throw new IllegalStateException("ROOM_DURABLE_RESTORE_GENERATION_CONFLICT");
        if (after.stateVersion()<before.stateVersion()) throw new IllegalStateException("ROOM_DURABLE_RESTORE_STALE");
        if (restored.requireAuthoritativeSession().stateVersion()!=after.stateVersion())
            throw new IllegalStateException("ROOM_DURABLE_RESTORE_SESSION_VERSION_CONFLICT");
        return rooms.compute(restored.roomId(),(roomId,current)->{
            if(current!=expected) throw new IllegalStateException("ROOM_DURABLE_RESTORE_EXPECTED_REFERENCE_CONFLICT");
            return restored;
        });
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

    private static void requireRuntimeIdentity(GameRoomHandle room) {
        Objects.requireNonNull(room, "room");
        if (room.runtimeIdentity() == null) {
            throw new IllegalStateException("ROOM_RUNTIME_IDENTITY_REQUIRED");
        }
    }

    private void requireRuntimeMetadata(com.aoo.bcg.gamespi.GameDescriptor descriptor) {
        if (runtimeMetadataProjection == null) return;
        RuntimeMetadataProjectionValidator.validate(descriptor, runtimeMetadataProjection);
    }

    private void requireRuntimeMetadata(GameRoomHandle room) {
        if (runtimeMetadataProjection == null) return;
        RuntimeMetadataProjection.Entry row = runtimeMetadataProjection.require(room.gameId());
        if (!row.playVersion().equals(room.playVersion())) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_IDENTITY_MISMATCH gameId="
                    + room.gameId() + " playVersion=" + room.playVersion());
        }
    }

    private static boolean sameRuntimeIdentity(GameRoomHandle left, GameRoomHandle right) {
        return left.runtimeIdentity() != null && right.runtimeIdentity() != null
                && left.runtimeIdentity().equals(right.runtimeIdentity());
    }

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
