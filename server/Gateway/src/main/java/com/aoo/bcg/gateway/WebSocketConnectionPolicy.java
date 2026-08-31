package com.aoo.bcg.gateway;

import java.time.*;
import java.util.*;

/** State, heartbeat and bounded per-connection/per-room ordering policy. */
public final class WebSocketConnectionPolicy {
    public enum State { CONNECTING, ACTIVE, SUSPECT, CLOSED }
    public enum EnqueueResult { ACCEPTED, BACKPRESSURE, DISCONNECT }
    private final int highWatermark, capacity, disconnectStrikes;
    private final Duration heartbeat, halfOpen;
    private State state = State.CONNECTING; private Instant lastSeen; private int strikes;
    private final ArrayDeque<WebSocketFrame> queue = new ArrayDeque<>();
    public WebSocketConnectionPolicy(int highWatermark, int capacity, int disconnectStrikes, Duration heartbeat, Duration halfOpen, Instant now) {
        if (highWatermark < 1 || capacity < highWatermark || disconnectStrikes < 1 || heartbeat.isZero() || halfOpen.compareTo(heartbeat) <= 0) throw new IllegalArgumentException("invalid connection policy");
        this.highWatermark=highWatermark; this.capacity=capacity; this.disconnectStrikes=disconnectStrikes; this.heartbeat=heartbeat; this.halfOpen=halfOpen; this.lastSeen=Objects.requireNonNull(now);
    }
    public synchronized void activate(Instant now) { if (state != State.CONNECTING) throw new IllegalStateException("already activated"); state=State.ACTIVE; lastSeen=now; }
    public synchronized void heartbeat(Instant now) { if (state == State.CLOSED) return; lastSeen=now; state=State.ACTIVE; strikes=0; }
    public synchronized State inspect(Instant now) { Duration idle=Duration.between(lastSeen, now); if (idle.compareTo(halfOpen)>=0) state=State.CLOSED; else if(idle.compareTo(heartbeat)>=0) state=State.SUSPECT; return state; }
    public synchronized void kick() { state=State.CLOSED; queue.clear(); }
    public synchronized EnqueueResult enqueue(WebSocketFrame frame) {
        if (state == State.CLOSED) return EnqueueResult.DISCONNECT;
        if (queue.size() >= capacity) { if (++strikes >= disconnectStrikes) { kick(); return EnqueueResult.DISCONNECT; } return EnqueueResult.BACKPRESSURE; }
        queue.addLast(frame); return queue.size() > highWatermark ? EnqueueResult.BACKPRESSURE : EnqueueResult.ACCEPTED;
    }
    public synchronized Optional<WebSocketFrame> poll() { return Optional.ofNullable(queue.pollFirst()); }
    public synchronized int queued() { return queue.size(); }
}
