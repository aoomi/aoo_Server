package com.aoo.bcg.common.room;

import java.util.Comparator;
import java.util.Map;

/** Durable room-owner identity. Connectivity never implicitly changes ownership. */
public final class RoomOwnership {
    public enum Status { ACTIVE, OWNER_OFFLINE, DISSOLVED }
    private long ownerId;
    private long revision;
    private Status status;

    public RoomOwnership(long ownerId) { this(ownerId, 0, Status.ACTIVE); }
    private RoomOwnership(long ownerId, long revision, Status status) {
        if (ownerId <= 0 || revision < 0 || status == null) throw new IllegalArgumentException("invalid room ownership");
        this.ownerId = ownerId; this.revision = revision; this.status = status;
    }
    public static RoomOwnership restore(long ownerId, long revision, Status status) { return new RoomOwnership(ownerId, revision, status); }
    public synchronized void disconnected(long playerId) {
        requireActive(); if (playerId == ownerId) status = Status.OWNER_OFFLINE;
    }
    public synchronized void reconnected(long playerId) {
        requireNotDissolved(); if (playerId == ownerId) status = Status.ACTIVE;
    }
    public synchronized void transfer(long actorId, long nextOwnerId, Map<Integer, RoomSeat> seats) {
        requireNotDissolved();
        if (actorId != ownerId) throw new SecurityException("owner only");
        if (nextOwnerId <= 0 || nextOwnerId == ownerId || !occupied(nextOwnerId, seats)) throw new IllegalArgumentException("next owner must occupy a seat");
        ownerId = nextOwnerId; revision = increment(); status = Status.ACTIVE;
    }
    public synchronized boolean ownerLeaving(long actorId, OwnerDeparturePolicy policy, Map<Integer, RoomSeat> seats) {
        requireNotDissolved();
        if (actorId != ownerId) return false;
        return switch (java.util.Objects.requireNonNull(policy)) {
            case RETAIN_OFFLINE_OWNER -> { status = Status.OWNER_OFFLINE; yield false; }
            case DISSOLVE_ROOM -> { status = Status.DISSOLVED; revision = increment(); yield true; }
            case TRANSFER_TO_LOWEST_OCCUPIED_SEAT -> {
                RoomSeat successor = seats.values().stream().filter(seat -> seat.playerId() != ownerId)
                        .filter(seat -> seat.status() != SeatStatus.EMPTY && seat.status() != SeatStatus.LEFT)
                        .min(Comparator.comparingInt(RoomSeat::seatId)).orElseThrow(() -> new IllegalStateException("no owner successor"));
                ownerId = successor.playerId(); revision = increment(); status = Status.ACTIVE; yield false;
            }
        };
    }
    public synchronized void dissolved(long actorId) { requireNotDissolved(); if (actorId != ownerId) throw new SecurityException("owner only"); status = Status.DISSOLVED; revision = increment(); }
    public synchronized long ownerId() { return ownerId; }
    public synchronized long revision() { return revision; }
    public synchronized Status status() { return status; }
    private static boolean occupied(long playerId, Map<Integer, RoomSeat> seats) { return seats.values().stream().anyMatch(seat -> seat.playerId() == playerId && seat.status() != SeatStatus.EMPTY && seat.status() != SeatStatus.LEFT); }
    private void requireActive() { if (status == Status.DISSOLVED) throw new IllegalStateException("room dissolved"); }
    private void requireNotDissolved() { requireActive(); }
    private long increment() { return com.aoo.bcg.common.math.ExactDomainMath.increment(revision, "room ownership revision"); }
}
