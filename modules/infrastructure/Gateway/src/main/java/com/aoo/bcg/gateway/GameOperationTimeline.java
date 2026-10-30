package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.time.OperationDeadline;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded per-room timeline used to reconstruct stalls without dumping authority state. */
public final class GameOperationTimeline {
    public record Entry(long roomId, long eventSeq, String requestId, String msgId, String phase,
                        int opPos, long deadlineEpochMillis, int queueLength,
                        String threadName, Thread.State threadState, Instant observedAt, String failureType) { }
    private final int capacity;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Map<Long, ArrayDeque<Entry>> entries = new ConcurrentHashMap<>();

    public GameOperationTimeline(int capacity) {
        if (capacity < 8 || capacity > 4096) throw new IllegalArgumentException("timeline capacity out of range");
        this.capacity = capacity;
    }

    public void begin(long roomId, long eventSeq, String requestId, String msgId) {
        append(roomId, eventSeq, requestId, msgId, "RECEIVED", OperationDeadline.none(),
                inFlight.incrementAndGet(), "");
    }

    public void complete(long roomId, long eventSeq, String requestId, String msgId,
                         OperationDeadline deadline) {
        append(roomId, eventSeq, requestId, msgId, "COMMITTED", deadline,
                Math.max(0, inFlight.decrementAndGet()), "");
    }

    public void fail(long roomId, long eventSeq, String requestId, String msgId, Throwable failure) {
        append(roomId, eventSeq, requestId, msgId, "FAILED", OperationDeadline.none(),
                Math.max(0, inFlight.decrementAndGet()), failure.getClass().getName());
    }

    public List<Entry> snapshot(long roomId) {
        ArrayDeque<Entry> timeline = entries.get(roomId);
        if (timeline == null) return List.of();
        synchronized (timeline) { return List.copyOf(timeline); }
    }

    private void append(long roomId, long eventSeq, String requestId, String msgId, String phase,
                        OperationDeadline deadline, int queueLength, String failureType) {
        Thread thread = Thread.currentThread();
        Entry entry = new Entry(roomId, eventSeq, requestId, msgId, phase,
                deadline.open() ? deadline.seatId() : -1,
                deadline.open() ? deadline.deadline().toEpochMilli() : 0,
                queueLength, thread.getName(), thread.getState(), Instant.now(), failureType);
        ArrayDeque<Entry> timeline = entries.computeIfAbsent(roomId, ignored -> new ArrayDeque<>(capacity));
        synchronized (timeline) {
            while (timeline.size() >= capacity) timeline.removeFirst();
            timeline.addLast(entry);
        }
    }
}
