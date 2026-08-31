package com.aoo.bcg.common.room;

import com.aoo.bcg.gamespi.RoomState;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AuthoritativeRoom {
    private final long roomId;
    private final int gameId;
    private final String playVersion;
    private final Map<Integer, RoomSeat> seats = new LinkedHashMap<>();
    private final RoomOwnership ownership;
    private final ReadyStateRegistry readiness;
    private RoomState state = RoomState.CREATED;
    private int roundNo;
    protected AuthoritativeRoom(long roomId, int gameId, String playVersion, int seatCount) {
        this(RoomDefinition.unowned(roomId, gameId, playVersion, seatCount));
    }
    protected AuthoritativeRoom(long roomId, int gameId, String playVersion, int seatCount, long ownerId) {
        this(new RoomDefinition(roomId, gameId, playVersion, seatCount, ownerId));
    }
    protected AuthoritativeRoom(RoomDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("room definition is required");
        this.roomId = definition.roomId(); this.gameId = definition.gameId(); this.playVersion = definition.playVersion();
        this.ownership = definition.ownerId() > 0 ? new RoomOwnership(definition.ownerId()) : null;
        for (int i = 0; i < definition.seatCount(); i++) seats.put(i, new RoomSeat(i, 0, SeatStatus.EMPTY, 0));
        this.readiness = new ReadyStateRegistry(seats.keySet());
    }
    public final synchronized void transition(RoomState expected, RoomState next) {
        if (state != expected || !allowed(expected, next)) throw new IllegalStateException("illegal transition " + state + " -> " + next);
        state = next;
    }
    public final synchronized SeatAssignment assign(int seatId, long playerId) {
        requirePlayerAvailable(playerId);
        RoomSeat seat = seats.get(seatId);
        if (seat == null || (seat.status() != SeatStatus.EMPTY && seat.status() != SeatStatus.LEFT)) throw new IllegalStateException("seat unavailable");
        seats.put(seatId, new RoomSeat(seatId, playerId, SeatStatus.OCCUPIED, 0));
        readiness.apply(ReadyLifecycleEvent.JOINED, seatId);
        return new SeatAssignment(seatId, playerId);
    }
    public final synchronized SeatAssignment assignAutomatically(long playerId) {
        requirePlayerAvailable(playerId);
        int seatId = seats.values().stream()
                .filter(seat -> seat.status() == SeatStatus.EMPTY || seat.status() == SeatStatus.LEFT)
                .mapToInt(RoomSeat::seatId).min().orElseThrow(() -> new IllegalStateException("room is full"));
        seats.put(seatId, new RoomSeat(seatId, playerId, SeatStatus.OCCUPIED, 0));
        readiness.apply(ReadyLifecycleEvent.JOINED, seatId);
        return new SeatAssignment(seatId, playerId);
    }
    public final synchronized void rollbackAssignment(SeatAssignment assignment) {
        if (assignment == null) return;
        RoomSeat current = seats.get(assignment.seatId());
        if (current == null || current.playerId() != assignment.playerId()) {
            throw new IllegalStateException("seat assignment ownership changed");
        }
        seats.put(assignment.seatId(), new RoomSeat(assignment.seatId(), 0, SeatStatus.EMPTY, 0));
        readiness.apply(ReadyLifecycleEvent.LEFT, assignment.seatId());
    }
    public final synchronized Map<Integer, RoomSeat> seats() { return Map.copyOf(seats); }
    public final long roomId() { return roomId; }
    public final int gameId() { return gameId; }
    public final String playVersion() { return playVersion; }
    public final synchronized RoomState state() { return state; }
    public final synchronized int roundNo() { return roundNo; }
    public final ReadyStateRegistry readiness() { return readiness; }
    public final RoomOwnership ownership() {
        if (ownership == null) throw new IllegalStateException("room owner was not configured");
        return ownership;
    }
    protected final synchronized void beginNextRound() {
        roundNo = com.aoo.bcg.common.math.ExactDomainMath.increment(roundNo, "room round number");
    }
    private void requirePlayerAvailable(long playerId) {
        if (playerId <= 0) throw new IllegalArgumentException("invalid player");
        if (seats.values().stream().anyMatch(seat -> seat.playerId() == playerId
                && seat.status() != SeatStatus.EMPTY && seat.status() != SeatStatus.LEFT)) {
            throw new IllegalStateException("player already seated");
        }
    }
    private static boolean allowed(RoomState from, RoomState to) {
        return switch (from) {
            case CREATED -> to == RoomState.WAITING || to == RoomState.DISSOLVED;
            case WAITING -> to == RoomState.PLAYING || to == RoomState.DISSOLVED;
            case PLAYING -> to == RoomState.SUSPENDED || to == RoomState.SETTLING || to == RoomState.DISSOLVED;
            case SUSPENDED -> to == RoomState.PLAYING || to == RoomState.DISSOLVED;
            case SETTLING -> to == RoomState.WAITING || to == RoomState.FINISHED || to == RoomState.DISSOLVED;
            case FINISHED, DISSOLVED -> false;
        };
    }
}
