package com.aoo.bcg.common.perspective;
import com.aoo.bcg.gamespi.ImmutableValue;

public record PerspectiveMessage<T>(long roomId, long sequence, String messageId,
        PerspectiveVisibility visibility, long ownerPlayerId, T payload) {
    public PerspectiveMessage {
        if (roomId <= 0 || sequence < 0 || messageId == null || messageId.isBlank()
                || visibility == null || payload == null)
            throw new IllegalArgumentException("invalid perspective message");
        if (visibility == PerspectiveVisibility.PUBLIC && ownerPlayerId != 0)
            throw new IllegalArgumentException("public message cannot have owner");
        if (visibility == PerspectiveVisibility.PLAYER_PRIVATE && ownerPlayerId <= 0)
            throw new IllegalArgumentException("private message requires owner");
        @SuppressWarnings("unchecked") T frozen = (T) ImmutableValue.freeze(payload);
        payload = frozen;
    }
    public boolean visibleTo(ViewerContext viewer) {
        if (viewer.roomId() != roomId) return false;
        return visibility == PerspectiveVisibility.PUBLIC
                || viewer.role() == ViewerRole.PLAYER && viewer.authenticatedViewerId() == ownerPlayerId;
    }
}
