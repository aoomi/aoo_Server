package core.replay;

public interface ReplayParticipantRegistry extends ReplayParticipantAuthorizer {
    void grant(long roomId, int setId, long playerId, int seatId);
}
