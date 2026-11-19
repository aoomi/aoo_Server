package com.aoo.bcg.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Lease/fencing, shard, retry, idempotency and approved manual-rerun coordinator. */
public final class AdminJobCoordinator {
    public enum State { READY, RUNNING, RETRY_WAIT, SUCCEEDED, DEAD }
    public record JobKey(String jobCode, String businessKey, int shard, int shardCount) {
        public JobKey {
            if (blank(jobCode) || jobCode.length() > 48 || !jobCode.matches("[A-Za-z0-9._-]+")
                    || blank(businessKey) || businessKey.length() > 64
                    || !businessKey.matches("[A-Za-z0-9._:-]+")
                    || shardCount < 1 || shardCount > 4096 || shard < 0 || shard >= shardCount)
                throw new IllegalArgumentException("valid sharded job key required");
        }
        public String id() { return jobCode + ":" + businessKey + ":" + shard + "/" + shardCount; }
    }
    public record Run(String runId, JobKey key, State state, int attempt, int maxAttempts,
            String leaseOwner, long fencingToken, Instant leaseUntil, Instant nextAttemptAt,
            String result, String error, String rerunOf, long requestedBy, Long approvedBy,
            String reason, Instant updatedAt) {
        public Run {
            if (blank(runId) || runId.length() > 128 || !runId.matches("[A-Za-z0-9._-]+")
                    || key == null || state == null || attempt < 0 || maxAttempts < 1
                    || fencingToken < 0 || requestedBy <= 0 || blank(reason) || updatedAt == null)
                throw new IllegalArgumentException("valid job run required");
        }
    }
    public record Lease(String runId, String owner, long fencingToken, Instant expiresAt) { }
    public record Outcome(boolean success, String result, String error) {
        public static Outcome success(String result) { return new Outcome(true, result, ""); }
        public static Outcome failure(String error) { return new Outcome(false, "", error); }
    }
    @FunctionalInterface public interface Handler { Outcome execute(JobKey key, long fencingToken) throws Exception; }
    public interface Store {
        void create(Run run);
        Optional<Run> find(String runId);
        Optional<Run> findSucceeded(JobKey key);
        Optional<Lease> acquire(String runId, String owner, Instant now, Instant leaseUntil);
        boolean saveFenced(Run run, String owner, long fencingToken);
        List<Run> all();
    }

    private final Store store;
    private final Clock clock;
    private final Duration leaseDuration;
    private final Duration initialBackoff;

