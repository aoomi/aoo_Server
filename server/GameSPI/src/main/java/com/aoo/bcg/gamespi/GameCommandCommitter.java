package com.aoo.bcg.gamespi;

/** Durable boundary invoked exactly once after a successful authoritative command. */
@FunctionalInterface
public interface GameCommandCommitter {
    void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result);

    /** Resolves an acknowledgement-lost commit without executing the command again. */
    default java.util.Optional<GameCommandResult> findCommitted(GameRoomHandle room, GameCommandRequest request) {
        return java.util.Optional.empty();
    }

    static GameCommandCommitter noOp() { return new GameCommandCommitter() {
        @Override public void commit(GameRoomHandle room, GameCommandRequest request, GameCommandResult result) { }
    }; }
}
