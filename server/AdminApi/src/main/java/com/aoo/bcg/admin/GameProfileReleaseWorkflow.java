package com.aoo.bcg.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Draft -> validation -> four-eyes approval -> canary -> active -> rollback workflow. */
public final class GameProfileReleaseWorkflow {
    public enum State { DRAFT, VALIDATED, PENDING_APPROVAL, APPROVED, CANARY, ACTIVE, ROLLED_BACK, REJECTED }
    public record Release(String releaseId, long gameId, String version, State state,
            int rolloutPercentage, Map<String, Object> profile, long createdBy,
            Long approvedBy, Instant updatedAt, String previousVersion) {
        public Release {
            if (blank(releaseId) || releaseId.length() > 128 || !releaseId.matches("[A-Za-z0-9._-]+")
                    || gameId <= 0 || blank(version) || version.length() > 64 || state == null
                    || rolloutPercentage < 0 || rolloutPercentage > 100 || profile == null
                    || createdBy <= 0 || updatedAt == null || previousVersion != null && previousVersion.length() > 64)
                throw new IllegalArgumentException("invalid game-profile release");
            profile = freezeMap(profile);
        }
    }
    public record Event(String requestId, String releaseId, State before, State after,
            long operatorId, String reason, Instant occurredAt) {
        public Event {
            if (blank(requestId) || blank(releaseId) || after == null || operatorId <= 0
                    || blank(reason) || reason.length() > 500 || occurredAt == null)
                throw new IllegalArgumentException("complete release event required");
        }
    }
    @FunctionalInterface public interface ProfileValidator { List<String> validate(long gameId, Map<String, Object> profile); }
    public interface Publisher {
        void activate(Release release, String requestId, long operatorId, String reason);
        void rollback(long gameId, String previousVersion, String requestId, long operatorId, String reason);
    }
    public interface Store {
        Optional<Release> findRelease(String releaseId);
        Optional<Release> findRequest(String requestId);
        void save(String requestId, Release before, Release after, Event event);
        List<Event> history();
    }

    private final Store store;
    private final ProfileValidator validator;
    private final Publisher publisher;
    private final Clock clock;

    public GameProfileReleaseWorkflow(ProfileValidator validator, Publisher publisher, Clock clock) {
        this(validator, publisher, clock, new InMemoryStore());
    }

    public GameProfileReleaseWorkflow(ProfileValidator validator, Publisher publisher, Clock clock, Store store) {
        this.validator = Objects.requireNonNull(validator);
        this.publisher = Objects.requireNonNull(publisher);
        this.clock = Objects.requireNonNull(clock);
        this.store = Objects.requireNonNull(store);
    }

    public synchronized Release draft(String requestId, String releaseId, long gameId, String version,
            Map<String, Object> profile, String previousVersion, long operatorId, String reason) {
        Release repeated = repeated(requestId, releaseId);
        if (repeated != null) return repeated;
        requireText(reason, "reason");
        if (store.findRelease(releaseId).isPresent()) throw new IllegalArgumentException("release already exists");
        Release release = new Release(releaseId, gameId, version, State.DRAFT, 0,
                deepCopy(profile), operatorId, null, clock.instant(), previousVersion);
        persist(requestId, null, release, operatorId, reason);
        return release;
    }

    public synchronized Release validate(String requestId, String releaseId, long operatorId, String reason) {
        Release current = requireState(requestId, releaseId, State.DRAFT);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        List<String> errors = List.copyOf(validator.validate(current.gameId(), current.profile()));
        if (!errors.isEmpty()) throw new IllegalArgumentException("profile validation failed: " + errors);
        return transition(requestId, current, State.VALIDATED, 0, null, operatorId, reason);
    }

    public synchronized Release submit(String requestId, String releaseId, long operatorId, String reason) {
        Release current = requireState(requestId, releaseId, State.VALIDATED);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        return transition(requestId, current, State.PENDING_APPROVAL, 0, null, operatorId, reason);
    }

    public synchronized Release approve(String requestId, String releaseId, long approverId, String reason) {
        Release current = requireState(requestId, releaseId, State.PENDING_APPROVAL);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        if (current.createdBy() == approverId) throw new IllegalArgumentException("creator cannot approve own release");
        return transition(requestId, current, State.APPROVED, 0, approverId, approverId, reason);
    }

    public synchronized Release deployCanary(String requestId, String releaseId, int percentage,
            long operatorId, String reason) {
        Release current = requireState(requestId, releaseId, State.APPROVED);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        if (percentage < 1 || percentage > 99) throw new IllegalArgumentException("canary percentage must be 1..99");
        return transition(requestId, current, State.CANARY, percentage, current.approvedBy(), operatorId, reason);
    }

