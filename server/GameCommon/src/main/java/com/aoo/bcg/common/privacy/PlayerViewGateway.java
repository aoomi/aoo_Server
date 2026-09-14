package com.aoo.bcg.common.privacy;

import com.aoo.bcg.common.reconnect.PerspectiveViewBuilder;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class PlayerViewGateway<S, V> {
    private final PerspectiveViewBuilder<S, V> projector;
    private final BiConsumer<Long, V> sender;
    public PlayerViewGateway(PerspectiveViewBuilder<S, V> projector, BiConsumer<Long, V> sender) {
        this.projector = Objects.requireNonNull(projector); this.sender = Objects.requireNonNull(sender);
    }
    public void send(long authenticatedViewerId, S authoritativeState) {
        sender.accept(authenticatedViewerId, Objects.requireNonNull(projector.build(authoritativeState, authenticatedViewerId)));
    }
    public void broadcast(Iterable<Long> authenticatedViewerIds, S authoritativeState) {
        for (Long viewerId : authenticatedViewerIds) send(viewerId, authoritativeState);
    }
}
