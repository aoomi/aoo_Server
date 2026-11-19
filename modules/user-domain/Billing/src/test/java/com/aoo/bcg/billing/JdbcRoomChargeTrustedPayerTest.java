package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

final class JdbcRoomChargeTrustedPayerTest {
    private final AtomicLong ids=new AtomicLong(100);
    private JdbcDataSource fixture() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=100");
        try(var c=ds.getConnection();var s=c.createStatement()) {
            s.execute("CREATE TABLE payer_lock(id BIGINT PRIMARY KEY,owner_id BIGINT)");s.execute("INSERT INTO payer_lock VALUES(1,7)");
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");s.execute("INSERT INTO aoo_currency_balance VALUES(7,'ROOM_CARD',0,500,0,CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR,channel_code VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT PRIMARY KEY,environment VARCHAR)");s.execute("INSERT INTO aoo_room_cost_publication VALUES(1,'PRODUCTION')");
            s.execute("CREATE TABLE aoo_room_cost_active(pointer_name VARCHAR PRIMARY KEY,publication_id BIGINT)");s.execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1)");
            s.execute("CREATE TABLE aoo_room_cost_policy(publication_id BIGINT,policy_version BIGINT,content_hash VARCHAR,cost_minor BIGINT,currency_code VARCHAR,free_policy_ref VARCHAR,refund_policy_ref VARCHAR,tier_minor BIGINT,round_count BIGINT,game_id BIGINT,play_version VARCHAR,region_code VARCHAR,measure_kind VARCHAR,measure_value INT,player_count INT,payer_mode VARCHAR,billing_kind VARCHAR,billing_context VARCHAR,status VARCHAR,confirmation_status VARCHAR)");
            for(String context:new String[]{"CLUB","HALL"})s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+"a".repeat(64)+"',100,'ROOM_CARD',NULL,'r5',0,0,1,'test-v1','TEST','ROUND_COUNT',8,4,'"+(context.equals("CLUB")?"CLUB":"OWNER")+"','FIXED','"+context+"','ACTIVE','CONFIRMED')");
            s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP,billing_operation_key VARCHAR UNIQUE,command_hash VARCHAR,actor_id BIGINT,room_attempt VARCHAR,billing_kind VARCHAR,billing_unit VARCHAR,policy_publication_id BIGINT,policy_snapshot VARCHAR,debit_business_id VARCHAR,refunded_minor BIGINT DEFAULT 0,confirmation_id VARCHAR,confirmation_hash VARCHAR)");
        }return ds;
    }
    private JdbcRoomChargeCoordinator coordinator(DataSource ds,JdbcRoomChargeCoordinator.TrustedPayerVerifier v){return new JdbcRoomChargeCoordinator(ds,new JdbcBillingService(ds,ids::incrementAndGet,Clock.systemUTC()),new JdbcRoomCostPolicyRepository(ds,false),Clock.systemUTC(),v);}
    private RoomChargeOperation op(boolean hall){return new RoomChargeOperation(1,"attempt","unit",7,7,1,"test-v1","TEST",RoomChargeOperation.Kind.FIXED,hall?RoomChargeOperation.Context.HALL:RoomChargeOperation.Context.CLUB,hall?RoomChargeOperation.Payer.OWNER:RoomChargeOperation.Payer.CLUB,"ROUND_COUNT",8,4,0);}
    private JdbcRoomChargeCoordinator.Subject subject(){return new JdbcRoomChargeCoordinator.Subject(9,7,1);}
    private long scalar(DataSource ds,String sql)throws Exception{try(var c=ds.getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    @Test void successAndReplayVerifySameTransactionAndDebitOnce()throws Exception{
        var ds=fixture();var calls=new AtomicInteger();var closed=new java.util.ArrayList<Connection>();
        var charge=coordinator(ds,(c,o,sub)->{assertFalse(c.getAutoCommit());assertEquals(subject(),sub);closed.add(c);calls.incrementAndGet();try(var s=c.createStatement();var r=s.executeQuery("SELECT owner_id FROM payer_lock WHERE id=1 FOR UPDATE")){assertTrue(r.next());assertEquals(7,r.getLong(1));}});
        var a=charge.reserve(op(false),subject());assertEquals(a,charge.reserve(op(false),subject()));assertEquals(2,calls.get());assertEquals(400,scalar(ds,"SELECT balance FROM aoo_currency_balance"));assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));for(var c:closed)assertTrue(c.isClosed());
    }
    @Test void denialBeforePolicyAndRollbackLeavesNoFinancialWrites()throws Exception{
        var ds=fixture();try(var c=ds.getConnection();var s=c.createStatement()){s.execute("DELETE FROM aoo_room_cost_policy");}
        var charge=coordinator(ds,(c,o,sub)->{try(var s=c.createStatement()){s.execute("UPDATE payer_lock SET owner_id=99 WHERE id=1");}throw new SQLException("denied","23000");});
        var error=assertThrows(IllegalStateException.class,()->charge.reserve(op(false),subject()));assertInstanceOf(SQLException.class,error.getCause());assertEquals(7,scalar(ds,"SELECT owner_id FROM payer_lock"));assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_room_billing_reservation"));assertEquals(500,scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }
    @Test void callbackSql23CannotReturnExistingReceiptAndFindRemainsReadable()throws Exception{
        var ds=fixture();var good=coordinator(ds,(c,o,s)->{});var original=good.reserve(op(false),subject());
        var denied=coordinator(ds,(c,o,s)->{throw new SQLException("denied","23000");});assertThrows(IllegalStateException.class,()->denied.reserve(op(false),subject()));assertEquals(original,denied.find(original.requestId()).orElseThrow());assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void changedSubjectOnReplayIsNotBypassed()throws Exception{
        var ds=fixture();var charge=coordinator(ds,(c,o,s)->{if(!s.equals(subject()))throw new IllegalArgumentException("scope drift");});charge.reserve(op(false),subject());assertThrows(IllegalArgumentException.class,()->charge.reserve(op(false),new JdbcRoomChargeCoordinator.Subject(9,7,2)));assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void oldHallCompatibilityButNoOrganizationBypass()throws Exception{
        var ds=fixture();var charge=new JdbcRoomChargeCoordinator(ds,new JdbcBillingService(ds,ids::incrementAndGet,Clock.systemUTC()),new JdbcRoomCostPolicyRepository(ds,false),Clock.systemUTC());assertThrows(IllegalArgumentException.class,()->charge.reserve(op(false)));assertThrows(IllegalStateException.class,()->charge.reserve(op(false),subject()));assertEquals(100,charge.reserve(op(true)).amount());
        assertThrows(IllegalArgumentException.class,()->coordinator(ds,(c,o,s)->{}).reserve(op(true),new JdbcRoomChargeCoordinator.Subject(9,8,0)));
    }
    @Test void verifierLockPersistsUntilCoordinatorCommit()throws Exception{
        var ds=fixture();var charge=coordinator(ds,(c,o,s)->{try(var q=c.createStatement();var r=q.executeQuery("SELECT owner_id FROM payer_lock WHERE id=1 FOR UPDATE")){assertTrue(r.next());}try(var other=ds.getConnection();var q=other.createStatement()){other.setAutoCommit(false);assertThrows(SQLException.class,()->q.executeUpdate("UPDATE payer_lock SET owner_id=8 WHERE id=1"));other.rollback();}});charge.reserve(op(false),subject());try(var c=ds.getConnection();var s=c.createStatement()){assertEquals(1,s.executeUpdate("UPDATE payer_lock SET owner_id=8 WHERE id=1"));}
    }
    /** Real H2 duplicate is normalized to MySQL's 23000 at this test JDBC boundary only. */
    private DataSource insertRace(JdbcDataSource ds,Runnable winner){var once=new AtomicBoolean();return (DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(p,m,a)->{try{Object result=m.invoke(ds,a);if(!m.getName().equals("getConnection"))return result;Connection c=(Connection)result;return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(cp,cm,ca)->{try{Object cr=cm.invoke(c,ca);if(cm.getName().equals("prepareStatement")&&((String)ca[0]).startsWith("INSERT INTO aoo_room_billing_reservation")){PreparedStatement statement=(PreparedStatement)cr;return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(sp,sm,sa)->{if(sm.getName().equals("executeUpdate")&&once.compareAndSet(false,true))winner.run();try{return sm.invoke(statement,sa);}catch(InvocationTargetException ex){if(ex.getCause() instanceof SQLException sql&&"23505".equals(sql.getSQLState()))throw new SQLException("duplicate","23000",sql);throw ex.getCause();}});}return cr;}catch(InvocationTargetException ex){throw ex.getCause();}});}catch(InvocationTargetException ex){throw ex.getCause();}});}
    @Test void realInsertLoserReverifiesInNewTransactionBeforeReadingWinner()throws Exception{
        var ds=fixture();var calls=new AtomicInteger();var connections=new java.util.ArrayList<Connection>();var race=insertRace(ds,()->coordinator(ds,(c,o,s)->{}).reserve(op(false),subject()));
        var charge=coordinator(race,(c,o,s)->{connections.add(c);if(calls.incrementAndGet()==2)throw new IllegalArgumentException("owner changed");});assertThrows(IllegalArgumentException.class,()->charge.reserve(op(false),subject()));assertEquals(2,calls.get());assertNotSame(connections.get(0),connections.get(1));assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(400,scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }
    @Test void eachSubjectFieldPersistsAgainstSameOwnerAcceptingVerifier()throws Exception {
        var ds=fixture();var c=coordinator(ds,(conn,o,sub)->{});var operation=op(false);var original=c.reserve(operation,subject());
        for(var changed:new JdbcRoomChargeCoordinator.Subject[]{new JdbcRoomChargeCoordinator.Subject(10,7,1),new JdbcRoomChargeCoordinator.Subject(9,8,1),new JdbcRoomChargeCoordinator.Subject(9,7,2)}) {
            assertThrows(IllegalArgumentException.class,()->coordinator(ds,(conn,o,sub)->{}).reserve(operation,changed));
        }
        assertEquals(subject().commandHash(operation),original.commandHash());assertNotEquals(operation.commandHash(),original.commandHash());
        assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(400,scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }
    @Test void sameSubjectSurvivesNewCoordinatorAndConnectionWithoutDoubleDebit()throws Exception {
        var ds=fixture();var first=coordinator(ds,(conn,o,sub)->{}).reserve(op(false),subject());
        var restarted=coordinator(ds,(conn,o,sub)->{});assertEquals(first,restarted.reserve(op(false),subject()));
        try(var conn=ds.getConnection();var q=conn.prepareStatement("SELECT command_hash FROM aoo_room_billing_reservation WHERE request_id=?")){q.setString(1,first.requestId());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(subject().commandHash(op(false)),r.getString(1));}}
        assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void originalRefundIgnoresLaterOwnerAndPublishedPrice()throws Exception {
        var ds=fixture();var c=coordinator(ds,(conn,o,sub)->{try(var q=conn.createStatement();var r=q.executeQuery("SELECT owner_id FROM payer_lock WHERE id=1 FOR UPDATE")){assertTrue(r.next());if(r.getLong(1)!=o.payerId())throw new IllegalArgumentException("owner drift");}});
        var original=c.reserve(op(false),subject());
        try(var conn=ds.getConnection();var q=conn.createStatement()){q.execute("CREATE TABLE aoo_room_billing_refund(refund_id VARCHAR PRIMARY KEY,request_id VARCHAR,command_hash VARCHAR,amount BIGINT,credit_business_id VARCHAR,evidence_json VARCHAR,created_at TIMESTAMP)");q.execute("UPDATE payer_lock SET owner_id=8");q.execute("UPDATE aoo_room_cost_policy SET cost_minor=300");}
        assertThrows(IllegalArgumentException.class,()->c.reserve(op(false),subject()));
        var evidence=new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.BUSINESS_FAILED,"failed",RoomChargeOperation.hash("TEST_ONLY_FAILURE"));
        var refund=c.refund(original.requestId(),"refund-original",evidence);assertEquals(100,refund.amount());assertEquals(500,scalar(ds,"SELECT balance FROM aoo_currency_balance WHERE player_id=7"));assertEquals(100,c.find(original.requestId()).orElseThrow().amount());assertEquals(refund,c.refund(original.requestId(),"refund-original",evidence));assertEquals(2,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void legacyOperationOnlyReceiptCannotBeAdoptedAsSubjectBound()throws Exception {
        var ds=fixture();var legacy=new JdbcRoomChargeCoordinator(ds,new JdbcBillingService(ds,ids::incrementAndGet,Clock.systemUTC()),new JdbcRoomCostPolicyRepository(ds,false),Clock.systemUTC());var old=legacy.reserve(op(true));
        assertEquals(op(true).commandHash(),old.commandHash());assertThrows(IllegalArgumentException.class,()->coordinator(ds,(c,o,sub)->{}).reserve(op(true),new JdbcRoomChargeCoordinator.Subject(7,7,0)));
        assertEquals(old,legacy.find(old.requestId()).orElseThrow());assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(400,scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }

}
