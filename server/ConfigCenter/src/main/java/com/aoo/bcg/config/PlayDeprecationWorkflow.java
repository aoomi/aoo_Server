package com.aoo.bcg.config;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Audited one-way retirement: stop creation, drain, archive, then retire dependencies. */
public final class PlayDeprecationWorkflow {
    public enum State { ACTIVE, NEW_ROOMS_STOPPED, DRAINING, ARCHIVED, COMPONENTS_RETIRED }
    public record Event(State from, State to, long operatorId, String reason, Instant occurredAt,
                        long activeRooms, String archiveReference) { }
    private final Clock clock;
    private State state = State.ACTIVE;
    private long activeRooms;
    private final List<Event> events = new ArrayList<>();

    public PlayDeprecationWorkflow(Clock clock, long activeRooms) {
        if (clock == null || activeRooms < 0) throw new IllegalArgumentException("invalid deprecation workflow");
        this.clock = clock; this.activeRooms = activeRooms;
    }
    public synchronized State state() { return state; }
    public synchronized long activeRooms() { return activeRooms; }
    public synchronized List<Event> events() { return List.copyOf(events); }
    public synchronized void updateActiveRooms(long count) {
        if (state != State.NEW_ROOMS_STOPPED && state != State.DRAINING)
            throw new IllegalStateException("room drain telemetry is not active");
        if (count < 0 || count > activeRooms) throw new IllegalArgumentException("active rooms must decrease monotonically");
        activeRooms = count;
    }
    public synchronized void stopNewRooms(long operatorId, String reason) { move(State.NEW_ROOMS_STOPPED, operatorId, reason, ""); }
    public synchronized void beginDrain(long operatorId, String reason) { move(State.DRAINING, operatorId, reason, ""); }
    public synchronized void archive(long operatorId, String reason, String archiveReference) {
        if (activeRooms != 0) throw new IllegalStateException("cannot archive while rooms are active");
        if (archiveReference == null || archiveReference.isBlank()) throw new IllegalArgumentException("archive reference is required");
        move(State.ARCHIVED, operatorId, reason, archiveReference);
    }
    public synchronized void retireComponents(long operatorId, String reason, boolean dependenciesStillReferenced) {
        if (dependenciesStillReferenced) throw new IllegalStateException("components are still referenced by live or archived snapshots");
        move(State.COMPONENTS_RETIRED, operatorId, reason, "");
    }
    private void move(State target, long operatorId, String reason, String archiveReference) {
        if (operatorId <= 0 || reason == null || reason.isBlank()) throw new IllegalArgumentException("operator and reason are required");
        if (state == State.COMPONENTS_RETIRED) throw new IllegalStateException("play deprecation is already complete");
        State expected = State.values()[state.ordinal() + 1];
        if (target != expected) throw new IllegalStateException("invalid deprecation transition: " + state + " -> " + target);
        State previous = state; state = target;
        events.add(new Event(previous, target, operatorId, reason, clock.instant(), activeRooms, archiveReference));
    }
}
