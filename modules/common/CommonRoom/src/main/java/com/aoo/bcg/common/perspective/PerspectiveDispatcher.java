package com.aoo.bcg.common.perspective;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/** The only supported fan-out path for realtime, reconnect, replay and spectator output. */
public final class PerspectiveDispatcher {
    private PerspectiveDispatcher() {}
    public static <T> void dispatch(PerspectiveMessage<T> message, Collection<ViewerContext> viewers,
            BiConsumer<ViewerContext, T> sink) {
        Objects.requireNonNull(message); Objects.requireNonNull(viewers); Objects.requireNonNull(sink);
        viewers.stream().filter(message::visibleTo).forEach(viewer -> sink.accept(viewer, message.payload()));
    }
}