    public AdminJobCoordinator(Store store, Clock clock, Duration leaseDuration, Duration initialBackoff) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        if (leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()
                || initialBackoff == null || initialBackoff.isNegative() || initialBackoff.isZero()
                || initialBackoff.compareTo(Duration.ofDays(1)) > 0)
            throw new IllegalArgumentException("positive lease and backoff required");
        this.leaseDuration = leaseDuration;
        this.initialBackoff = initialBackoff;
    }

    public Run schedule(String runId, JobKey key, int maxAttempts, long requestedBy, String reason) {
        if (maxAttempts < 1 || maxAttempts > 20) throw new IllegalArgumentException("maxAttempts must be 1..20");
        Run run = new Run(runId, key, State.READY, 0, maxAttempts, "", 0, Instant.EPOCH,
                clock.instant(), "", "", "", requestedBy, null, requireReason(reason), clock.instant());
        store.create(run);
        return run;
    }

    public Run execute(String runId, String workerId, Handler handler) {
        if (blank(workerId)) throw new IllegalArgumentException("workerId required");
        Run current = store.find(runId).orElseThrow(() -> new IllegalArgumentException("job run not found"));
        if (current.state() == State.SUCCEEDED || current.state() == State.DEAD) return current;
        Optional<Run> succeeded = store.findSucceeded(current.key());
        if (blank(current.rerunOf()) && succeeded.isPresent() && !succeeded.get().runId().equals(runId))
            return succeeded.get();
        Instant now = clock.instant();
        if (current.nextAttemptAt().isAfter(now)) throw new IllegalStateException("job retry is not due");
        Lease lease = store.acquire(runId, workerId, now, now.plus(leaseDuration))
                .orElseThrow(() -> new IllegalStateException("job lease is held by another worker"));
        Run running = new Run(current.runId(), current.key(), State.RUNNING, current.attempt() + 1,
                current.maxAttempts(), workerId, lease.fencingToken(), lease.expiresAt(), current.nextAttemptAt(),
                current.result(), current.error(), current.rerunOf(), current.requestedBy(), current.approvedBy(),
                current.reason(), now);
        if (!store.saveFenced(running, workerId, lease.fencingToken()))
            throw new IllegalStateException("stale fencing token before execution");
        Outcome outcome;
        try { outcome = Objects.requireNonNull(handler.execute(current.key(), lease.fencingToken())); }
        catch (Exception error) { outcome = Outcome.failure(error.getClass().getSimpleName() + ":" + error.getMessage()); }
        Instant finished = clock.instant();
        State state = outcome.success() ? State.SUCCEEDED
                : running.attempt() >= running.maxAttempts() ? State.DEAD : State.RETRY_WAIT;
        Duration delay = initialBackoff.multipliedBy(1L << Math.min(20, running.attempt() - 1));
        Run completed = new Run(running.runId(), running.key(), state, running.attempt(), running.maxAttempts(),
                workerId, lease.fencingToken(), lease.expiresAt(),
                outcome.success() ? finished : finished.plus(delay), outcome.result(), outcome.error(),
                running.rerunOf(), running.requestedBy(), running.approvedBy(), running.reason(), finished);
        if (!store.saveFenced(completed, workerId, lease.fencingToken()))
            throw new IllegalStateException("stale fencing token after execution");
        return completed;
    }

    public Run approvedManualRerun(String newRunId, String originalRunId, long requesterId,
            long approverId, String reason) {
        if (requesterId <= 0 || approverId <= 0 || requesterId == approverId)
            throw new IllegalArgumentException("independent rerun approver required");
        Run original = store.find(originalRunId).orElseThrow(() -> new IllegalArgumentException("original run not found"));
        if (original.state() != State.DEAD && original.state() != State.SUCCEEDED)
            throw new IllegalStateException("only terminal jobs may be manually rerun");
        Run rerun = new Run(newRunId, original.key(), State.READY, 0, original.maxAttempts(), "", 0,
                Instant.EPOCH, clock.instant(), "", "", originalRunId, requesterId, approverId,
                requireReason(reason), clock.instant());
        store.create(rerun);
        return rerun;
    }

    public List<Run> runs() { return store.all(); }
    private static String requireReason(String reason) {
        if (blank(reason) || reason.length() > 500) throw new IllegalArgumentException("reason required");
        return reason;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    public static final class InMemoryStore implements Store {
        private final Map<String, Run> runs = new ConcurrentHashMap<>();
        @Override public void create(Run run) {
            if (runs.putIfAbsent(run.runId(), run) != null) throw new IllegalArgumentException("run already exists");
        }
        @Override public Optional<Run> find(String runId) { return Optional.ofNullable(runs.get(runId)); }
        @Override public Optional<Run> findSucceeded(JobKey key) {
            return runs.values().stream().filter(run -> run.key().equals(key) && run.state() == State.SUCCEEDED).findFirst();
        }
        @Override public synchronized Optional<Lease> acquire(String runId, String owner, Instant now, Instant until) {
            Run current = runs.get(runId);
            if (current == null) return Optional.empty();
            if (current.state() == State.RUNNING && current.leaseUntil().isAfter(now)
                    && !current.leaseOwner().equals(owner)) return Optional.empty();
            long token = current.fencingToken() + 1;
            Run leased = new Run(current.runId(), current.key(), current.state(), current.attempt(),
                    current.maxAttempts(), owner, token, until, current.nextAttemptAt(), current.result(),
                    current.error(), current.rerunOf(), current.requestedBy(), current.approvedBy(),
                    current.reason(), now);
            runs.put(runId, leased);
            return Optional.of(new Lease(runId, owner, token, until));
        }
        @Override public synchronized boolean saveFenced(Run run, String owner, long token) {
            Run current = runs.get(run.runId());
            if (current == null || current.fencingToken() != token || !current.leaseOwner().equals(owner)) return false;
            runs.put(run.runId(), run);
            return true;
        }
        @Override public List<Run> all() { return List.copyOf(new ArrayList<>(runs.values())); }
    }
}
