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
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Read-only counts of currently retained account creation rows, not lifetime event history. */
public final class JdbcAccountRegistrationStatistics {
    public static final int MAX_DAYS = 31;
    public static final int MAX_ROWS = 10_000;
    public static final int QUERY_TIMEOUT_SECONDS = 3;
    private static final String SQL = "SELECT account_id,created_at FROM aoo_account "
            + "WHERE created_at>=? AND created_at<? ORDER BY created_at,account_id LIMIT ?";
    private static final String COHORT_SCHEMA = "aoo_local_dev_login_20261016_r1";
    private static final String SEED_SHA = "2c4421f37c02672fd98455c48484fbf1620ec53a40d3324f17938357b7a7f9be";
    private static final String RECEIPT_SHA = "d1674e436bf6a76175a8dad3eee153801f3ee04d6158261e22c7b1296c559690";
    private static final String COHORT_SQL = "SELECT a.account_id,a.created_at,EXISTS(SELECT 1 FROM aoo_display_id_history h "
            + "WHERE h.account_id=a.account_id AND a.account_id BETWEEN 1 AND 100 "
            + "AND h.reason='LOCAL_DEVELOPMENT_INITIALIZATION' AND h.trace_id='dev-init-20261016' "
            + "AND h.new_display_id=CAST(100000+a.account_id AS CHAR(6))) "
            + "FROM aoo_account a WHERE a.created_at>=? AND a.created_at<? ORDER BY a.created_at,a.account_id LIMIT ?";

    /** Trusted server composition dependency, not a request-supplied or cryptographic assertion. */
    @FunctionalInterface public interface TestCohortSourceVerifier {
        Optional<TestCohortSourceWitness> verify(DataSource actualSource, Connection openedConnection) throws SQLException;
    }
    public record TestCohortSourceWitness(DataSource actualSource, Connection openedConnection,
            String sourceInstanceId, String generationDigest, String schema, String traceId, String reason,
            String seedToolSHA, String stage1ReceiptSHA) {}
    private final TestCohortSourceVerifier cohortVerifier;
    private final DataSource source;
    private final Clock clock;
    private final int rowLimit;
    private final Object issuer = new Object();
    private final String sourceSalt = UUID.randomUUID().toString();

    public JdbcAccountRegistrationStatistics(DataSource source, Clock clock) {
        this(source, clock, MAX_ROWS, null);
    }

    public JdbcAccountRegistrationStatistics(DataSource source, Clock clock, int rowLimit) {
        this(source, clock, rowLimit, null);
    }

    public JdbcAccountRegistrationStatistics(DataSource source, Clock clock, TestCohortSourceVerifier verifier) {
        this(source, clock, MAX_ROWS, verifier);
    }

    public JdbcAccountRegistrationStatistics(DataSource source, Clock clock, int rowLimit,
                                              TestCohortSourceVerifier verifier) {
        this.cohortVerifier = verifier;
        this.source = Objects.requireNonNull(source);
        this.clock = Objects.requireNonNull(clock);
        if (rowLimit < 1 || rowLimit > MAX_ROWS) throw new IllegalArgumentException("invalid row budget");
        this.rowLimit = rowLimit;
    }

    public record Window(LocalDate startInclusive, LocalDate endExclusive, ZoneId reportingZone) {
        public Window {
            if (startInclusive == null || endExclusive == null || reportingZone == null) {
                throw new IllegalArgumentException("range and reporting zone required");
            }
            long days = ChronoUnit.DAYS.between(startInclusive, endExclusive);
            if (days < 1 || days > MAX_DAYS) throw new IllegalArgumentException("range must cover 1..31 calendar days");
        }
    }

    /** Only this service can issue snapshots; source identity is private and cannot be caller supplied. */
    public static final class Snapshot {
        private final Object issuer;
        private final String sourceScope;
        private final Window window;
        private final Instant observedAt;
        private final Map<LocalDate, Long> unknownByDay;
        private final Map<LocalDate, Long> testByDay;

        private Snapshot(Object issuer, String sourceScope, Window window, Instant observedAt,
                         Map<LocalDate, Long> unknownByDay, Map<LocalDate, Long> testByDay) {
            this.issuer = issuer;
            this.sourceScope = sourceScope;
            this.window = window;
            this.observedAt = observedAt;
            this.unknownByDay = Collections.unmodifiableMap(new TreeMap<>(unknownByDay));
            this.testByDay = Collections.unmodifiableMap(new TreeMap<>(testByDay));
        }

        public Window window() { return window; }
        public Instant observedAt() { return observedAt; }
        public Map<LocalDate, Long> unknownByDay() { return unknownByDay; }
        public String factKind() { return "ACCOUNT_CREATED"; }
        public String sourceDefinition() { return "CURRENT_RETAINED_AOO_ACCOUNT_CREATION_ROWS"; }
        public Map<LocalDate, Long> testByDay() { return testByDay; }
        public long totalTest() { return testByDay.values().stream().mapToLong(Long::longValue).sum(); }
        public String classification() { return totalTest() == 0 ? "UNKNOWN" : totalUnknown() == 0 ? "TEST" : "MIXED"; }
        public int projectionVersion() { return 2; }
        public long totalUnknown() { return unknownByDay.values().stream().mapToLong(Long::longValue).sum(); }

        @Override public String toString() {
            return sourceDefinition() + " " + window + " UNKNOWN=" + unknownByDay + " TEST=" + testByDay + " observedAt=" + observedAt;
        }
    }

