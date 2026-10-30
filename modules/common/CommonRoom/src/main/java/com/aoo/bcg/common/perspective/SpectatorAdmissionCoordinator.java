package com.aoo.bcg.common.perspective;

import com.aoo.bcg.common.room.AuthoritativeRoom;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/** Spectators receive cropped views and may become players only at an explicit round boundary. */
public final class SpectatorAdmissionCoordinator {
    private record Key(long roomId, long playerId) { }
    private final Map<Key, SpectatorAdmission> values = new ConcurrentHashMap<>();
    public SpectatorAdmission watch(long roomId, long playerId, int currentRoundNo) {
        Key key = new Key(roomId, playerId);
        SpectatorAdmission value = new SpectatorAdmission(roomId, playerId, SpectatorAdmission.State.SPECTATING,
                OptionalInt.empty(), currentRoundNo, 0);
        if (values.putIfAbsent(key, value) != null) throw new IllegalStateException("viewer already admitted");
        return value;
    }
    public SpectatorAdmission reserveForNextRound(long roomId, long playerId, int seatId, int currentRoundNo) {
        Key key = new Key(roomId, playerId);
        return values.compute(key, (ignored, current) -> {
            if (current == null || current.state() != SpectatorAdmission.State.SPECTATING) throw new IllegalStateException("not an eligible spectator");
            return new SpectatorAdmission(roomId, playerId, SpectatorAdmission.State.RESERVED_FOR_NEXT_ROUND,
                    OptionalInt.of(seatId), Math.addExact(currentRoundNo, 1), current.revision() + 1);
        });
    }
    public SpectatorAdmission activate(AuthoritativeRoom room, long playerId, int roundNo) {
        Key key = new Key(room.roomId(), playerId);
        return values.compute(key, (ignored, current) -> {
            if (current == null || current.state() != SpectatorAdmission.State.RESERVED_FOR_NEXT_ROUND
                    || current.effectiveRoundNo() != roundNo) throw new IllegalStateException("promotion is not effective in this round");
            int seatId = current.requestedSeatId().orElseThrow();
            room.assign(seatId, playerId);
            return new SpectatorAdmission(room.roomId(), playerId, SpectatorAdmission.State.PLAYER,
                    OptionalInt.of(seatId), roundNo, current.revision() + 1);
        });
    }
    public ViewerContext viewer(long roomId, long playerId, int roundNo) {
        SpectatorAdmission admission = values.get(new Key(roomId, playerId));
        ViewerRole role = admission != null && admission.state() == SpectatorAdmission.State.PLAYER
                && admission.effectiveRoundNo() <= roundNo ? ViewerRole.PLAYER : ViewerRole.SPECTATOR;
        return new ViewerContext(roomId, playerId, role);
    }
    public Optional<SpectatorAdmission> find(long roomId, long playerId) { return Optional.ofNullable(values.get(new Key(roomId, playerId))); }
}
