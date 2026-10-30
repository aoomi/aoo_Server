package com.aoo.bcg.telemetry;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

/** Counts retained final creation receipts, never Saga timestamps or active rooms. */
public final class JdbcRoomCreationStatistics {
    public static final int MAX_DAYS = 31;
    public static final int MAX_ROWS = 10_000;
    public static final int QUERY_TIMEOUT_SECONDS = 3;
    private static final String SQL = "SELECT r.event_id,r.source_request_id,r.request_hash,r.creator_id,"
            + "r.room_id,r.game_id,r.play_version,r.occurred_at,r.schema_version,"
            + "s.request_id,s.request_hash,s.account_id,s.room_id,s.game_id,s.play_version,s.step "
            + "FROM aoo_room_create_confirmed_receipt r LEFT JOIN aoo_room_create_saga s "
            + "ON s.request_id=r.source_request_id WHERE r.occurred_at>=? AND r.occurred_at<? "
            + "ORDER BY r.occurred_at,r.event_id LIMIT ?";
    private final DataSource source;
    private final Clock clock;
    private final int rowLimit;
    private final Object issuer = new Object();
    private final String salt = UUID.randomUUID().toString();

    public JdbcRoomCreationStatistics(DataSource source, Clock clock) { this(source, clock, MAX_ROWS); }
    public JdbcRoomCreationStatistics(DataSource source, Clock clock, int rowLimit) {
        this.source = Objects.requireNonNull(source);
        this.clock = Objects.requireNonNull(clock);
        if (rowLimit < 1 || rowLimit > MAX_ROWS) throw new IllegalArgumentException("invalid row budget");
        this.rowLimit = rowLimit;
    }

    public record Window(LocalDate startInclusive, LocalDate endExclusive, ZoneId reportingZone) {
        public Window {
            if (startInclusive == null || endExclusive == null || reportingZone == null)
                throw new IllegalArgumentException("range and explicit reporting zone required");
            long days = ChronoUnit.DAYS.between(startInclusive, endExclusive);
            if (days < 1 || days > MAX_DAYS) throw new IllegalArgumentException("range must cover 1..31 days");
        }
    }

    public static final class Snapshot {
        private final Object issuer;
        private final String scope;
        private final Window window;
        private final Instant observedAt;
        private final Map<LocalDate, Long> unknownByDay;
        private Snapshot(Object issuer, String scope, Window window, Instant observedAt, Map<LocalDate, Long> counts) {
            this.issuer = issuer; this.scope = scope; this.window = window; this.observedAt = observedAt;
            this.unknownByDay = Collections.unmodifiableMap(new TreeMap<>(counts));
        }
        public String factKind() { return "ROOM_CREATED"; }
        public String sourceDefinition() { return "CURRENT_RETAINED_FINAL_CONFIRMED_RECEIPTS"; }
        public String timeDefinition() { return "DATABASE_UTC_MILLISECOND_OCCURRENCE_WITHIN_FINAL_CONFIRMATION_TRANSACTION"; }
        public String classification() { return "UNKNOWN"; }
        public int schemaVersion() { return 1; }
        public Window window() { return window; }
        public Instant observedAt() { return observedAt; }
        public Map<LocalDate, Long> unknownByDay() { return unknownByDay; }
        public long totalUnknown() { return unknownByDay.values().stream().mapToLong(Long::longValue).sum(); }
        @Override public String toString() { return sourceDefinition() + " " + window + " UNKNOWN=" + unknownByDay; }
    }

