package com.aoo.bcg.telemetry;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class JdbcRoomCreationStatisticsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 17);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), UTC);
    private static String hash(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }
    /** Scalar consumer fixture; does not pretend to execute MySQL07 immutable triggers. */
    private static final class Fixture implements AutoCloseable {
        final JdbcDataSource ds = new JdbcDataSource();
        Fixture(boolean keys) throws Exception {
            ds.setURL("jdbc:h2:mem:room_stats_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
            execute("CREATE TABLE aoo_room_create_saga(request_id VARCHAR(128),request_hash VARCHAR(128),account_id BIGINT,room_id BIGINT,game_id BIGINT,play_version VARCHAR(64),step VARCHAR(32),updated_at TIMESTAMP(3))");
            execute("CREATE TABLE aoo_room_create_confirmed_receipt(event_id CHAR(64)" + (keys ? " PRIMARY KEY" : "") + ",source_request_id VARCHAR(128)" + (keys ? " UNIQUE" : "") + ",request_hash VARCHAR(128),creator_id BIGINT,room_id BIGINT" + (keys ? " UNIQUE" : "") + ",game_id BIGINT,play_version VARCHAR(64),occurred_at TIMESTAMP(3),schema_version INT)");
        }
        void execute(String sql) throws Exception { try (var c=ds.getConnection();var s=c.createStatement()) { s.execute(sql); } }
        void saga(int id, String state) throws Exception {
            try(var c=ds.getConnection();var s=c.prepareStatement("INSERT INTO aoo_room_create_saga VALUES(?,?,?,?,?,?,?,?)")) {
                s.setString(1,"create-request-"+id);s.setString(2,"request-hash-"+id);s.setLong(3,100+id);s.setLong(4,1000+id);s.setLong(5,2999);s.setString(6,"cx-v1.0.0");s.setString(7,state);s.setTimestamp(8,Timestamp.from(CLOCK.instant()),utc());s.executeUpdate();
            }
        }
        void receipt(int id, Instant at) throws Exception {
            try(var c=ds.getConnection();var s=c.prepareStatement("INSERT INTO aoo_room_create_confirmed_receipt VALUES(?,?,?,?,?,?,?,?,1)")) {
                s.setString(1,hash("ROOM_CREATED_CONFIRMED:v1:create-request-"+id));s.setString(2,"create-request-"+id);s.setString(3,"request-hash-"+id);s.setLong(4,100+id);s.setLong(5,1000+id);s.setLong(6,2999);s.setString(7,"cx-v1.0.0");s.setTimestamp(8,Timestamp.from(at),utc());s.executeUpdate();
            }
        }
        void fact(int id, String at) throws Exception { saga(id,"CONFIRMED");receipt(id,Instant.parse(at)); }
        JdbcRoomCreationStatistics reader() { return new JdbcRoomCreationStatistics(ds,CLOCK); }
        @Override public void close() throws Exception { execute("DROP ALL OBJECTS"); }
    }
    private static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    private static JdbcRoomCreationStatistics.Snapshot today(JdbcRoomCreationStatistics x) { return x.snapshot(DAY,DAY.plusDays(1),UTC); }

    @Test void realFinalReceiptAndRecomputedReplayAreOneUnknownEvent() throws Exception {
        try(var f=new Fixture(true)) {
            f.fact(1,"2026-10-17T01:00:00Z");var reader=f.reader();var a=today(reader);var b=today(reader);
            assertEquals(1,a.totalUnknown());assertEquals("ROOM_CREATED",a.factKind());assertEquals("UNKNOWN",a.classification());assertEquals("CURRENT_RETAINED_FINAL_CONFIRMED_RECEIPTS",a.sourceDefinition());assertEquals(1,a.schemaVersion());assertTrue(reader.reconcile(a,b).isEmpty());
            assertThrows(UnsupportedOperationException.class,()->a.unknownByDay().put(DAY,9L));
        }
    }
    @Test void oldConfirmedAndPendingWithoutReceiptsNeverInventEvents() throws Exception {
        try(var f=new Fixture(true)) {
            int id=1;for(String step:List.of("CONFIRMED","STARTED","RESERVED","REGISTERED","AUTHORITY_CREATED","COMPENSATION_PENDING","COMPENSATED"))f.saga(id++,step);
            assertEquals(0,today(f.reader()).totalUnknown());
        }
    }
    @ParameterizedTest @ValueSource(strings={"orphan","step","event","hash","creator","room","game","play","schema","nulltime"})
    void damagedReceiptOrSourceRejectsInsteadOfDisappearing(String kind) throws Exception {
        try(var f=new Fixture(true)) {
            f.fact(1,"2026-10-17T01:00:00Z");
            switch(kind) {
                case "orphan" -> f.execute("DELETE FROM aoo_room_create_saga");
                case "step" -> f.execute("UPDATE aoo_room_create_saga SET step='AUTHORITY_CREATED'");
                case "event" -> f.execute("UPDATE aoo_room_create_confirmed_receipt SET event_id=REPEAT('0',64)");
                case "hash" -> f.execute("UPDATE aoo_room_create_saga SET request_hash='different'");
                case "creator" -> f.execute("UPDATE aoo_room_create_saga SET account_id=999");
                case "room" -> f.execute("UPDATE aoo_room_create_saga SET room_id=999");
                case "game" -> f.execute("UPDATE aoo_room_create_saga SET game_id=5");
                case "play" -> f.execute("UPDATE aoo_room_create_saga SET play_version='other'");
                case "schema" -> f.execute("UPDATE aoo_room_create_confirmed_receipt SET schema_version=2");
                case "nulltime" -> f.execute("UPDATE aoo_room_create_saga SET account_id=NULL");
                default -> throw new AssertionError();
            }
            assertThrows(IllegalStateException.class,()->today(f.reader()));
        }
    }
    @Test void immutableOccurrenceNotMutableSagaTimeOrCurrentRoomExistence() throws Exception {
        try(var f=new Fixture(true)) {
            f.fact(1,"2026-10-17T01:00:00Z");f.execute("CREATE TABLE aoo_hall_room(room_id BIGINT,state VARCHAR)");f.execute("INSERT INTO aoo_hall_room VALUES(1001,'OPEN')");f.execute("CREATE TABLE aoo_room_snapshot(room_id BIGINT)");f.execute("INSERT INTO aoo_room_snapshot VALUES(1001)");
            var reader=f.reader();var before=today(reader);f.execute("UPDATE aoo_room_create_saga SET updated_at=TIMESTAMP '2026-10-20 00:00:00'");f.execute("UPDATE aoo_hall_room SET state='DISSOLVED'");f.execute("DELETE FROM aoo_room_snapshot");
            assertEquals(1,today(reader).totalUnknown());assertTrue(reader.reconcile(before,today(reader)).isEmpty());
        }
    }
    @Test void halfOpenBoundaryAndExplicitDstZoneIgnoreJvmDefaultZone() throws Exception {
        TimeZone original=TimeZone.getDefault();
        try(var f=new Fixture(true)) {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            f.fact(1,"2026-03-08T04:59:59Z");f.fact(2,"2026-03-08T05:00:00Z");f.fact(3,"2026-03-09T03:59:59Z");f.fact(4,"2026-03-09T04:00:00Z");
            var d=LocalDate.of(2026,3,8);assertEquals(2,f.reader().snapshot(d,d.plusDays(1),ZoneId.of("America/New_York")).totalUnknown());
        } finally {TimeZone.setDefault(original);}
    }
    @Test void rowCapDoesNotReturnPartialCounts() throws Exception {
        try(var f=new Fixture(true)) {f.fact(1,"2026-10-17T01:00:00Z");f.fact(2,"2026-10-17T02:00:00Z");assertThrows(JdbcRoomCreationStatistics.QueryBudgetExceeded.class,()->today(new JdbcRoomCreationStatistics(f.ds,CLOCK,1)));}
    }
    @Test void corruptFixtureDuplicateIdentityFailsClosed() throws Exception {
        try(var f=new Fixture(false)) {f.fact(1,"2026-10-17T01:00:00Z");f.receipt(1,Instant.parse("2026-10-17T01:00:00Z"));assertThrows(IllegalStateException.class,()->today(f.reader()));}
    }
    @Test void lateReceiptAddsButImmutableLossAndCrossIssuerOrZoneReject() throws Exception {
        try(var f=new Fixture(true);var other=new Fixture(true)) {
            var reader=f.reader();var a=today(reader);f.fact(1,"2026-10-17T01:00:00Z");var b=today(reader);assertEquals(Map.of(DAY,1L),reader.reconcile(a,b));
            assertThrows(IllegalArgumentException.class,()->reader.reconcile(a,today(other.reader())));
            assertThrows(IllegalArgumentException.class,()->reader.reconcile(a,reader.snapshot(DAY,DAY.plusDays(1),ZoneId.of("Asia/Shanghai"))));
            f.execute("DELETE FROM aoo_room_create_confirmed_receipt");assertThrows(IllegalStateException.class,()->reader.reconcile(b,today(reader)));
        }
    }
    @Test void invalidWindowAndBudgetRejectBeforeJdbc() throws Exception {
        try(var f=new Fixture(true)) {
            var x=f.reader();assertThrows(IllegalArgumentException.class,()->x.snapshot(DAY,DAY,UTC));assertThrows(IllegalArgumentException.class,()->x.snapshot(DAY,DAY.plusDays(32),UTC));assertThrows(IllegalArgumentException.class,()->x.snapshot(DAY,DAY.plusDays(1),null));assertThrows(IllegalArgumentException.class,()->new JdbcRoomCreationStatistics(f.ds,CLOCK,0));assertThrows(IllegalArgumentException.class,()->new JdbcRoomCreationStatistics(f.ds,CLOCK,10001));
        }
    }
    @Test void queryIsScalarBoundedAndRestoresReadOnlyFlagWithoutTransactionWrites() throws Exception {
        try(var f=new Fixture(true)) {
            f.fact(1,"2026-10-17T01:00:00Z");List<Boolean> flags=new ArrayList<>();List<String> queries=new ArrayList<>();AtomicInteger timeout=new AtomicInteger(),maxRows=new AtomicInteger();
            DataSource ds=(DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(obj,m,args)->{
                if(!m.getName().equals("getConnection"))return m.invoke(f.ds,args);
                Connection raw=f.ds.getConnection();
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(o,method,a)->{
                    if(Set.of("commit","rollback").contains(method.getName()))throw new AssertionError("reader must not own transactions");
                    if(method.getName().equals("setReadOnly"))flags.add((Boolean)a[0]);
                    if(method.getName().equals("prepareStatement")){
                        queries.add((String)a[0]);PreparedStatement stmt=(PreparedStatement)method.invoke(raw,a);
                        return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(o2,m2,a2)->{
                            if(m2.getName().equals("setQueryTimeout"))timeout.set((Integer)a2[0]);if(m2.getName().equals("setMaxRows"))maxRows.set((Integer)a2[0]);
                            try{return m2.invoke(stmt,a2);}catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
                        });
                    }
                    try{return method.invoke(raw,a);}catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
                });
            });
            assertEquals(1,today(new JdbcRoomCreationStatistics(ds,CLOCK)).totalUnknown());assertEquals(List.of(true,false),flags);assertEquals(3,timeout.get());assertEquals(10001,maxRows.get());assertEquals(1,queries.size());String sql=queries.getFirst().toLowerCase(Locale.ROOT);assertTrue(sql.contains("left join"));assertFalse(sql.contains("request_json"));assertFalse(sql.contains("response_json"));assertFalse(sql.contains("updated_at"));assertFalse(sql.contains(" for update"));assertTrue(sql.startsWith("select "));
        }
    }
}
