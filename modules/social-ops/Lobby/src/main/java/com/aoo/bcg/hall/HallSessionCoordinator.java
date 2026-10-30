package com.aoo.bcg.hall;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Orders asynchronous login/bootstrap work and isolates all hall state by account generation. */
public final class HallSessionCoordinator {
    private final AtomicLong generation = new AtomicLong();
    private final Map<Long, AccountState> states = new HashMap<>();
    private long activeAccount;

    public synchronized LoginAttempt beginLogin(long accountId) { return new LoginAttempt(accountId, generation.incrementAndGet()); }
    public synchronized boolean accept(LoginAttempt attempt) {
        if (attempt.generation != generation.get()) return false;
        activeAccount = attempt.accountId; states.computeIfAbsent(activeAccount, ignored -> new AccountState()); return true;
    }
    /** Invalidates late callbacks and removes all sensitive in-memory state for the active account. */
    public synchronized void logout() {
        generation.incrementAndGet();
        if (activeAccount > 0) states.remove(activeAccount);
        activeAccount = 0;
    }
    public synchronized void put(String key, String value) { if (activeAccount <= 0) throw new IllegalStateException("not logged in"); states.get(activeAccount).values.put(key, value); }
    public synchronized Optional<String> get(String key) { return activeAccount <= 0 ? Optional.empty() : Optional.ofNullable(states.get(activeAccount).values.get(key)); }
    public synchronized StartupPhase next(StartupPhase phase) { return switch (phase) { case CONFIG -> StartupPhase.REFRESH_TOKEN; case REFRESH_TOKEN -> StartupPhase.RESTORE_ROOM; case RESTORE_ROOM -> StartupPhase.LOAD_HALL; case LOAD_HALL -> StartupPhase.READY; case READY -> throw new IllegalStateException("already ready"); }; }
    private static final class AccountState { final Map<String,String> values = new HashMap<>(); }
    public record LoginAttempt(long accountId,long generation) { public LoginAttempt { if(accountId<=0) throw new IllegalArgumentException("invalid account"); } }
    public enum StartupPhase { CONFIG, REFRESH_TOKEN, RESTORE_ROOM, LOAD_HALL, READY }
}
