package com.aoo.bcg.common.reconnect;
import java.util.List;
public record ReconnectResult<V, E>(V viewerSnapshot, List<E> missingEvents, long latestSequence) {
    public ReconnectResult { missingEvents = List.copyOf(missingEvents); }
}
