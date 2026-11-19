package com.aoo.bcg.club;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class ClubCreatePolicyTest {
    private static final long ACTOR = 98;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    private static ClubCreatePolicy policy(boolean eligibility, long minimum, boolean charge, long cost, int version) {
        return new ClubCreatePolicy("ROOM_CARD", eligibility, minimum, charge, cost, version, "a".repeat(64));
    }
    private static final ClubCreatePolicy DEFAULT = policy(true,500,true,100,1);
    private record Fixture(JdbcDataSource source, ObjectMapper mapper, AtomicLong ids) {
        ClubDispatchService dispatcher(ClubCreatePolicy policy) {
            return new ClubDispatchService(source,mapper,CLOCK,ids::incrementAndGet,policy);
        }
        Map<?,?> create(ClubDispatchService dispatcher,String request,String name) {
            return (Map<?,?>)dispatcher.dispatch(ACTOR,request,"club.CClubCreate",Map.of("clubName",name));
        }
        long value(String sql) throws Exception { try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);} }
        void execute(String sql) throws Exception { try(var c=source.getConnection();var q=c.createStatement()){q.execute(sql);} }
        long balance() throws Exception {return value("SELECT balance FROM aoo_currency_balance WHERE player_id=98 AND currency='ROOM_CARD'");}
        void assertEmpty() throws Exception {assertEquals(0,value("SELECT COUNT(*) FROM aoo_club_state"));assertEquals(0,value("SELECT COUNT(*) FROM aoo_ledger"));assertEquals(0,value("SELECT COUNT(*) FROM aoo_club_write_idempotency"));}
    }
    private Fixture fixture(long balance) throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:club_policy_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        JdbcClubServiceIntegrationTest.schema(ds);
        try(var c=ds.getConnection();var q=c.createStatement()) {
            q.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR(32) PRIMARY KEY,scope_type VARCHAR(16),status VARCHAR(16))");
            q.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE'),('GOLD','GLOBAL','ACTIVE')");
            q.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR(32),currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");
            q.execute("INSERT INTO aoo_currency_balance VALUES(98,'ROOM_CARD',0,"+balance+",0,CURRENT_TIMESTAMP),(98,'GOLD',0,999999,0,CURRENT_TIMESTAMP),(98,'CRYSTAL',0,999999,0,CURRENT_TIMESTAMP)");
            q.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR(128) UNIQUE,player_id BIGINT,currency VARCHAR(32),currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR(32),entry_status VARCHAR(32),channel_code VARCHAR(32),created_at TIMESTAMP)");
        }
        return new Fixture(ds,new ObjectMapper().findAndRegisterModules(),new AtomicLong(1000));
    }
    @Test void policyChecksCurrencyRangesVersionAndHashFormat() {
        assertThrows(IllegalArgumentException.class,()->new ClubCreatePolicy("CRYSTAL",true,500,true,100,1,"a".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->policy(true,-1,true,100,1));
        assertThrows(IllegalArgumentException.class,()->policy(true,500,true,-1,1));
        assertThrows(IllegalArgumentException.class,()->policy(true,500,true,100,0));
        assertThrows(IllegalArgumentException.class,()->new ClubCreatePolicy("ROOM_CARD",true,500,true,100,1,"A".repeat(64)));
        assertDoesNotThrow(()->policy(false,0,false,0,1));
    }
    @Test void defaultFiveHundredQualifiesAndOnlyOneHundredIsDebited() throws Exception {
        var f=fixture(500);var result=f.create(f.dispatcher(DEFAULT),"default","fixture");
        assertEquals(400L,((Number)result.get("roomCard")).longValue());assertFalse(result.containsKey("diamond"));assertEquals(400,f.balance());
        assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_ledger WHERE currency='ROOM_CARD' AND delta=-100"));
        assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_club_member WHERE player_id=98 AND member_role='OWNER' AND member_status='ACTIVE'"));
        assertEquals(999999,f.value("SELECT balance FROM aoo_currency_balance WHERE player_id=98 AND currency='CRYSTAL'"));
    }
    @Test void belowQualificationFailsAtomicallyAndSameRequestCanRetryAfterFunding() throws Exception {
        var f=fixture(499);var d=f.dispatcher(DEFAULT);assertThrows(IllegalStateException.class,()->f.create(d,"retry","fixture"));f.assertEmpty();assertEquals(499,f.balance());
        f.execute("UPDATE aoo_currency_balance SET balance=500 WHERE currency='ROOM_CARD'");f.create(d,"retry","fixture");assertEquals(400,f.balance());
    }
    @Test void eligibilityOffStillRequiresSufficientCreationCharge() throws Exception {
        var f=fixture(100);f.create(f.dispatcher(policy(false,500,true,100,1)),"off","fixture");assertEquals(0,f.balance());
        var poor=fixture(99);assertThrows(IllegalStateException.class,()->poor.create(poor.dispatcher(policy(false,500,true,100,1)),"poor","fixture"));poor.assertEmpty();assertEquals(99,poor.balance());
    }
    @Test void chargeOffStillRequiresEligibilityWithoutLedger() throws Exception {
        var f=fixture(500);f.create(f.dispatcher(policy(true,500,false,100,1)),"free","fixture");assertEquals(500,f.balance());assertEquals(0,f.value("SELECT COUNT(*) FROM aoo_ledger"));
        var poor=fixture(499);assertThrows(IllegalStateException.class,()->poor.create(poor.dispatcher(policy(true,500,false,100,1)),"poor","fixture"));poor.assertEmpty();
    }
    @Test void independentZeroSettingsAndCustomBoundaryAreHonored() throws Exception {
        var zero=fixture(0);zero.create(zero.dispatcher(policy(false,500,false,100,1)),"both-off","fixture");assertEquals(0,zero.balance());assertEquals(0,zero.value("SELECT COUNT(*) FROM aoo_ledger"));
        var zeroCharge=fixture(0);zeroCharge.create(zeroCharge.dispatcher(policy(true,0,true,0,1)),"zero","fixture");assertEquals(0,zeroCharge.value("SELECT COUNT(*) FROM aoo_ledger"));
        var f=fixture(700);f.create(f.dispatcher(policy(true,700,true,200,3)),"custom","fixture");assertEquals(500,f.balance());
        var poor=fixture(699);assertThrows(IllegalStateException.class,()->poor.create(poor.dispatcher(policy(true,700,true,200,3)),"custom","fixture"));poor.assertEmpty();
    }
    @Test void currentPolicyAndClientActorOrPriceCannotBypassAuthority() throws Exception {
        var f=fixture(500);var missing=new ClubDispatchService(f.source,f.mapper,CLOCK,f.ids::incrementAndGet);
        assertThrows(IllegalStateException.class,()->f.create(missing,"missing","fixture"));f.assertEmpty();
        var d=f.dispatcher(DEFAULT);
        assertThrows(IllegalArgumentException.class,()->d.dispatch(ACTOR,"forged","club.CClubCreate",Map.of("clubName","fixture","actor",99,"chargeRoomCardAmount",0)));
        assertThrows(IllegalArgumentException.class,()->d.dispatch(ACTOR,"conflict","club.CClubCreate",Map.of("clubName","one","name","two")));
        assertThrows(IllegalArgumentException.class,()->f.create(d,"","fixture"));f.assertEmpty();
    }
    @Test void successfulReplayPrecedesNewPolicyOrMissingPolicyAndRejectsChangedName() throws Exception {
        var f=fixture(500);var first=f.create(f.dispatcher(DEFAULT),"same","fixture");
        var changed=f.create(f.dispatcher(policy(true,900,true,300,2)),"same","fixture");assertEquals(first,changed);
        var missing=new ClubDispatchService(f.source,f.mapper,CLOCK,f.ids::incrementAndGet);assertEquals(first,f.create(missing,"same","fixture"));
        assertThrows(IllegalArgumentException.class,()->f.create(f.dispatcher(DEFAULT),"same","other"));
        assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_club_state"));assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_ledger"));assertEquals(400,f.balance());
    }
    @Test void concurrentSameRequestCreatesOneCircleAndDebitsOnce() throws Exception {
        var f=fixture(500);var d=f.dispatcher(DEFAULT);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{start.await();return f.create(d,"same","fixture");});var b=pool.submit(()->{start.await();return f.create(d,"same","fixture");});start.countDown();assertEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
        }
        assertEquals(400,f.balance());assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_club_state"));assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void differentRequestsHandleIdentityCollisionWithoutOverwritingAnotherCircle() throws Exception {
        var f=fixture(600);var d=f.dispatcher(DEFAULT);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{start.await();return f.create(d,"one","one");});var b=pool.submit(()->{start.await();return f.create(d,"two","two");});start.countDown();assertNotEquals(a.get(5,TimeUnit.SECONDS).get("id"),b.get(5,TimeUnit.SECONDS).get("id"));
        }
        assertEquals(400,f.balance());assertEquals(2,f.value("SELECT COUNT(*) FROM aoo_club_state"));assertEquals(2,f.value("SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void concurrentChargeCompetitionCannotMakeBalanceNegative() throws Exception {
        var f=fixture(100);var d=f.dispatcher(policy(false,500,true,100,1));var start=new CountDownLatch(1);int success=0;
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=new ArrayList<Future<Boolean>>();for(String request:List.of("one","two"))tasks.add(pool.submit(()->{start.await();try{f.create(d,request,request);return true;}catch(IllegalStateException expected){return false;}}));start.countDown();for(var task:tasks)if(task.get(5,TimeUnit.SECONDS))success++;
        }
        assertEquals(1,success);assertEquals(0,f.balance());assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_club_state"));assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void persistenceFailureAfterDebitRollsBackChargeMemberClaimAndAllowsRetry() throws Exception {
        var f=fixture(500);var d=f.dispatcher(DEFAULT);f.execute("ALTER TABLE aoo_club_member ADD CONSTRAINT reject_owner CHECK (member_role<>'OWNER')");
        assertThrows(IllegalStateException.class,()->f.create(d,"failure","fixture"));f.assertEmpty();assertEquals(500,f.balance());assertEquals(0,f.value("SELECT COUNT(*) FROM aoo_club_member"));
        f.execute("ALTER TABLE aoo_club_member DROP CONSTRAINT reject_owner");f.create(d,"failure","fixture");assertEquals(400,f.balance());assertEquals(1,f.value("SELECT COUNT(*) FROM aoo_ledger"));
    }
}
