package com.aoo.bcg.telemetry;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class JdbcAccountRegistrationStatisticsTest {
    private JdbcDataSource db;
    private final AtomicReference<Instant> observed = new AtomicReference<>(Instant.parse("2026-10-14T12:00:00Z"));
    private Clock clock;
    private JdbcAccountRegistrationStatistics statistics;
    private final LocalDate first = LocalDate.of(2026, 10, 13);

    @BeforeEach void setup() throws Exception {
        db = database("registration_" + System.nanoTime());
        clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return observed.get(); }
        };
        statistics = new JdbcAccountRegistrationStatistics(db, clock);
    }

    @AfterEach void cleanup() throws Exception { shutdown(db); }

    @Test void explicitZoneAndHalfOpenBoundsReturnOnlyUnknownMaskedBuckets() throws Exception {
        insert(81234567L, "2026-10-13T15:59:59.999Z");
        insert(82345678L, "2026-10-13T16:00:00Z");
        insert(83456789L, "2026-10-14T16:00:00Z");
        var snapshot = statistics.snapshot(first.plusDays(1), first.plusDays(2), ZoneId.of("Asia/Shanghai"));
        assertEquals(Map.of(first.plusDays(1), 1L), snapshot.unknownByDay());
        assertEquals(1, snapshot.totalUnknown());
        assertEquals("UNKNOWN", snapshot.classification());
        assertEquals("ACCOUNT_CREATED", snapshot.factKind());
        assertEquals("CURRENT_RETAINED_AOO_ACCOUNT_CREATION_ROWS", snapshot.sourceDefinition());
        assertFalse(snapshot.toString().contains("82345678"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.unknownByDay().clear());
    }

    @Test void repeatedReadsAndLateVisibleFactsCorrectOriginalDayWithoutAccumulation() throws Exception {
        insert(1, "2026-10-13T02:00:00Z");
        var before = statistics.snapshot(first, first.plusDays(2), ZoneOffset.UTC);
        var repeated = statistics.snapshot(first, first.plusDays(2), ZoneOffset.UTC);
        assertEquals(Map.of(), statistics.reconcile(before, repeated));
        insert(99999999, "2026-10-13T04:00:00Z");
        observed.set(observed.get().plusSeconds(60));
        var after = statistics.snapshot(first, first.plusDays(2), ZoneOffset.UTC);
        assertEquals(2, after.totalUnknown());
        assertEquals(Map.of(first, 1L), statistics.reconcile(before, after));
        assertEquals(Map.of(), statistics.reconcile(after, statistics.snapshot(first, first.plusDays(2), ZoneOffset.UTC)));
    }

    @Test void calendarDayWithDstUsesItsActualBoundaries() throws Exception {
        insert(1, "2026-03-08T05:00:00Z");
        insert(2, "2026-03-09T03:59:59.999Z");
        insert(3, "2026-03-09T04:00:00Z");
        var day = LocalDate.of(2026, 3, 8);
        assertEquals(Map.of(day, 2L), statistics.snapshot(day, day.plusDays(1), ZoneId.of("America/New_York")).unknownByDay());
    }

    @Test void invalidParametersRejectBeforeAccessingTheDatabase() {
        AtomicInteger connections = new AtomicInteger();
        DataSource forbidden = (DataSource)java.lang.reflect.Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class[]{DataSource.class}, (p,m,a) -> {
            if (m.getName().equals("getConnection")) connections.incrementAndGet();
            throw new AssertionError("invalid request accessed database");
        });
        var reader = new JdbcAccountRegistrationStatistics(forbidden, clock);
        assertThrows(IllegalArgumentException.class, () -> reader.snapshot(first, first.plusDays(1), null));
        assertThrows(IllegalArgumentException.class, () -> reader.snapshot(null, first, ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> reader.snapshot(first, first, ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> reader.snapshot(first, first.minusDays(1), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> reader.snapshot(first, first.plusDays(32), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> new JdbcAccountRegistrationStatistics(forbidden, clock, 0));
        assertThrows(IllegalArgumentException.class, () -> new JdbcAccountRegistrationStatistics(forbidden, clock, 10001));
        assertEquals(0, connections.get());
    }

    @Test void rowBudgetOverflowReturnsNoPartialSnapshot() throws Exception {
        for (int i = 1; i <= 3; i++) insert(i, "2026-10-13T02:00:00Z");
        var bounded = new JdbcAccountRegistrationStatistics(db, clock, 2);
        assertThrows(JdbcAccountRegistrationStatistics.QueryBudgetExceeded.class,
                () -> bounded.snapshot(first, first.plusDays(1), ZoneOffset.UTC));
    }

    @Test void reconciliationRejectsDifferentSourcesRetargetingWindowsZonesAndTimeReversal() throws Exception {
        var before = statistics.snapshot(first, first.plusDays(1), ZoneOffset.UTC);
        var other = database("other_" + System.nanoTime());
        String originalUrl = db.getURL();
        try {
            var otherReader = new JdbcAccountRegistrationStatistics(other, clock);
            assertThrows(IllegalArgumentException.class, () -> statistics.reconcile(before,
                    otherReader.snapshot(first, first.plusDays(1), ZoneOffset.UTC)));
            db.setURL(other.getURL());
            var retargeted = statistics.snapshot(first, first.plusDays(1), ZoneOffset.UTC);
            assertThrows(IllegalArgumentException.class, () -> statistics.reconcile(before, retargeted));
            db.setURL(originalUrl);
            assertThrows(IllegalArgumentException.class, () -> statistics.reconcile(before,
                    statistics.snapshot(first, first.plusDays(2), ZoneOffset.UTC)));
            assertThrows(IllegalArgumentException.class, () -> statistics.reconcile(before,
                    statistics.snapshot(first, first.plusDays(1), ZoneId.of("Asia/Shanghai"))));
            observed.set(observed.get().minusSeconds(1));
            var reversed = statistics.snapshot(first, first.plusDays(1), ZoneOffset.UTC);
            assertThrows(IllegalArgumentException.class, () -> statistics.reconcile(before, reversed));
        } finally { db.setURL(originalUrl); shutdown(other); }
    }

    @Test void queryTimeoutAndReadOnlyBudgetFailClosedWithOriginalCause() {
        var timeout = new SQLTimeoutException("isolated timeout fixture");
        AtomicInteger seconds = new AtomicInteger();
        AtomicBoolean readOnly = new AtomicBoolean();
        DataSource wrapped = (DataSource)java.lang.reflect.Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class[]{DataSource.class}, (p,m,a) -> {
            if (!m.getName().equals("getConnection")) return invoke(m, db, a);
            var connection = db.getConnection();
            return java.lang.reflect.Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(), new Class[]{java.sql.Connection.class}, (c,method,args) -> {
                if (method.getName().equals("setReadOnly")) readOnly.set((Boolean)args[0]);
                if (!method.getName().equals("prepareStatement")) return invoke(method, connection, args);
                var query = (java.sql.PreparedStatement)invoke(method, connection, args);
                return java.lang.reflect.Proxy.newProxyInstance(java.sql.PreparedStatement.class.getClassLoader(), new Class[]{java.sql.PreparedStatement.class}, (q,call,values) -> {
                    if (call.getName().equals("setQueryTimeout")) seconds.set((Integer)values[0]);
                    if (call.getName().equals("executeQuery")) throw timeout;
                    return invoke(call, query, values);
                });
            });
        });
        var error = assertThrows(IllegalStateException.class, () -> new JdbcAccountRegistrationStatistics(wrapped, clock)
                .snapshot(first, first.plusDays(1), ZoneOffset.UTC));
        assertSame(timeout, error.getCause());
        assertEquals(3, seconds.get());
        assertTrue(readOnly.get());
    }

    // Explicit synthetic trusted verifier; this does not attest the real local MySQL instance.
    private JdbcAccountRegistrationStatistics.TestCohortSourceWitness witness(DataSource source,
            java.sql.Connection connection, String variant) {
        return new JdbcAccountRegistrationStatistics.TestCohortSourceWitness(
                variant.equals("source") ? db : source, variant.equals("connection") ? null : connection,
                variant.equals("instance") ? "" : "synthetic-fixture-instance",
                variant.equals("digest") ? "invalid" : variant.equals("generation2") ? "b".repeat(64) : "a".repeat(64),
                variant.equals("schema") ? "other_schema" : "aoo_local_dev_login_20261016_r1",
                variant.equals("trace") ? "other-init" : "dev-init-20261016",
                variant.equals("reason") ? "other-reason" : "LOCAL_DEVELOPMENT_INITIALIZATION",
                variant.equals("tool") ? "c".repeat(64) : "2c4421f37c02672fd98455c48484fbf1620ec53a40d3324f17938357b7a7f9be",
                variant.equals("receipt") ? "d".repeat(64) : "d1674e436bf6a76175a8dad3eee153801f3ee04d6158261e22c7b1296c559690");
    }

    private DataSource cohortSource(String catalog) throws Exception {
        try (var c = db.getConnection(); var q = c.createStatement()) {
            q.execute("CREATE TABLE aoo_display_id_history(account_id BIGINT,new_display_id VARCHAR(32),reason VARCHAR(64),trace_id VARCHAR(64),valid_from TIMESTAMP(3))");
        }
        return (DataSource)java.lang.reflect.Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class[]{DataSource.class}, (p,m,a) -> {
            if (!m.getName().equals("getConnection")) return invoke(m, db, a);
            var connection = db.getConnection();
            return java.lang.reflect.Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(), new Class[]{java.sql.Connection.class}, (c,method,args) -> {
                if (method.getName().equals("getCatalog")) return catalog;
                return invoke(method, connection, args);
            });
        });
    }

    private void marker(long account, String display, String reason, String trace) throws Exception {
        try (var c = db.getConnection(); var q = c.prepareStatement("INSERT INTO aoo_display_id_history VALUES(?,?,?,?,CURRENT_TIMESTAMP)")) {
            q.setLong(1, account); q.setString(2, display); q.setString(3, reason); q.setString(4, trace); q.executeUpdate();
        }
    }

    @Test void verifiedCohortUsesCanonicalMarkerAndNeverDuplicatesOrGuessesRange() throws Exception {
        var source = cohortSource("aoo_local_dev_login_20261016_r1");
        insert(1, "2026-10-13T02:00:00Z"); insert(2, "2026-10-13T03:00:00Z"); insert(101, "2026-10-13T04:00:00Z");
        marker(1, "100001", "LOCAL_DEVELOPMENT_INITIALIZATION", "dev-init-20261016");
        marker(1, "100001", "LOCAL_DEVELOPMENT_INITIALIZATION", "dev-init-20261016");
        marker(101, "100101", "LOCAL_DEVELOPMENT_INITIALIZATION", "dev-init-20261016");
        var reader = new JdbcAccountRegistrationStatistics(source, clock, (actual,c) -> java.util.Optional.of(witness(actual,c,"valid")));
        var result = reader.snapshot(first, first.plusDays(1), ZoneOffset.UTC);
        assertEquals(1, result.totalTest()); assertEquals(2, result.totalUnknown()); assertEquals("MIXED", result.classification());
        assertEquals(Map.of(first,1L), result.testByDay()); assertThrows(UnsupportedOperationException.class, () -> result.testByDay().clear());
        assertFalse(result.toString().contains("synthetic-fixture-instance"));
        assertEquals(3, new JdbcAccountRegistrationStatistics(source,clock).snapshot(first,first.plusDays(1),ZoneOffset.UTC).totalUnknown());
    }

    @Test void absentRevokedAndMismatchedWitnessesAlwaysStayUnknown() throws Exception {
        var source = cohortSource("aoo_local_dev_login_20261016_r1"); insert(1,"2026-10-13T02:00:00Z");
        marker(1,"100001","LOCAL_DEVELOPMENT_INITIALIZATION","dev-init-20261016");
        for (String invalid : new String[]{"source","connection","instance","digest","schema","trace","reason","tool","receipt"}) {
            var reader = new JdbcAccountRegistrationStatistics(source,clock,(actual,c) -> java.util.Optional.of(witness(actual,c,invalid)));
            var result = reader.snapshot(first,first.plusDays(1),ZoneOffset.UTC);
            assertEquals(0,result.totalTest(),invalid); assertEquals(1,result.totalUnknown(),invalid);
        }
        var revoked = new JdbcAccountRegistrationStatistics(source,clock,(actual,c) -> java.util.Optional.empty());
        assertEquals(0,revoked.snapshot(first,first.plusDays(1),ZoneOffset.UTC).totalTest());
    }

    @Test void copiedMarkerInWrongCatalogOrOtherDataSourceCannotAuthorizeTest() throws Exception {
        var source = cohortSource("other_schema"); insert(1,"2026-10-13T02:00:00Z");
        marker(1,"100001","LOCAL_DEVELOPMENT_INITIALIZATION","dev-init-20261016");
        var reader = new JdbcAccountRegistrationStatistics(source,clock,(actual,c) -> java.util.Optional.of(witness(actual,c,"valid")));
        assertEquals(0,reader.snapshot(first,first.plusDays(1),ZoneOffset.UTC).totalTest());
        var otherReader = new JdbcAccountRegistrationStatistics(db,clock,(actual,c) -> java.util.Optional.of(witness(source,c,"valid")));
        assertEquals(0,otherReader.snapshot(first,first.plusDays(1),ZoneOffset.UTC).totalTest());
    }

    @Test void wrongPerSubjectDisplayTraceReasonAndUnmarkedIdsRemainUnknown() throws Exception {
        var source=cohortSource("aoo_local_dev_login_20261016_r1");
        for(int id=1;id<=4;id++)insert(id,"2026-10-13T02:00:00Z");
        marker(1,"100099","LOCAL_DEVELOPMENT_INITIALIZATION","dev-init-20261016");
        marker(2,"100002","LOCAL_DEVELOPMENT_INITIALIZATION","other-init");
        marker(3,"100003","ordinary-creation","dev-init-20261016");
        var reader=new JdbcAccountRegistrationStatistics(source,clock,(actual,c)->java.util.Optional.of(witness(actual,c,"valid")));
        var result=reader.snapshot(first,first.plusDays(1),ZoneOffset.UTC);assertEquals(0,result.totalTest());assertEquals(4,result.totalUnknown());
    }

    @Test void purposeCorrectionsPreserveLateDayZoneAndWitnessCompatibility() throws Exception {
        var source=cohortSource("aoo_local_dev_login_20261016_r1");var version=new AtomicReference<>("valid");
        var reader=new JdbcAccountRegistrationStatistics(source,clock,(actual,c)->java.util.Optional.of(witness(actual,c,version.get())));
        insert(1,"2026-10-13T16:00:00Z");marker(1,"100001","LOCAL_DEVELOPMENT_INITIALIZATION","dev-init-20261016");
        var day=first.plusDays(1);var before=reader.snapshot(day,day.plusDays(1),ZoneId.of("Asia/Shanghai"));
        assertEquals(Map.of(day,1L),before.testByDay());assertEquals(Map.of(),reader.reconcileTest(before,reader.snapshot(day,day.plusDays(1),ZoneId.of("Asia/Shanghai"))));
        insert(2,"2026-10-13T17:00:00Z");marker(2,"100002","LOCAL_DEVELOPMENT_INITIALIZATION","dev-init-20261016");
        insert(3,"2026-10-13T18:00:00Z");observed.set(observed.get().plusSeconds(60));
        var later=reader.snapshot(day,day.plusDays(1),ZoneId.of("Asia/Shanghai"));
        assertEquals(Map.of(day,1L),reader.reconcileTest(before,later));assertEquals(Map.of(day,1L),reader.reconcile(before,later));
        version.set("generation2");var changed=reader.snapshot(day,day.plusDays(1),ZoneId.of("Asia/Shanghai"));
        assertThrows(IllegalArgumentException.class,()->reader.reconcile(before,changed));assertThrows(IllegalArgumentException.class,()->reader.reconcileTest(before,changed));
    }

    private static JdbcDataSource database(String name) throws Exception {
        var db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:" + name + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        try (var c = db.getConnection(); var s = c.createStatement()) {
            s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,created_at TIMESTAMP(3) NOT NULL)");
        }
        return db;
    }
    private void insert(long id, String instant) throws Exception {
        try (var c = db.getConnection(); var q = c.prepareStatement("INSERT INTO aoo_account VALUES(?,?)")) {
            q.setLong(1, id); q.setTimestamp(2, Timestamp.from(Instant.parse(instant))); q.executeUpdate();
        }
    }
    private static Object invoke(java.lang.reflect.Method method, Object target, Object[] args) throws Throwable {
        try { return method.invoke(target, args); } catch (InvocationTargetException error) { throw error.getCause(); }
    }
    private static void shutdown(JdbcDataSource db) throws Exception {
        try (var c = db.getConnection(); var s = c.createStatement()) { s.execute("SHUTDOWN"); }
    }
}