    public synchronized Release activate(String requestId, String releaseId, long operatorId, String reason) {
        Release current = requireState(requestId, releaseId, State.CANARY);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        Release active = copy(current, State.ACTIVE, 100, current.approvedBy());
        publisher.activate(active, requestId, operatorId, requireText(reason, "reason"));
        persist(requestId, current, active, operatorId, reason);
        return active;
    }

    public synchronized Release rollback(String requestId, String releaseId, long operatorId, String reason) {
        Release current = requireState(requestId, releaseId, State.ACTIVE, State.CANARY);
        if (store.findRequest(requestId).isPresent()) return store.findRequest(requestId).orElseThrow();
        if (blank(current.previousVersion())) throw new IllegalArgumentException("previous version required for rollback");
        publisher.rollback(current.gameId(), current.previousVersion(), requestId, operatorId,
                requireText(reason, "reason"));
        return transition(requestId, current, State.ROLLED_BACK, 0, current.approvedBy(), operatorId, reason);
    }

    public Optional<Release> find(String releaseId) { return store.findRelease(releaseId); }
    public synchronized List<Event> history() { return store.history(); }

    private Release transition(String requestId, Release current, State state, int percentage,
            Long approvedBy, long operatorId, String reason) {
        Release next = copy(current, state, percentage, approvedBy);
        persist(requestId, current, next, operatorId, requireText(reason, "reason"));
        return next;
    }

    private Release copy(Release current, State state, int percentage, Long approvedBy) {
        return new Release(current.releaseId(), current.gameId(), current.version(), state, percentage,
                current.profile(), current.createdBy(), approvedBy, clock.instant(), current.previousVersion());
    }

    private void persist(String requestId, Release before, Release after, long operatorId, String reason) {
        requireRequestId(requestId);
        Event event = new Event(requestId, after.releaseId(), before == null ? null : before.state(),
                after.state(), operatorId, reason, clock.instant());
        store.save(requestId, before, after, event);
    }

    private Release repeated(String requestId, String releaseId) {
        requireRequestId(requestId);
        Release repeated = store.findRequest(requestId).orElse(null);
        if (repeated != null && !repeated.releaseId().equals(releaseId))
            throw new IllegalArgumentException("requestId already used by another release");
        return repeated;
    }

    private Release requireState(String requestId, String releaseId, State... allowed) {
        Release repeated = repeated(requestId, releaseId);
        if (repeated != null) return repeated;
        Release current = store.findRelease(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("release not found"));
        if (java.util.Arrays.stream(allowed).noneMatch(state -> state == current.state()))
            throw new IllegalStateException("release state does not permit this transition");
        return current;
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) throw new IllegalArgumentException("profile required");
        return new LinkedHashMap<>(source);
    }
    private static String requireText(String value, String field) {
        if (blank(value) || value.length() > 500) throw new IllegalArgumentException(field + " required");
        return value;
    }
    private static void requireRequestId(String value) {
        if (blank(value) || value.length() > 128) throw new IllegalArgumentException("requestId required");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, freeze(value)));
        return java.util.Collections.unmodifiableMap(result);
    }
    private static Object freeze(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), freeze(item)));
            return java.util.Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list)
            return java.util.Collections.unmodifiableList(list.stream().map(GameProfileReleaseWorkflow::freeze).toList());
        if (value instanceof java.util.Set<?> set)
            return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(set.stream()
                    .map(GameProfileReleaseWorkflow::freeze).toList()));
        return value;
    }

    public static final class InMemoryStore implements Store {
        private final Map<String, Release> releases = new ConcurrentHashMap<>();
        private final Map<String, Release> requests = new ConcurrentHashMap<>();
        private final List<Event> events = new ArrayList<>();
        @Override public Optional<Release> findRelease(String releaseId) {
            return Optional.ofNullable(releases.get(releaseId));
        }
        @Override public Optional<Release> findRequest(String requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }
        @Override public synchronized void save(String requestId, Release before, Release after, Event event) {
            Release repeated = requests.putIfAbsent(requestId, after);
            if (repeated != null) return;
            Release current = releases.get(after.releaseId());
            if (!Objects.equals(current, before)) {
                requests.remove(requestId, after);
                throw new IllegalStateException("release state changed concurrently");
            }
            releases.put(after.releaseId(), after);
            events.add(event);
        }
        @Override public synchronized List<Event> history() { return List.copyOf(events); }
    }
}
