package com.aoo.bcg.gamespi;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps authentication/resource/snapshot preparation separate from authoritative seat mutation. */
public final class SeatAdmissionCoordinator {
    private record Key(long roomId, PlayerSeatIdentity identity) { }
    private final Map<Key, SeatAdmission> admissions = new ConcurrentHashMap<>();
    private final Clock clock;

    public SeatAdmissionCoordinator() { this(Clock.systemUTC()); }
    public SeatAdmissionCoordinator(Clock clock) { this.clock = java.util.Objects.requireNonNull(clock); }

    public SeatAdmission prepare(GameRoomHandle room, GameCommandRequest request) {
        PlayerSeatIdentity identity = request.playerSeatIdentity();
        Key key = new Key(room.roomId(), identity);
        transition(key, SeatAdmissionPhase.AUTHENTICATED, "");
        if (!room.playVersion().equals(request.playVersion())) {
            reject(key, "PLAY_VERSION_MISMATCH");
            throw new SecurityException("room play version mismatch");
        }
        transition(key, SeatAdmissionPhase.RESOURCES_READY, "");
        room.requireAuthoritativeSession().authoritativeState();
        return transition(key, SeatAdmissionPhase.SNAPSHOT_READY, "");
    }

    public SeatAdmission admitted(GameRoomHandle room, GameCommandRequest request) {
        return transition(new Key(room.roomId(), request.playerSeatIdentity()), SeatAdmissionPhase.ADMITTED, "");
    }

    public SeatAdmission rejected(GameRoomHandle room, GameCommandRequest request, Throwable failure) {
        String code = failure == null ? "JOIN_REJECTED" : failure.getClass().getSimpleName();
        return reject(new Key(room.roomId(), request.playerSeatIdentity()), code);
    }

    public Optional<SeatAdmission> find(long roomId, PlayerSeatIdentity identity) {
        return Optional.ofNullable(admissions.get(new Key(roomId, identity)));
    }

    public void clear(long roomId, PlayerSeatIdentity identity) { admissions.remove(new Key(roomId, identity)); }

    private SeatAdmission reject(Key key, String code) { return transition(key, SeatAdmissionPhase.REJECTED, code); }
    private SeatAdmission transition(Key key, SeatAdmissionPhase phase, String failureCode) {
        SeatAdmission next = new SeatAdmission(key.roomId(), key.identity(), phase, Instant.now(clock), failureCode);
        admissions.compute(key, (ignored, previous) -> {
            if (previous != null && previous.phase() == SeatAdmissionPhase.ADMITTED && phase != SeatAdmissionPhase.ADMITTED)
                throw new IllegalStateException("admitted seat cannot return to pre-seat state");
            return next;
        });
        return next;
    }
}
