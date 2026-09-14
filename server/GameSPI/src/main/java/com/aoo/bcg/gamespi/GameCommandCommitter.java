package com.aoo.bcg.gamespi;

/** Durable boundary invoked exactly once after a successful authoritative command. */
@FunctionalInterface
public interface GameCommandCommitter {
    void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result);

    /**
     * Commits and returns the exact durable response. Persistence adapters may attach values that
     * are only known while closing the transaction (for example a round replay code).
     */
    default GameCommandResult commitResult(GameRoomHandle room, GameCommandRequest request,
                                           GameCommandResult result) {
        commit(room, request, result);
        return result;
    }

    /** Resolves an acknowledgement-lost commit without executing the command again. */
    default java.util.Optional<GameCommandResult> findCommitted(GameRoomHandle room, GameCommandRequest request) {
        return java.util.Optional.empty();
    }

    static GameCommandCommitter noOp() { return new GameCommandCommitter() {
        @Override public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) { }
    }; }
}
