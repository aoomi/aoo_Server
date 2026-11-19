package core.replay;

@FunctionalInterface
public interface ReplayParticipantAuthorizer {
    boolean mayView(long roomId, int setId, long authenticatedPlayerId);
    default boolean mayView(long roomId, int setId, long authenticatedPlayerId, int authenticatedSeatId) {
        return authenticatedSeatId >= 0 && mayView(roomId, setId, authenticatedPlayerId);
    }
}
