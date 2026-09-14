package com.aoo.bcg.common.replay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryReplayRecorder implements ReplayRecorder {
    private final ConcurrentHashMap<Long, List<ReplayFrame>> frames = new ConcurrentHashMap<>();
    @Override public void append(long roomId, ReplayFrame frame) {
        List<ReplayFrame> roomFrames = frames.computeIfAbsent(roomId, ignored -> new ArrayList<>());
        synchronized (roomFrames) {
            if (!roomFrames.isEmpty() && frame.sequence() <= roomFrames.getLast().sequence()) throw new IllegalStateException("replay sequence must increase");
            roomFrames.add(frame);
        }
    }
    @Override public List<ReplayFrame> load(long roomId) {
        List<ReplayFrame> roomFrames = frames.get(roomId);
        if (roomFrames == null) return List.of();
        synchronized (roomFrames) { return List.copyOf(roomFrames); }
    }
}
