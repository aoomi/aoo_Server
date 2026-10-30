package core.replay;

import java.util.List;

/** Storage adapter. Production implementations must preserve sequence ordering. */
public interface PerspectiveReplayRepository {
    void append(PerspectiveReplayEvent event);

    List<PerspectiveReplayEvent> findPublic(long roomId, int setId);

    List<PerspectiveReplayEvent> findPrivate(long roomId, int setId, long playerId);
}
