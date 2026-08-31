package com.aoo.bcg.common.transaction;

import java.util.Objects;

/** Durable state machine for game-state/accounting operations with idempotent compensation. */
public final class RoomAccountingSaga {
    private static final int MAX_TRANSITION_ATTEMPTS = 16;
    @FunctionalInterface public interface Step { void run() throws Exception; }
    public static final class ExecutionFailure extends RuntimeException {
        private final RoomAccountingSagaState state;
        private ExecutionFailure(RoomAccountingSagaState state, Throwable cause) {
            super("room accounting saga stopped in " + state, cause);
            this.state = state;
        }
        public RoomAccountingSagaState state() { return state; }
    }

    private final RoomAccountingSagaRepository repository;

    public RoomAccountingSaga(RoomAccountingSagaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public RoomAccountingSagaState execute(RoomAccountingSagaId id, Step applyGame,
                                           Step applyAccounting, Step finalizeGame,
                                           Step compensateGame) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(applyGame, "applyGame");
        Objects.requireNonNull(applyAccounting, "applyAccounting");
        Objects.requireNonNull(finalizeGame, "finalizeGame");
        Objects.requireNonNull(compensateGame, "compensateGame");
        repository.create(id);
        for (int attempt = 1; attempt <= MAX_TRANSITION_ATTEMPTS; attempt++) {
            RoomAccountingSagaState state = repository.find(id).orElseThrow();
            switch (state) {
                case NEW -> {
                    if (!repository.compareAndSet(id, state, RoomAccountingSagaState.GAME_APPLYING)) continue;
                    runOrRestore(id, applyGame, RoomAccountingSagaState.GAME_APPLYING,
                            RoomAccountingSagaState.NEW);
                    transition(id, RoomAccountingSagaState.GAME_APPLYING, RoomAccountingSagaState.GAME_APPLIED);
                }
                case GAME_APPLIED -> {
                    if (!repository.compareAndSet(id, state, RoomAccountingSagaState.ACCOUNTING_APPLYING)) continue;
                    try {
                        applyAccounting.run();
                        transition(id, RoomAccountingSagaState.ACCOUNTING_APPLYING,
                                RoomAccountingSagaState.ACCOUNTING_APPLIED);
                    } catch (Exception accountingFailure) {
                        transition(id, RoomAccountingSagaState.ACCOUNTING_APPLYING,
                                RoomAccountingSagaState.COMPENSATING);
                        try {
                            compensateGame.run();
                            transition(id, RoomAccountingSagaState.COMPENSATING,
                                    RoomAccountingSagaState.COMPENSATED);
                            throw new ExecutionFailure(RoomAccountingSagaState.COMPENSATED, accountingFailure);
                        } catch (ExecutionFailure known) {
                            throw known;
                        } catch (Exception compensationFailure) {
                            transition(id, RoomAccountingSagaState.COMPENSATING,
                                    RoomAccountingSagaState.MANUAL_RECOVERY);
                            compensationFailure.addSuppressed(accountingFailure);
                            throw new ExecutionFailure(RoomAccountingSagaState.MANUAL_RECOVERY,
                                    compensationFailure);
                        }
                    }
                }
                case ACCOUNTING_APPLIED -> {
                    if (!repository.compareAndSet(id, state, RoomAccountingSagaState.FINALIZING)) continue;
                    runOrRestore(id, finalizeGame, RoomAccountingSagaState.FINALIZING,
                            RoomAccountingSagaState.ACCOUNTING_APPLIED);
                    transition(id, RoomAccountingSagaState.FINALIZING, RoomAccountingSagaState.COMPLETED);
                }
                case COMPLETED, COMPENSATED -> { return state; }
                case GAME_APPLYING, ACCOUNTING_APPLYING, FINALIZING, COMPENSATING ->
                        throw new IllegalStateException("room accounting saga is already in progress: " + state);
                case MANUAL_RECOVERY -> throw new IllegalStateException("room accounting saga requires manual recovery");
            }
        }
        throw new IllegalStateException("room accounting saga transition contention exceeded "
                + MAX_TRANSITION_ATTEMPTS + " attempts");
    }

    private void runOrRestore(RoomAccountingSagaId id, Step step, RoomAccountingSagaState applying,
                              RoomAccountingSagaState restored) {
        try { step.run(); }
        catch (Exception failure) {
            transition(id, applying, restored);
            throw new ExecutionFailure(restored, failure);
        }
    }

    private void transition(RoomAccountingSagaId id, RoomAccountingSagaState expected,
                            RoomAccountingSagaState next) {
        if (!repository.compareAndSet(id, expected, next))
            throw new IllegalStateException("room accounting saga state changed concurrently");
    }
}
