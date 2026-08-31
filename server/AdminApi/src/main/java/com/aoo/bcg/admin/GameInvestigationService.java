package com.aoo.bcg.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only evidence assembler. There is deliberately no game-control command port. */
public final class GameInvestigationService {
    public record Query(long roomId, long fromSequence, long toSequence, int replaySetId) {
        public Query {
            if (roomId <= 0 || fromSequence < 0 || toSequence < fromSequence
                    || toSequence - fromSequence > 10_000 || replaySetId < 0)
                throw new IllegalArgumentException("bounded investigation query required");
        }
    }
    public record Snapshot(long roomId, long gameId, String playVersion, long fencingToken,
            long lastEventSequence, Instant capturedAt, Map<String, Object> state) { }
    public record Event(long sequence, String type, Map<String, Object> payload, Instant occurredAt) { }
    public record ReplayFrame(long sequence, String visibility, long ownerPlayerId,
            String messageId, int schemaVersion, String playVersion, byte[] payload) {
        public ReplayFrame { payload = payload.clone(); }
        @Override public byte[] payload() { return payload.clone(); }
    }
    public record Evidence(Query query, Snapshot snapshot, List<Event> events,
            List<ReplayFrame> replay, long investigatorId, String reason,
            Instant assembledAt, String evidenceHash) { }
    public interface ReadOnlyRepository {
        Snapshot snapshot(long roomId);
        List<Event> events(long roomId, long fromSequence, long toSequence);
        List<ReplayFrame> replay(long roomId, int setId);
    }

    private final ReadOnlyRepository repository;
    private final Clock clock;

    public GameInvestigationService(ReadOnlyRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Evidence investigate(Query query, long investigatorId, String reason) {
        Objects.requireNonNull(query);
        if (investigatorId <= 0 || reason == null || reason.isBlank())
            throw new IllegalArgumentException("investigator and reason required");
        Snapshot snapshot = repository.snapshot(query.roomId());
        List<Event> events = List.copyOf(repository.events(query.roomId(), query.fromSequence(), query.toSequence()));
        List<ReplayFrame> replay = List.copyOf(repository.replay(query.roomId(), query.replaySetId()));
        if (events.stream().anyMatch(event -> event.sequence() < query.fromSequence()
                || event.sequence() > query.toSequence()))
            throw new IllegalStateException("repository returned out-of-range evidence");
        Instant now = clock.instant();
        return new Evidence(query, snapshot, events, replay, investigatorId, reason, now,
                hash(query, snapshot, events, replay, investigatorId, reason, now));
    }

    private String hash(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) update(digest, value);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void update(MessageDigest digest, Object value) {
        if (value == null) { digest.update((byte) 0); return; }
        if (value instanceof byte[] bytes) { digest.update(bytes); digest.update((byte) 0); return; }
        if (value instanceof Map<?, ?> map) {
            map.entrySet().stream().sorted(java.util.Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> { update(digest, String.valueOf(entry.getKey())); update(digest, entry.getValue()); });
            digest.update((byte) 0); return;
        }
        if (value instanceof Iterable<?> items) {
            for (Object item : items) update(digest, item);
            digest.update((byte) 0); return;
        }
        if (value instanceof Query query) {
            update(digest, query.roomId()); update(digest, query.fromSequence());
            update(digest, query.toSequence()); update(digest, query.replaySetId()); return;
        }
        if (value instanceof Snapshot snapshot) {
            update(digest, snapshot.roomId()); update(digest, snapshot.gameId()); update(digest, snapshot.playVersion());
            update(digest, snapshot.fencingToken()); update(digest, snapshot.lastEventSequence());
            update(digest, snapshot.capturedAt()); update(digest, snapshot.state()); return;
        }
        if (value instanceof Event event) {
            update(digest, event.sequence()); update(digest, event.type());
            update(digest, event.payload()); update(digest, event.occurredAt()); return;
        }
        if (value instanceof ReplayFrame frame) {
            update(digest, frame.sequence()); update(digest, frame.visibility()); update(digest, frame.ownerPlayerId());
            update(digest, frame.messageId()); update(digest, frame.schemaVersion()); update(digest, frame.playVersion());
            update(digest, frame.payload()); return;
        }
        digest.update(String.valueOf(value).getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
    }
}
