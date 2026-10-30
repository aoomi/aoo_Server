package com.aoo.bcg.gamespi;

/** Commit-time identity; never inferred from a current room or a response body. */
public record CommittedCommandBinding(int schemaVersion, String userId, String operation,
        long roomId, int roundNo, String requestId, String roomInstanceId,
        long lifecycleEpoch, long fencingToken, long stateVersion) {
    public CommittedCommandBinding {
        if (schemaVersion != 2 || userId == null || userId.isBlank()
                || operation == null || operation.isBlank() || roomId <= 0 || roundNo < 0
                || requestId == null || requestId.isBlank() || requestId.length() > 128
                || roomInstanceId == null || roomInstanceId.isBlank()
                || lifecycleEpoch <= 0 || fencingToken <= 0 || stateVersion < 0)
            throw new IllegalArgumentException("invalid committed command binding");
    }
    public void requireScope(String user, String op, long room, int round, String request) {
        if (!userId.equals(user) || !operation.equals(op) || roomId != room
                || roundNo != round || !requestId.equals(request))
            throw new IllegalArgumentException("committed command scope conflict");
    }
}