    public Snapshot snapshot(LocalDate startInclusive, LocalDate endExclusive, ZoneId reportingZone) {
        Window window = new Window(startInclusive, endExclusive, reportingZone);
        Instant start = startInclusive.atStartOfDay(reportingZone).toInstant();
        Instant end = endExclusive.atStartOfDay(reportingZone).toInstant();
        Map<LocalDate, Long> counts = new TreeMap<>();
        startInclusive.datesUntil(endExclusive).forEach(day -> counts.put(day, 0L));
        Map<LocalDate, Long> testCounts = new TreeMap<>(counts);
        Map<Long, Instant> seen = new HashMap<>();
        try (Connection connection = source.getConnection()) {
            connection.setReadOnly(true);
            TestCohortSourceWitness witness = verifiedWitness(connection);
            String scope = sourceScope(connection) + (witness == null ? ":UNKNOWN" : ":" + witnessScope(witness));
            try (var query = connection.prepareStatement(witness == null ? SQL : COHORT_SQL)) {
                // Match the existing Account writer's JDBC Timestamp/Instant convention.
                query.setTimestamp(1, Timestamp.from(start));
                query.setTimestamp(2, Timestamp.from(end));
                query.setInt(3, rowLimit + 1);
                query.setMaxRows(rowLimit + 1);
                query.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                try (var rows = query.executeQuery()) {
                    int scanned = 0;
                    while (rows.next()) {
                        if (++scanned > rowLimit) throw new QueryBudgetExceeded();
                        long account = rows.getLong(1);
                        if (rows.wasNull()) throw new SQLException("missing account fact key");
                        Timestamp timestamp = rows.getTimestamp(2);
                        if (timestamp == null) throw new SQLException("missing account creation time");
                        Instant createdAt = timestamp.toInstant();
                        LocalDate day = createdAt.atZone(reportingZone).toLocalDate();
                        if (!counts.containsKey(day)) throw new SQLException("JDBC time mapping does not match query range");
                        Instant previous = seen.putIfAbsent(account, createdAt);
                        if (previous != null && !previous.equals(createdAt)) {
                            throw new SQLException("conflicting account creation fact");
                        }
                        if (previous == null) {
                            Map<LocalDate, Long> target = witness != null && rows.getBoolean(3) ? testCounts : counts;
                            target.put(day, Math.addExact(target.get(day), 1L));
                        }
                    }
                }
            }
            return new Snapshot(issuer, scope, window, clock.instant(), counts, testCounts);
        } catch (SQLException failure) {
            throw new IllegalStateException("registration statistics read unavailable", failure);
        }
    }

    /** Recomputed state-view corrections; a deleted retained row can legitimately produce a negative delta. */
    public Map<LocalDate, Long> reconcile(Snapshot earlier, Snapshot later) {
        compatible(earlier, later);
        return corrections(earlier.unknownByDay, later.unknownByDay);
    }

    public Map<LocalDate, Long> reconcileTest(Snapshot earlier, Snapshot later) {
        compatible(earlier, later);
        return corrections(earlier.testByDay, later.testByDay);
    }

    private void compatible(Snapshot earlier, Snapshot later) {
        if (earlier == null || later == null || earlier.issuer != issuer || later.issuer != issuer
                || !earlier.sourceScope.equals(later.sourceScope) || !earlier.window.equals(later.window)
                || later.observedAt.isBefore(earlier.observedAt)) {
            throw new IllegalArgumentException("snapshots have incompatible source, window, zone or observation time");
        }
    }

    private static Map<LocalDate, Long> corrections(Map<LocalDate, Long> earlier, Map<LocalDate, Long> later) {
        Map<LocalDate, Long> corrections = new TreeMap<>();
        earlier.forEach((day, count) -> {
            long delta = later.get(day) - count;
            if (delta != 0) corrections.put(day, delta);
        });
        return Collections.unmodifiableMap(corrections);
    }

    private TestCohortSourceWitness verifiedWitness(Connection connection) throws SQLException {
        if (cohortVerifier == null) return null;
        Optional<TestCohortSourceWitness> result = cohortVerifier.verify(source, connection);
        if (result == null || result.isEmpty()) return null;
        var w = result.get();
        return w.actualSource() == source && w.openedConnection() == connection
                && text(w.sourceInstanceId()) && w.generationDigest() != null && w.generationDigest().matches("[0-9a-f]{64}")
                && COHORT_SCHEMA.equals(w.schema()) && COHORT_SCHEMA.equals(connection.getCatalog())
                && "dev-init-20261016".equals(w.traceId()) && "LOCAL_DEVELOPMENT_INITIALIZATION".equals(w.reason())
                && SEED_SHA.equals(w.seedToolSHA()) && RECEIPT_SHA.equals(w.stage1ReceiptSHA()) ? w : null;
    }

    private static boolean text(String value) {
        return value != null && !value.isBlank() && value.length() <= 256 && value.chars().noneMatch(Character::isISOControl);
    }

    private static String witnessScope(TestCohortSourceWitness w) {
        // Keep deployment/receipt identifiers private; length framing avoids ambiguous tuples.
        StringBuilder value = new StringBuilder();
        for (String part : new String[]{w.sourceInstanceId(), w.generationDigest(), w.schema(), w.traceId(), w.reason(), w.seedToolSHA(), w.stage1ReceiptSHA()})
            value.append(part.length()).append(':').append(part);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("source identity digest unavailable", unavailable); }
    }

    private String sourceScope(Connection connection) throws SQLException {
        String url = connection.getMetaData().getURL();
        if (url == null || url.isBlank()) throw new SQLException("database source identity unavailable");
        // Do not expose or persist JDBC URLs: they can contain connection secrets.
        String descriptor = sourceSalt + '\0' + url + '\0' + connection.getCatalog() + '\0' + connection.getSchema();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(descriptor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("source identity digest unavailable", unavailable);
        }
    }

    public static final class QueryBudgetExceeded extends IllegalStateException {
        private QueryBudgetExceeded() { super("registration statistics row budget exceeded; no partial result"); }
    }
}
