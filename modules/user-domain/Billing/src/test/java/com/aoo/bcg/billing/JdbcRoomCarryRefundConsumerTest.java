package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomCarryReceipt.*;

final class JdbcRoomCarryRefundConsumerTest {
    static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-17T00:00:00.123456789Z"),ZoneOffset.UTC);
    static String h(String x){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new AssertionError(e);}}
    static final Attempt A=new Attempt(h("attempt"),"native-original-request",h("native-request"),20,100,"native-room:1",1,0,10000,Kind.INITIAL);
    static final RoomChargeOperation OP=new RoomChargeOperation(100,"native-room:1","unit-initial",10,10,1,"test-v1","TEST",RoomChargeOperation.Kind.CARRY,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.OWNER,"CARRY_MINOR_PER_TIER",10000,4,10000);
    static final Original ORIGINAL=new Original(OP,new JdbcRoomChargeCoordinator.Subject(20,10,0));
    final JdbcRoomCarryReceiptRepository repo=new JdbcRoomCarryReceiptRepository(CLOCK);
    final AtomicLong ids=new AtomicLong(1000);
    JdbcDataSource db()throws Exception {
        var d=new JdbcDataSource();d.setURL("jdbc:h2:mem:carry"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000");
        try(var c=d.getConnection();var s=c.createStatement()){
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");s.execute("INSERT INTO aoo_currency_balance VALUES(10,'ROOM_CARD',0,1000,0,CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR,channel_code VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT PRIMARY KEY,environment VARCHAR)");s.execute("INSERT INTO aoo_room_cost_publication VALUES(1,'PRODUCTION')");
            s.execute("CREATE TABLE aoo_room_cost_active(pointer_name VARCHAR PRIMARY KEY,publication_id BIGINT)");s.execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1)");
            s.execute("CREATE TABLE aoo_room_cost_policy(publication_id BIGINT,policy_version BIGINT,content_hash VARCHAR,cost_minor BIGINT,currency_code VARCHAR,free_policy_ref VARCHAR,refund_policy_ref VARCHAR,tier_minor BIGINT,round_count BIGINT,game_id BIGINT,play_version VARCHAR,region_code VARCHAR,measure_kind VARCHAR,measure_value INT,player_count INT,payer_mode VARCHAR,billing_kind VARCHAR,billing_context VARCHAR,status VARCHAR,confirmation_status VARCHAR)");
            s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+h("policy")+"',100,'ROOM_CARD',NULL,'XQP_R5',10000,10000,1,'test-v1','TEST','CARRY_MINOR_PER_TIER',10000,4,'OWNER','CARRY','HALL','ACTIVE','CONFIRMED')");
            s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP,billing_operation_key VARCHAR UNIQUE,command_hash VARCHAR,actor_id BIGINT,room_attempt VARCHAR,billing_kind VARCHAR,billing_unit VARCHAR,policy_publication_id BIGINT,policy_snapshot VARCHAR,debit_business_id VARCHAR,refunded_minor BIGINT DEFAULT 0,confirmation_id VARCHAR,confirmation_hash VARCHAR)");
            s.execute("CREATE TABLE aoo_room_billing_refund(refund_id VARCHAR PRIMARY KEY,request_id VARCHAR,command_hash VARCHAR,amount BIGINT,credit_business_id VARCHAR,evidence_json VARCHAR,created_at TIMESTAMP)");
            // Explicit local SQL-backed original association, not a claim that a live producer exists.
            s.execute("CREATE TABLE source_original(attempt_id VARCHAR PRIMARY KEY,request_id VARCHAR,request_hash VARCHAR,actor BIGINT,room BIGINT,amount BIGINT,original_hash VARCHAR)");
            try(var q=c.prepareStatement("INSERT INTO source_original VALUES(?,?,?,?,?,?,?)")){q.setString(1,A.attemptId());q.setString(2,A.originalRequestId());q.setString(3,A.requestHash());q.setLong(4,20);q.setLong(5,100);q.setLong(6,10000);q.setString(7,ORIGINAL.subject().commandHash(OP));q.executeUpdate();}
            s.execute("CREATE TABLE source_snapshot(room BIGINT PRIMARY KEY,instance VARCHAR,epoch BIGINT,fence BIGINT,version BIGINT,snapshot TEXT,response TEXT,trigger_id VARCHAR,writer_kind VARCHAR)");
            RunScript.execute(c,new StringReader(Files.readString(Path.of("server/database/migrations/V20261017_06__room_card_carry_effect_receipt.sql"))));
        }
        charges(d).reserve(OP,ORIGINAL.subject());return d;
    }
    JdbcRoomChargeCoordinator charges(JdbcDataSource d){return new JdbcRoomChargeCoordinator(d,new JdbcBillingService(d,ids::incrementAndGet,CLOCK),new JdbcRoomCostPolicyRepository(d,false),CLOCK,(c,o,s)->{if(s.creatorId()!=10||s.initiatingActorId()!=20)throw new SQLException("untrusted original actor");});}
    JdbcRoomCarryRefundConsumer consumer(JdbcDataSource d){return new JdbcRoomCarryRefundConsumer(repo,charges(d));}
    void bind(Connection c)throws SQLException {repo.bindOriginal(c,A,ORIGINAL,(conn,a,o)->{
        try(var s=conn.prepareStatement("SELECT request_id,request_hash,actor,room,amount,original_hash FROM source_original WHERE attempt_id=? FOR UPDATE")){s.setString(1,a.attemptId());try(var r=s.executeQuery()){
            if(!r.next()||!r.getString(1).equals(a.originalRequestId())||!r.getString(2).equals(a.requestHash())||r.getLong(3)!=a.nativeActorId()||r.getLong(4)!=a.roomId()||r.getLong(5)!=a.amount()||!r.getString(6).equals(o.subject().commandHash(o.operation())))throw new SQLException("original source denied","23000");}}
    });}
    Batch batch(Stage... stages){var es=new ArrayList<Effect>();long n=0;for(Stage stage:stages){n++;es.add(new Effect(h("effect"+n+stage),A,stage,n,n));}return new Batch(h(repo.encode(es)),n,es);}
    Proof persist(Connection c,Batch b,WriterKind kind)throws SQLException {
        String snapshot=repo.encode(b),response=kind==WriterKind.COMMAND?"actual-result:"+b.batchHash():null,trigger=kind==WriterKind.COMMAND?"native-original-request":"later-system-timeout";
        try(var s=c.prepareStatement("MERGE INTO source_snapshot KEY(room) VALUES(100,'native-room:1',1,5,?,?,?,?,?)")){s.setLong(1,b.throughOrdinal());s.setString(2,snapshot);s.setString(3,response);s.setString(4,trigger);s.setString(5,kind.name());s.executeUpdate();}
        // Read stored column bytes, rather than assuming the writer's serialized representation.
        try(var s=c.createStatement();var r=s.executeQuery("SELECT snapshot,response FROM source_snapshot WHERE room=100")){r.next();return new Proof(kind,trigger,"native-room:1",1,5,b.throughOrdinal(),h(r.getString(1)),r.getString(2)==null?null:h(r.getString(2)));}
    }
    final StoredProofVerifier proof=(c,b,p)->{
        try(var s=c.prepareStatement("SELECT instance,epoch,fence,version,snapshot,response,trigger_id,writer_kind FROM source_snapshot WHERE room=100 FOR UPDATE");var r=s.executeQuery()){
            if(!r.next()||!r.getString(1).equals(p.instanceId())||r.getLong(2)!=p.epoch()||r.getLong(3)!=p.fence()||r.getLong(4)!=p.stateVersion()||!h(r.getString(5)).equals(p.snapshotHash())||!r.getString(5).equals(repo.encode(b))||!Objects.equals(r.getString(6)==null?null:h(r.getString(6)),p.responseHash())||!r.getString(7).equals(p.triggerId())||!r.getString(8).equals(p.writerKind().name()))throw new SQLException("stored proof mismatch","23000");}
    };
    void finalizeAt(Connection c,Batch b,WriterKind kind)throws SQLException{repo.stageBatch(c,b);repo.finalizeBatch(c,b,persist(c,b,kind),proof);repo.requireReadyToCommit(c,b.batchHash());}
    long value(JdbcDataSource d,String sql)throws Exception{try(var c=d.getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    long balance(JdbcDataSource d)throws Exception{return value(d,"SELECT balance FROM aoo_currency_balance WHERE player_id=10");}
    void consume(Connection c,JdbcDataSource d,Batch b)throws SQLException{consumer(d).consumeOriginal(c,b.effects().getLast().effectId(),ORIGINAL,proof);}
    @Test void requestedAppliedNeverTakeNewMoney()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);consume(c,d,b);c.commit();}assertEquals(900,balance(d));assertEquals(1,value(d,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals("RESERVED",charges(d).find(OP.requestId()).orElseThrow().state());}
    @Test void usedOnlyConfirmsOriginalAndReplays()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED,Stage.USED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);consume(c,d,b);consume(c,d,b);c.commit();}assertEquals("CONFIRMED",charges(d).find(OP.requestId()).orElseThrow().state());assertEquals(900,balance(d));assertEquals(1,value(d,"SELECT COUNT(*) FROM aoo_ledger"));}
    @Test void unusedRefundsOriginalExactlyOnceAfterPriceChange()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED,Stage.TERMINATED_UNUSED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.SYSTEM_SNAPSHOT);try(var s=c.createStatement()){s.execute("UPDATE aoo_room_cost_policy SET cost_minor=999,status='DISABLED'");}consume(c,d,b);consume(c,d,b);c.commit();}assertEquals(1000,balance(d));assertEquals(2,value(d,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(100,value(d,"SELECT amount FROM aoo_room_billing_refund"));}
    @Test void requestedTerminationCanRefundWithoutApplied()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.TERMINATED_UNUSED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);consume(c,d,b);c.commit();}assertEquals(1000,balance(d));}
    @Test void callerRollbackRestoresReceiptsLedgerAndBalance()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.TERMINATED_UNUSED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);consume(c,d,b);c.rollback();}assertEquals(900,balance(d));assertEquals(0,value(d,"SELECT COUNT(*) FROM aoo_room_carry_effect"));assertEquals(1,value(d,"SELECT COUNT(*) FROM aoo_ledger"));}
    @Test void absentLinkIsPermanentlyFactOnly()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.TERMINATED_UNUSED);try(var c=d.getConnection()){c.setAutoCommit(false);finalizeAt(c,b,WriterKind.COMMAND);assertThrows(IllegalArgumentException.class,()->bind(c));assertThrows(IllegalArgumentException.class,()->consume(c,d,b));c.commit();}assertEquals(900,balance(d));assertEquals(0,value(d,"SELECT COUNT(*) FROM aoo_room_billing_refund"));}
    @Test void stagedNotFinalCannotConsumeOrPassCommitGuard()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);repo.stageBatch(c,b);assertThrows(IllegalStateException.class,()->repo.requireReadyToCommit(c,b.batchHash()));assertThrows(IllegalStateException.class,()->consume(c,d,b));c.rollback();}}
    @Test void finalStoredBytesMustStillMatchAtConsumption()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED,Stage.USED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);try(var s=c.createStatement()){s.execute("UPDATE source_snapshot SET snapshot='tampered'");}SQLException e=assertThrows(SQLException.class,()->consume(c,d,b));assertEquals("23000",e.getSQLState());c.rollback();}assertEquals("RESERVED",charges(d).find(OP.requestId()).orElseThrow().state());}
    @Test void writerKindsDoNotInventSystemResponses()throws Exception {assertThrows(IllegalArgumentException.class,()->new Proof(WriterKind.SYSTEM_SNAPSHOT,"tick","native-room:1",1,5,1,h("x"),h("fake")));assertThrows(IllegalArgumentException.class,()->new Proof(WriterKind.COMMAND,"cmd","native-room:1",1,5,1,h("x"),null));var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED,Stage.USED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.SYSTEM_SNAPSHOT);consume(c,d,b);c.commit();}assertEquals("CONFIRMED",charges(d).find(OP.requestId()).orElseThrow().state());}
    @Test void usedAndUnusedAreMutuallyExclusivePerAttempt()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED,Stage.USED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);consume(c,d,b);c.commit();}try(var c=d.getConnection()){c.setAutoCommit(false);var e=new Effect(h("late-unused"),A,Stage.TERMINATED_UNUSED,4,4);assertThrows(IllegalStateException.class,()->repo.stageBatch(c,new Batch(h("late"),4,List.of(e))));c.rollback();}assertEquals(900,balance(d));}
    @Test void conflictingOriginalActorAmountAndRuntimeReject()throws Exception {var d=db();try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);var wrong=new Attempt(A.attemptId(),A.originalRequestId(),A.requestHash(),20,100,A.instanceId(),1,0,9999,Kind.INITIAL);assertThrows(IllegalArgumentException.class,()->repo.bindOriginal(c,wrong,ORIGINAL,(x,a,o)->{}));var e=new Effect(h("wrong-request"),new Attempt(A.attemptId(),"other-request",A.requestHash(),20,100,A.instanceId(),1,0,10000,Kind.INITIAL),Stage.REQUESTED,1,1);assertThrows(IllegalArgumentException.class,()->repo.stageBatch(c,new Batch(h("wrong-batch"),1,List.of(e))));c.rollback();}}
    @Test void originalVerifierSql23DoesNotBecomeSuccess()throws Exception {var d=db();try(var c=d.getConnection()){c.setAutoCommit(false);SQLException denied=assertThrows(SQLException.class,()->repo.bindOriginal(c,A,ORIGINAL,(x,a,o)->{throw new SQLException("denied","23000");}));assertEquals("denied",denied.getMessage());c.commit();}assertEquals(0,value(d,"SELECT COUNT(*) FROM aoo_room_carry_attempt"));}
    @Test void millisStoredAndCallerAutoCommitRejected()throws Exception {var d=db();var b=batch(Stage.REQUESTED);try(var c=d.getConnection()){assertThrows(IllegalStateException.class,()->repo.stageBatch(c,b));c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);c.commit();}try(var c=d.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT created_at FROM aoo_room_carry_batch")){r.next();assertEquals(123000000,r.getTimestamp(1).toInstant().getNano());}}
    @Test void concurrentEstablishedSnapshotCannotWriteOppositeTerminal()throws Exception {var d=db();var b=batch(Stage.REQUESTED,Stage.APPLIED);try(var c=d.getConnection()){c.setAutoCommit(false);bind(c);finalizeAt(c,b,WriterKind.COMMAND);c.commit();}
        try(var one=d.getConnection();var two=d.getConnection()){one.setAutoCommit(false);two.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);two.setAutoCommit(false);try(var s=two.createStatement();var r=s.executeQuery("SELECT stage FROM aoo_room_carry_attempt")){r.next();assertEquals("APPLIED",r.getString(1));}
            var used=new Batch(h("use-batch"),3,List.of(new Effect(h("use-effect"),A,Stage.USED,3,3)));repo.stageBatch(one,used);repo.finalizeBatch(one,used,persist(one,used,WriterKind.SYSTEM_SNAPSHOT),proof);consume(one,d,used);
            ExecutorService pool=Executors.newSingleThreadExecutor();try{var task=pool.submit(()->{try{var no=new Batch(h("end-batch"),4,List.of(new Effect(h("end-effect"),A,Stage.TERMINATED_UNUSED,4,4)));repo.stageBatch(two,no);return false;}catch(SQLException|IllegalStateException expected){return true;}});Thread.sleep(80);assertFalse(task.isDone());one.commit();assertTrue(task.get(2,TimeUnit.SECONDS));two.rollback();}finally{pool.shutdownNow();}}
        assertEquals(900,balance(d));assertEquals(0,value(d,"SELECT COUNT(*) FROM aoo_room_billing_refund"));}
}
