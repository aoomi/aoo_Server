package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomAaParticipation.*;

final class JdbcRoomAaParticipationTest {
    static final Instant TIME=Instant.parse("2026-10-17T00:00:00.123456789Z");
    static final SessionKey KEY=new SessionKey(100,"native-room:1",1);
    static final Context CTX=new Context(10,1,"test-v1","TEST","ROUND_COUNT",8,4,new RoomBillingSourceBinding.AuthorityReference(11,12,"b".repeat(64),"TEST"));
    final AtomicLong ids=new AtomicLong(1000);
    static CommandKey cmd(String request){return new CommandKey(request,RoomChargeOperation.hash(request),20);}
    static void ddl(Connection c,String file)throws Exception {RunScript.execute(c,new StringReader(Files.readString(Path.of("server/database/migrations",file))));}
    JdbcDataSource fixture()throws Exception {return fixture(true);}
    JdbcDataSource fixture(boolean create)throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:aa"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        try(var c=ds.getConnection();var s=c.createStatement()) {
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");s.execute("INSERT INTO aoo_currency_balance VALUES(20,'ROOM_CARD',0,1000,0,CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR,channel_code VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT PRIMARY KEY,environment VARCHAR)");s.execute("INSERT INTO aoo_room_cost_publication VALUES(1,'PRODUCTION')");
            s.execute("CREATE TABLE aoo_room_cost_active(pointer_name VARCHAR PRIMARY KEY,publication_id BIGINT)");s.execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1)");
            s.execute("CREATE TABLE aoo_room_cost_policy(publication_id BIGINT,policy_version BIGINT,content_hash VARCHAR,cost_minor BIGINT,currency_code VARCHAR,free_policy_ref VARCHAR,refund_policy_ref VARCHAR,tier_minor BIGINT,round_count BIGINT,game_id BIGINT,play_version VARCHAR,region_code VARCHAR,measure_kind VARCHAR,measure_value INT,player_count INT,payer_mode VARCHAR,billing_kind VARCHAR,billing_context VARCHAR,status VARCHAR,confirmation_status VARCHAR)");
            s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+"a".repeat(64)+"',100,'ROOM_CARD',NULL,'r5',0,0,1,'test-v1','TEST','ROUND_COUNT',8,4,'AA','FIXED','HALL','ACTIVE','CONFIRMED')");
            s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP,billing_operation_key VARCHAR UNIQUE,command_hash VARCHAR,actor_id BIGINT,room_attempt VARCHAR,billing_kind VARCHAR,billing_unit VARCHAR,policy_publication_id BIGINT,policy_snapshot VARCHAR,debit_business_id VARCHAR,refunded_minor BIGINT DEFAULT 0,confirmation_id VARCHAR,confirmation_hash VARCHAR)");
            s.execute("CREATE TABLE aoo_room_billing_refund(refund_id VARCHAR PRIMARY KEY,request_id VARCHAR,command_hash VARCHAR,amount BIGINT,credit_business_id VARCHAR,evidence_json VARCHAR,created_at TIMESTAMP)");
            ddl(c,"V20261017_01__room_card_billing_intent_journal.sql");ddl(c,"V20261016_91__room_card_pricing_publication_provenance.sql");ddl(c,"V20261017_04__room_card_aa_participation.sql");ddl(c,"V20261017_05__room_card_aa_creation.sql");
            s.execute("CREATE TABLE source_room(room_id BIGINT PRIMARY KEY,creator BIGINT,instance VARCHAR,epoch BIGINT,fence BIGINT,version BIGINT,snapshot VARCHAR,seat INT,context_hash VARCHAR)");
            try(var q=c.prepareStatement("INSERT INTO source_room VALUES(100,10,'native-room:1',1,5,1,'before',-1,?)")){q.setString(1,CTX.contextHash());q.executeUpdate();}
            s.execute("CREATE TABLE source_result(request_id VARCHAR PRIMARY KEY,request_hash VARCHAR,actor BIGINT,response VARCHAR,status VARCHAR)");
        }
        provenance(ds,false);if(create)createSource(ds);return ds;
    }
    static RoomAaCreation.Key createKey(){return new RoomAaCreation.Key("create-request-100",RoomChargeOperation.hash("create"),10);}
    static RoomAaCreation.Context createContext(){return new RoomAaCreation.Context(KEY.roomId(),CTX.gameId(),CTX.playVersion(),CTX.regionCode(),CTX.measureKind(),CTX.measureValue(),CTX.playerCount(),CTX.authority());}
    static RoomAaCreation.RuntimeProof createProof(){return new RoomAaCreation.RuntimeProof(KEY.roomInstanceId(),KEY.lifecycleEpoch(),5,1,10,1,"test-v1");}
    void createSource(JdbcDataSource ds)throws Exception {try(var c=ds.getConnection()){c.setAutoCommit(false);var repo=new JdbcRoomAaCreationRepository(charges(ds,TIME),Clock.fixed(TIME,ZoneOffset.UTC));var b=repo.bind(c,createKey(),createContext());repo.recordCommit(c,b,createProof());c.commit();}}
    void provenance(JdbcDataSource ds,boolean free)throws Exception {
        String key=RoomCardPricingProvenance.policyKey(new RoomCardPricingImportValidator.PriceKey(new RoomCardPricingImportValidator.IdentityKey(1,"TEST_ONLY","test-v1","TEST"),"FIXED","HALL",4,"ROUND_COUNT",8,"AA",0));
        try(var c=ds.getConnection()){c.setAutoCommit(false);new JdbcRoomCardPricingProvenanceRepository().save(c,new RoomCardPricingProvenance(1,key,"v1","a".repeat(64),"c".repeat(64),new RoomCardPricingImportValidator.SourceBinding(RoomCardPricingImportValidator.SourceClass.CURRENT_PUBLISHED,11,12,"b".repeat(64)),free?"FREE":null,free?TIME.minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS):null,free?TIME.plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS):null,false));c.commit();}
    }
    JdbcRoomChargeCoordinator charges(JdbcDataSource ds,Instant time){var clock=Clock.fixed(time,ZoneOffset.UTC);return new JdbcRoomChargeCoordinator(ds,new JdbcBillingService(ds,ids::incrementAndGet,clock),new JdbcRoomCostPolicyRepository(ds,false),clock,(c,o,s)->{assertFalse(c.getAutoCommit());assertEquals(o.actorId(),o.payerId());assertEquals(20,s.initiatingActorId());assertEquals(10,s.creatorId());});}
    class Verifier implements JdbcRoomAaParticipationService.TrustedSourceVerifier {
        boolean deny;
        public PreSeatState verifyBefore(Connection c,SessionKey k,Context ctx,CommandKey command)throws SQLException {
            if(deny)throw new SQLException("fixture denied","23000");
            try(var s=c.prepareStatement("SELECT creator,instance,epoch,fence,version,snapshot,seat,context_hash FROM source_room WHERE room_id=? FOR UPDATE")) {
                s.setLong(1,k.roomId());try(var r=s.executeQuery()){if(!r.next()||r.getLong(1)!=ctx.creatorId()||!r.getString(2).equals(k.roomInstanceId())||r.getLong(3)!=k.lifecycleEpoch()||!r.getString(8).equals(ctx.contextHash())||command.authenticatedActorId()!=20)throw new IllegalArgumentException("fixture source denied");return new PreSeatState(r.getInt(7),r.getLong(4),r.getLong(5),RoomChargeOperation.hash(r.getString(6)));}
            }
        }
        public void verifyPersistedSeat(Connection c,SessionKey k,Context ctx,CommandKey command,SeatProof p)throws SQLException {
            var actual=verifyBefore(c,k,ctx,command);
            try(var s=c.prepareStatement("SELECT request_hash,actor,response,status FROM source_result WHERE request_id=?")){s.setString(1,command.requestId());try(var r=s.executeQuery()){
                if(!r.next()||!r.getString(1).equals(command.requestHash())||r.getLong(2)!=p.actorId()||!"COMPLETED".equals(r.getString(4))||!RoomChargeOperation.hash(r.getString(3)).equals(p.responseHash())||actual.actorSeatBefore()!=p.seatId()||actual.fencingToken()!=p.fencingToken()||actual.stateVersion()!=p.stateVersion()||!actual.snapshotHash().equals(p.snapshotHash()))throw new IllegalArgumentException("fixture persisted bytes mismatch");
            }}
        }
    }
    JdbcRoomAaParticipationService service(JdbcDataSource ds,Verifier verifier){return new JdbcRoomAaParticipationService(charges(ds,TIME),Clock.fixed(TIME,ZoneOffset.UTC),verifier);}
    SeatProof persist(Connection c,Preparation p)throws Exception {
        long version=p.preState().stateVersion()+1;
        try(var s=c.prepareStatement("UPDATE source_room SET version=?,snapshot=?,seat=0 WHERE room_id=100")){s.setLong(1,version);s.setString(2,"after:"+p.command().requestId());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO source_result VALUES(?,?,20,?,'COMPLETED')")){s.setString(1,p.command().requestId());s.setString(2,p.command().requestHash());s.setString(3,"response:"+p.command().requestId());s.executeUpdate();}
        return new SeatProof(p.command().requestId(),p.command().requestHash(),20,0,KEY.roomInstanceId(),KEY.lifecycleEpoch(),5,version,RoomChargeOperation.hash("after:"+p.command().requestId()),RoomChargeOperation.hash("response:"+p.command().requestId()));
    }
    Preparation seated(JdbcDataSource ds,String request)throws Exception {try(var c=ds.getConnection()){c.setAutoCommit(false);var service=service(ds,new Verifier());var p=service.prepare(c,KEY,CTX,cmd(request));service.commitSeated(c,p,persist(c,p));c.commit();return p;}}
    long scalar(JdbcDataSource ds,String sql)throws Exception{try(var c=ds.getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    void sql(JdbcDataSource ds,String sql)throws Exception {try(var c=ds.getConnection();var s=c.createStatement()){s.execute(sql);}}
    @Test void actorDistinctCreatorChargesOnceAndRestartReplays()throws Exception {
        var ds=fixture();var p=seated(ds,"sit1");assertEquals(900,scalar(ds,"SELECT balance FROM aoo_currency_balance"));assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
        try(var c=ds.getConnection()){c.setAutoCommit(false);var s=service(ds,new Verifier());var replay=s.prepare(c,KEY,CTX,cmd("sit1"));assertEquals(Disposition.COMMAND_REPLAY,replay.disposition());assertEquals(p.intent(),replay.intent());assertEquals(0,replay.replayReceipt().createdAt().getNano()%1_000_000);assertTrue(s.findReceipt(c,KEY,CTX,cmd("sit1")).isPresent());c.commit();}
        assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void paidNewRequestUsesOriginalPriceThenRefundCreatesNewAttempt()throws Exception {
        var ds=fixture();var first=seated(ds,"first");sql(ds,"UPDATE aoo_room_cost_policy SET cost_minor=250");var second=seated(ds,"second");assertEquals(Disposition.REUSE_PAID,second.disposition());assertEquals(first.intent(),second.intent());assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
        charges(ds,TIME).refund(first.charge().requestId(),"refund1",new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.POLICY_ALLOWED,"allowed",RoomChargeOperation.hash("proof")));
        sql(ds,"UPDATE source_room SET seat=-1");
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->service(ds,new Verifier()).prepare(c,KEY,CTX,cmd("third")));c.rollback();}
        sql(ds,"UPDATE aoo_room_cost_policy SET cost_minor=100");var third=seated(ds,"third");assertEquals(2,third.attemptNo());assertEquals(100,third.charge().amount());assertEquals(900,scalar(ds,"SELECT balance FROM aoo_currency_balance"));
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertEquals(1,service(ds,new Verifier()).prepare(c,KEY,CTX,cmd("first")).attemptNo());c.rollback();}
    }
    @Test void commandAndContextConflictsRejectWithoutAnotherDebit()throws Exception {
        var ds=fixture();seated(ds,"same");try(var c=ds.getConnection()){c.setAutoCommit(false);var s=service(ds,new Verifier());assertThrows(IllegalArgumentException.class,()->s.prepare(c,KEY,CTX,new CommandKey("same","f".repeat(64),20)));c.rollback();
            assertThrows(IllegalArgumentException.class,()->s.prepare(c,KEY,new Context(11,1,"test-v1","TEST","ROUND_COUNT",8,4,CTX.authority()),cmd("next")));c.rollback();
            assertThrows(IllegalArgumentException.class,()->s.prepare(c,KEY,CTX,new CommandKey("same",cmd("same").requestHash(),21)));c.rollback();}
        assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void insufficientFundsAndCallerRollbackLeaveNoAttemptOrLedger()throws Exception {
        for(boolean insufficient:new boolean[]{true,false}) {var ds=fixture();if(insufficient)sql(ds,"UPDATE aoo_currency_balance SET balance=0");try(var c=ds.getConnection()){c.setAutoCommit(false);var s=service(ds,new Verifier());if(insufficient)assertThrows(RuntimeException.class,()->s.prepare(c,KEY,CTX,cmd("no-money")));else s.prepare(c,KEY,CTX,cmd("rollback"));c.rollback();}
            for(String table:List.of("aoo_room_aa_session","aoo_room_aa_attempt","aoo_room_aa_command","aoo_room_billing_intent_journal","aoo_room_billing_reservation","aoo_ledger"))assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM "+table));}
    }
    @Test void verifierSql23CannotBypassExistingReceipt()throws Exception {
        var ds=fixture();seated(ds,"same");var verifier=new Verifier();verifier.deny=true;try(var c=ds.getConnection()){c.setAutoCommit(false);var failure=assertThrows(IllegalStateException.class,()->service(ds,verifier).prepare(c,KEY,CTX,cmd("same")));assertEquals("23000",((SQLException)failure.getCause()).getSQLState());assertThrows(IllegalStateException.class,()->service(ds,verifier).findReceipt(c,KEY,CTX,cmd("same")));c.rollback();}
    }
    @Test void persistedProofMismatchRollsBackMoneyAndNativeFixture()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);var s=service(ds,new Verifier());var p=s.prepare(c,KEY,CTX,cmd("bad"));var proof=persist(c,p);var bad=new SeatProof(proof.requestId(),proof.requestHash(),20,0,KEY.roomInstanceId(),1,5,proof.stateVersion(),"e".repeat(64),proof.responseHash());assertThrows(IllegalArgumentException.class,()->s.commitSeated(c,p,bad));c.rollback();}
        assertEquals(1000,scalar(ds,"SELECT balance FROM aoo_currency_balance"));assertEquals(-1,scalar(ds,"SELECT seat FROM source_room"));assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void currentProvenanceAndExpiredFreeDenyBeforeNative()throws Exception {
        for(boolean free:new boolean[]{false,true}){var ds=fixture(!free);sql(ds,"DELETE FROM aoo_room_cost_policy_provenance");if(free){sql(ds,"UPDATE aoo_room_cost_policy SET cost_minor=0,free_policy_ref='FREE'");provenance(ds,true);createSource(ds);}try(var c=ds.getConnection()){c.setAutoCommit(false);var clock=Clock.fixed(TIME.plusSeconds(120),ZoneOffset.UTC);var s=new JdbcRoomAaParticipationService(charges(ds,TIME.plusSeconds(120)),clock,new Verifier());assertThrows(IllegalArgumentException.class,()->s.prepare(c,KEY,CTX,cmd("expired")));c.rollback();}assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));}
    }
    @Test void twoConnectionsSerializePaidReentry()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);var s=service(ds,new Verifier());var p=s.prepare(c,KEY,CTX,cmd("one"));s.commitSeated(c,p,persist(c,p));
            var executor=Executors.newSingleThreadExecutor();try{var f=executor.submit(()->seated(ds,"two"));Thread.sleep(50);assertFalse(f.isDone());c.commit();assertEquals(Disposition.REUSE_PAID,f.get(3,TimeUnit.SECONDS).disposition());}finally{executor.shutdownNow();}}
        assertEquals(1,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void pendingAndAlreadySeatedWithoutPaidAttemptFailClosed()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);service(ds,new Verifier()).prepare(c,KEY,CTX,cmd("pending"));c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->service(ds,new Verifier()).prepare(c,KEY,CTX,cmd("pending")));c.rollback();}
        var clean=fixture();sql(clean,"UPDATE source_room SET seat=0");try(var c=clean.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->service(clean,new Verifier()).prepare(c,KEY,CTX,cmd("already")));c.rollback();}
    }
    @Test void forgedProofIdentityAndPreparationCannotCommit()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);var service=service(ds,new Verifier());var p=service.prepare(c,KEY,CTX,cmd("proof"));var proof=persist(c,p);
            for(var bad:List.of(
                    new SeatProof("other",proof.requestHash(),20,0,KEY.roomInstanceId(),1,5,2,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),21,0,KEY.roomInstanceId(),1,5,2,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),20,4,KEY.roomInstanceId(),1,5,2,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),20,0,KEY.roomInstanceId(),2,5,2,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),20,0,KEY.roomInstanceId(),1,6,2,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),20,0,KEY.roomInstanceId(),1,5,1,proof.snapshotHash(),proof.responseHash()),
                    new SeatProof("proof",proof.requestHash(),20,0,KEY.roomInstanceId(),1,5,2,proof.snapshotHash(),"e".repeat(64))))
                assertThrows(IllegalArgumentException.class,()->service.commitSeated(c,p,bad));
            var forged=new Preparation(p.key(),p.context(),p.command(),p.sessionId(),p.attemptNo(),p.intent(),p.subject(),p.charge(),
                    new PreSeatState(-1,5,0,p.preState().snapshotHash()),p.disposition(),p.preparationHash(),null);
            assertThrows(IllegalArgumentException.class,()->service.commitSeated(c,forged,proof));c.rollback();}
        assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void callerTransactionAndActualPersistedResultAreMandatory()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){assertThrows(IllegalStateException.class,()->service(ds,new Verifier()).prepare(c,KEY,CTX,cmd("auto")));c.setAutoCommit(false);
            var service=service(ds,new Verifier());var p=service.prepare(c,KEY,CTX,cmd("bytes"));var proof=persist(c,p);
            try(var s=c.createStatement()){s.execute("UPDATE source_result SET response='different stored bytes'");}
            assertThrows(IllegalArgumentException.class,()->service.commitSeated(c,p,proof));c.rollback();}
        assertEquals(0,scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_session"));
    }

}
