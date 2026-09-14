package core.replay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Records and composes replay data strictly from the authenticated viewer perspective. */
public final class PerspectiveReplayService {
    private final PerspectiveReplayRepository repository;
    private final ReplayParticipantAuthorizer authorizer;

    public PerspectiveReplayService(PerspectiveReplayRepository repository,
            ReplayParticipantAuthorizer authorizer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
    }

    public void appendPublic(long roomId, int setId, long sequence,
            String messageId, byte[] payload) {
        repository.append(new PerspectiveReplayEvent(roomId, setId, sequence,
                ReplayEventVisibility.PUBLIC, 0, messageId, payload));
    }

    public void appendPrivate(long roomId, int setId, long sequence, long ownerPlayerId,
            String messageId, byte[] payload) {
        repository.append(new PerspectiveReplayEvent(roomId, setId, sequence,
                ReplayEventVisibility.PLAYER_PRIVATE, ownerPlayerId, messageId, payload));
    }

    public List<PerspectiveReplayEvent> loadPlayerView(long roomId, int setId,
            long authenticatedPlayerId) {
        if (authenticatedPlayerId <= 0) {
            throw new SecurityException("Authenticated player is required");
        }
        if (!authorizer.mayView(roomId, setId, authenticatedPlayerId)) {
            throw new SecurityException("Player is not a replay participant");
        }
        List<PerspectiveReplayEvent> result = new ArrayList<>();
        result.addAll(repository.findPublic(roomId, setId));
        result.addAll(repository.findPrivate(roomId, setId, authenticatedPlayerId));
        result.sort(Comparator.comparingLong(PerspectiveReplayEvent::getSequence));
        return List.copyOf(result);
    }

    public List<PerspectiveReplayEvent> loadPlayerView(int setId,
            com.aoo.bcg.gamespi.AuthenticatedViewerScope viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!authorizer.mayView(viewer.roomId(), setId, viewer.playerId(), viewer.seatId())) {
            throw new SecurityException("Authenticated player-seat is not a replay participant");
        }
        List<PerspectiveReplayEvent> result = new ArrayList<>();
        result.addAll(repository.findPublic(viewer.roomId(), setId));
        result.addAll(repository.findPrivate(viewer.roomId(), setId, viewer.playerId()));
        result.sort(Comparator.comparingLong(PerspectiveReplayEvent::getSequence));
        return List.copyOf(result);
    }
}