    public Snapshot snapshot(LocalDate startInclusive, LocalDate endExclusive, ZoneId reportingZone) {
        Window window = new Window(startInclusive, endExclusive, reportingZone);
        Instant start = startInclusive.atStartOfDay(reportingZone).toInstant();
        Instant end = endExclusive.atStartOfDay(reportingZone).toInstant();
        Map<LocalDate, Long> counts = new TreeMap<>();
        startInclusive.datesUntil(endExclusive).forEach(day -> counts.put(day, 0L));
        Set<String> events = new HashSet<>(), requests = new HashSet<>();
        Set<Long> rooms = new HashSet<>();
        try (Connection connection = source.getConnection()) {
            boolean readOnly = connection.isReadOnly();
            try {
                connection.setReadOnly(true);
                String url = connection.getMetaData().getURL();
                if (url == null || url.isBlank()) throw new SQLException("source identity unavailable");
                String scope = digest(salt + '\0' + url + '\0' + connection.getCatalog() + '\0' + connection.getSchema());
                try (var query = connection.prepareStatement(SQL)) {
                    query.setTimestamp(1, Timestamp.from(start), utc());
                    query.setTimestamp(2, Timestamp.from(end), utc());
                    query.setInt(3, rowLimit + 1);
                    query.setMaxRows(rowLimit + 1);
                    query.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    try (var rows = query.executeQuery()) {
                        int scanned = 0;
                        while (rows.next()) {
                            if (++scanned > rowLimit) throw new QueryBudgetExceeded();
                            String event = text(rows.getString(1), 64);
                            String request = text(rows.getString(2), 128);
                            String requestHash = text(rows.getString(3), 128);
                            long creator = positive(rows, 4), room = positive(rows, 5), game = positive(rows, 6);
                            String play = text(rows.getString(7), 64);
                            Timestamp timestamp = rows.getTimestamp(8, utc());
                            int schema = rows.getInt(9);
                            if (rows.wasNull() || schema != 1 || timestamp == null
                                    || !event.matches("[0-9a-f]{64}")
                                    || !event.equals(digest("ROOM_CREATED_CONFIRMED:v1:" + request))
                                    || !request.equals(rows.getString(10)) || !requestHash.equals(rows.getString(11))
                                    || creator != positive(rows, 12) || room != positive(rows, 13)
                                    || game != positive(rows, 14) || !play.equals(rows.getString(15))
                                    || !"CONFIRMED".equals(rows.getString(16)))
                                throw new SQLException("final creation receipt identity mismatch");
                            Instant occurredAt = timestamp.toInstant();
                            if (occurredAt.isBefore(start) || !occurredAt.isBefore(end)
                                    || occurredAt.getNano() % 1_000_000 != 0)
                                throw new SQLException("final creation receipt time mismatch");
                            if (!events.add(event) || !requests.add(request) || !rooms.add(room))
                                throw new SQLException("duplicate final creation identity");
                            LocalDate day = occurredAt.atZone(reportingZone).toLocalDate();
                            if (!counts.containsKey(day)) throw new SQLException("JDBC reporting zone mismatch");
                            counts.put(day, Math.addExact(counts.get(day), 1L));
                        }
                    }
                }
                return new Snapshot(issuer, scope, window, clock.instant(), counts);
            } finally {
                connection.setReadOnly(readOnly);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("room creation statistics read unavailable", failure);
        }
    }

    /** Immutable receipt loss is an integrity/generation error, not a negative business event. */
    public Map<LocalDate, Long> reconcile(Snapshot earlier, Snapshot later) {
        if (earlier == null || later == null || earlier.issuer != issuer || later.issuer != issuer
                || !earlier.scope.equals(later.scope) || !earlier.window.equals(later.window)
                || later.observedAt.isBefore(earlier.observedAt))
            throw new IllegalArgumentException("incompatible creation snapshots");
        Map<LocalDate, Long> delta = new TreeMap<>();
        earlier.unknownByDay.forEach((day, count) -> {
            long difference = later.unknownByDay.get(day) - count;
            if (difference < 0) throw new IllegalStateException("immutable receipt count decreased");
            if (difference > 0) delta.put(day, difference);
        });
        return Collections.unmodifiableMap(delta);
    }

    private static long positive(java.sql.ResultSet rows, int column) throws SQLException {
        long value = rows.getLong(column);
        if (rows.wasNull() || value <= 0) throw new SQLException("invalid creation identity");
        return value;
    }
    private static String text(String value, int limit) throws SQLException {
        if (value == null || value.isBlank() || value.length() > limit
                || value.chars().anyMatch(Character::isISOControl)) throw new SQLException("invalid creation field");
        return value;
    }
    private static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
    public static final class QueryBudgetExceeded extends IllegalStateException {
        public QueryBudgetExceeded() { super("creation statistics row budget exceeded"); }
    }
}
