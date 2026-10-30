package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomAaCreation.*;
import static com.aoo.bcg.billing.JdbcRoomAaParticipationTest.*;

final class JdbcRoomAaCreationTest {
    final JdbcRoomAaParticipationTest f=new JdbcRoomAaParticipationTest();
    JdbcRoomAaCreationRepository repo(JdbcDataSource ds){return new JdbcRoomAaCreationRepository(f.charges(ds,TIME),Clock.fixed(TIME,ZoneOffset.UTC));}
    @Test void configuredCreateHasNoMoneyAndCallerRollbackIsAtomic()throws Exception {
        var ds=f.fixture(false);try(var c=ds.getConnection()){c.setAutoCommit(false);var repo=repo(ds);var binding=repo.bind(c,createKey(),createContext());assertEquals(100,binding.policy().snapshot().amount());var receipt=repo.recordCommit(c,binding,createProof());assertEquals(0,receipt.createdAt().getNano()%1_000_000);
            assertEquals(receipt,repo.requireCommitted(c,binding,createProof()));assertEquals(binding,repo.bind(c,createKey(),createContext()));c.rollback();}
        assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation"));assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome"));
        for(String table:new String[]{"aoo_ledger","aoo_room_billing_intent_journal","aoo_room_billing_reservation"})assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM "+table));
        f.createSource(ds);f.seated(ds,"real-money-after-create");assertEquals(1,f.scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));assertEquals(900,f.scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }
    @Test void stableIdCannotChangeCreatorRequestHashContextOrPolicyChecksum()throws Exception {
        var ds=f.fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);var repo=repo(ds);assertThrows(IllegalArgumentException.class,()->repo.bind(c,new Key(createKey().requestId(),createKey().requestHash(),11),createContext()));c.rollback();
            assertThrows(IllegalArgumentException.class,()->repo.bind(c,new Key(createKey().requestId(),"d".repeat(64),10),createContext()));c.rollback();
            assertThrows(IllegalArgumentException.class,()->repo.bind(c,createKey(),new Context(101,1,"test-v1","TEST","ROUND_COUNT",8,4,CTX.authority())));c.rollback();}
        f.sql(ds,"UPDATE aoo_room_aa_creation SET policy_snapshot_hash='"+"e".repeat(64)+"'");try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->repo(ds).find(c,createKey(),true));c.rollback();}
    }
    @Test void missingOrInvalidPolicyNeverCreatesSource()throws Exception {
        for(String mutation:new String[]{"DELETE FROM aoo_room_cost_policy","UPDATE aoo_room_cost_policy SET status='DISABLED'","UPDATE aoo_room_cost_policy SET confirmation_status='PENDING'","UPDATE aoo_room_cost_policy_provenance SET release_id=99"}) {
            var ds=f.fixture(false);f.sql(ds,mutation);try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->repo(ds).bind(c,createKey(),createContext()));c.rollback();}assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation"));assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
        }
    }
    @Test void abortAndCommitAreMutuallyExclusiveAndDestroyNeedsExactCommittedIdentity()throws Exception {
        var ds=f.fixture(false);try(var c=ds.getConnection()){c.setAutoCommit(false);var repo=repo(ds);var b=repo.bind(c,createKey(),createContext());repo.abortBeforeCommit(c,b);assertThrows(IllegalStateException.class,()->repo.recordCommit(c,b,createProof()));assertThrows(IllegalStateException.class,()->repo.recordDestroy(c,b,createProof()));c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->f.service(ds,f.new Verifier()).prepare(c,KEY,CTX,cmd("aborted")));c.rollback();}
        var other=f.fixture();try(var c=other.getConnection()){c.setAutoCommit(false);var repo=repo(other);var b=repo.find(c,createKey(),true).orElseThrow();assertThrows(IllegalStateException.class,()->repo.abortBeforeCommit(c,b));assertThrows(IllegalArgumentException.class,()->repo.recordDestroy(c,b,new RuntimeProof("wrong-instance",1,5,2,10,1,"test-v1")));var destroy=repo.recordDestroy(c,b,new RuntimeProof(KEY.roomInstanceId(),1,5,2,10,1,"test-v1"));assertEquals(destroy,repo.recordDestroy(c,b,destroy.proof()));assertTrue(repo.outcome(c,b,EventType.COMMITTED).isPresent());assertThrows(IllegalStateException.class,()->repo.requireCommitted(c,b,createProof()));c.commit();}
        try(var c=other.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->f.service(other,f.new Verifier()).prepare(c,KEY,CTX,cmd("destroyed")));c.rollback();}assertEquals(0,f.scalar(other,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void normalWriterCannotInventCommitOrChangeIdentityAndVersionsDoNotGoBack()throws Exception {
        var ds=f.fixture(false);try(var c=ds.getConnection()){c.setAutoCommit(false);var repo=repo(ds);var b=repo.bind(c,createKey(),createContext());assertThrows(IllegalStateException.class,()->repo.requireCommitted(c,b,createProof()));repo.recordCommit(c,b,createProof());
            assertThrows(IllegalArgumentException.class,()->repo.requireCommitted(c,b,new RuntimeProof(KEY.roomInstanceId(),1,6,2,10,1,"test-v1")));
            assertThrows(IllegalArgumentException.class,()->repo.requireCommitted(c,b,new RuntimeProof(KEY.roomInstanceId(),1,5,0,10,1,"test-v1")));
            assertThrows(IllegalArgumentException.class,()->repo.recordCommit(c,b,new RuntimeProof(KEY.roomInstanceId(),1,5,2,10,1,"test-v1")));
            assertEquals(1,repo.requireCommitted(c,b,new RuntimeProof(KEY.roomInstanceId(),1,5,2,10,1,"test-v1")).proof().stateVersion());c.commit();}
    }
    @Test void newerActivePublicationDoesNotExpireValidOriginalPolicy()throws Exception {
        var ds=f.fixture();f.sql(ds,"INSERT INTO aoo_room_cost_publication VALUES(2,'PRODUCTION')");
        f.sql(ds,"INSERT INTO aoo_room_cost_policy SELECT 2,2,content_hash,250,currency_code,free_policy_ref,refund_policy_ref,tier_minor,round_count,game_id,play_version,region_code,measure_kind,measure_value,player_count,payer_mode,billing_kind,billing_context,status,confirmation_status FROM aoo_room_cost_policy WHERE publication_id=1");
        f.sql(ds,"UPDATE aoo_room_cost_active SET publication_id=2");
        var p=f.seated(ds,"old-publication-still-valid");assertEquals(1,p.charge().publicationId());assertEquals(100,p.charge().amount());assertEquals(900,f.scalar(ds,"SELECT balance FROM aoo_currency_balance"));
        f.sql(ds,"UPDATE aoo_room_cost_policy SET status='DISABLED' WHERE publication_id=1");
        assertEquals(RoomAaParticipation.Disposition.REUSE_PAID,f.seated(ds,"paid-after-disable").disposition());
        f.charges(ds,TIME).refund(p.charge().requestId(),"refund-for-new-attempt",new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.POLICY_ALLOWED,"allowed",RoomChargeOperation.hash("proof")));
        f.sql(ds,"UPDATE source_room SET seat=-1");try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->f.service(ds,f.new Verifier()).prepare(c,KEY,CTX,cmd("new-after-disable")));c.rollback();}
        assertEquals(1000,f.scalar(ds,"SELECT balance FROM aoo_currency_balance"));
    }
    @Test void commitAbortTwoConnectionsSerializeOnSameBinding()throws Exception {
        var ds=f.fixture(false);Binding binding;try(var c=ds.getConnection()){c.setAutoCommit(false);binding=repo(ds).bind(c,createKey(),createContext());c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);repo(ds).recordCommit(c,binding,createProof());var executor=Executors.newSingleThreadExecutor();try{var attempt=executor.submit(()->{try(var other=ds.getConnection()){other.setAutoCommit(false);try{repo(ds).abortBeforeCommit(other,binding);other.commit();return false;}catch(IllegalStateException expected){other.rollback();return true;}}});Thread.sleep(50);assertFalse(attempt.isDone());c.commit();assertTrue(attempt.get(3,TimeUnit.SECONDS));}finally{executor.shutdownNow();}}
    }
    @Test void missingCreationAndExpiredFreeCannotStartMoneyAndAutoCommitIsForbidden()throws Exception {
        var ds=f.fixture(false);try(var c=ds.getConnection()){assertThrows(IllegalStateException.class,()->repo(ds).bind(c,createKey(),createContext()));c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->f.service(ds,f.new Verifier()).prepare(c,KEY,CTX,cmd("missing-create")));c.rollback();}
        f.sql(ds,"DELETE FROM aoo_room_cost_policy_provenance");f.sql(ds,"UPDATE aoo_room_cost_policy SET cost_minor=0,free_policy_ref='FREE'");f.provenance(ds,true);
        try(var c=ds.getConnection()){c.setAutoCommit(false);var late=new JdbcRoomAaCreationRepository(f.charges(ds,TIME.plusSeconds(120)),Clock.fixed(TIME.plusSeconds(120),ZoneOffset.UTC));assertThrows(IllegalArgumentException.class,()->late.bind(c,createKey(),createContext()));c.rollback();}
        assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation"));assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void repeatableReadEstablishedSnapshotCannotCommitOppositeOutcome()throws Exception {
        for(boolean firstCommit:new boolean[]{true,false}) {
            var ds=f.fixture(false);Binding b;try(var c=ds.getConnection()){c.setAutoCommit(false);b=repo(ds).bind(c,createKey(),createContext());c.commit();}
            try(var stale=ds.getConnection();var first=ds.getConnection()) {
                stale.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);stale.setAutoCommit(false);
                try(var s=stale.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM aoo_room_aa_creation_outcome")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
                first.setAutoCommit(false);if(firstCommit)repo(ds).recordCommit(first,b,createProof());else repo(ds).abortBeforeCommit(first,b);
                var executor=Executors.newSingleThreadExecutor();try {
                    var opposite=executor.submit(()->{try {if(firstCommit)repo(ds).abortBeforeCommit(stale,b);else repo(ds).recordCommit(stale,b,createProof());stale.commit();return false;}
                        catch(IllegalStateException expected){stale.rollback();return true;}
                        catch(SQLException conflict){stale.rollback();assertTrue(conflict.getSQLState().startsWith("40"),conflict.getSQLState());return true;}});
                    Thread.sleep(50);assertFalse(opposite.isDone());first.commit();assertTrue(opposite.get(3,TimeUnit.SECONDS));
                }finally{executor.shutdownNow();}
            }
            assertEquals(1,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome"));
            assertEquals(firstCommit?"COMMITTED":"ABORTED_BEFORE_COMMIT",lifecycle(ds));
            assertEquals(firstCommit?1:0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome WHERE event_type='COMMITTED'"));
            assertEquals(firstCommit?0:1,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome WHERE event_type='ABORTED_BEFORE_COMMIT'"));
        }
    }


    private String lifecycle(JdbcDataSource ds)throws Exception {
        try(var c=ds.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT lifecycle_state FROM aoo_room_aa_creation")) {assertTrue(r.next());return r.getString(1);}
    }
    private boolean raceOutcome(JdbcDataSource ds,Binding b,CyclicBarrier barrier,boolean commit)throws Exception {
        try(var c=ds.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM aoo_room_aa_creation_outcome")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
            barrier.await(3,TimeUnit.SECONDS);
            try {if(commit)repo(ds).recordCommit(c,b,createProof());else repo(ds).abortBeforeCommit(c,b);c.commit();return true;}
            catch(IllegalStateException rejected){c.rollback();return false;}
            catch(SQLException conflict){c.rollback();assertTrue(conflict.getSQLState().startsWith("40"),conflict.getSQLState());return false;}
        }
    }
    @Test void concurrentCommitAbortHaveExactlyOneDurableWinner()throws Exception {
        var ds=f.fixture(false);Binding b;try(var c=ds.getConnection()){c.setAutoCommit(false);b=repo(ds).bind(c,createKey(),createContext());c.commit();}
        var barrier=new CyclicBarrier(2);var workers=Executors.newFixedThreadPool(2);
        try {var commit=workers.submit(()->raceOutcome(ds,b,barrier,true));var abort=workers.submit(()->raceOutcome(ds,b,barrier,false));boolean committed=commit.get(5,TimeUnit.SECONDS),aborted=abort.get(5,TimeUnit.SECONDS);assertNotEquals(committed,aborted);assertEquals(committed?"COMMITTED":"ABORTED_BEFORE_COMMIT",lifecycle(ds));assertEquals(1,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome"));}
        finally {workers.shutdownNow();}
        assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void targetReplayNeedsExactExistingReceiptAndRejectsChangedProof()throws Exception {
        var ds=f.fixture(false);try(var c=ds.getConnection()){c.setAutoCommit(false);var repository=repo(ds);var b=repository.bind(c,createKey(),createContext());var first=repository.recordCommit(c,b,createProof());assertEquals(first,repository.recordCommit(c,b,createProof()));assertThrows(IllegalArgumentException.class,()->repository.recordCommit(c,b,new RuntimeProof(KEY.roomInstanceId(),1,5,2,10,1,"test-v1")));assertThrows(IllegalStateException.class,()->repository.abortBeforeCommit(c,b));c.commit();}
        assertEquals("COMMITTED",lifecycle(ds));assertEquals(1,f.scalar(ds,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome"));assertEquals(0,f.scalar(ds,"SELECT COUNT(*) FROM aoo_ledger"));
    }
    @Test void staleCommittedSnapshotCannotReplayOrAuthorizeAfterDestroy()throws Exception {
        var ds=f.fixture();Binding b;try(var c=ds.getConnection()){c.setAutoCommit(false);b=repo(ds).find(c,createKey(),true).orElseThrow();c.commit();}
        for(boolean replay:new boolean[]{true,false}) {
            var db=f.fixture();Binding bound;try(var c=db.getConnection()){c.setAutoCommit(false);bound=repo(db).find(c,createKey(),true).orElseThrow();c.commit();}
            try(var stale=db.getConnection();var destroy=db.getConnection()) {
                stale.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);stale.setAutoCommit(false);
                try(var s=stale.createStatement();var r=s.executeQuery("SELECT lifecycle_state FROM aoo_room_aa_creation")){assertTrue(r.next());assertEquals("COMMITTED",r.getString(1));}
                destroy.setAutoCommit(false);repo(db).recordDestroy(destroy,bound,new RuntimeProof(KEY.roomInstanceId(),1,5,2,10,1,"test-v1"));destroy.commit();
                boolean rejected=false;try {if(replay)repo(db).recordCommit(stale,bound,createProof());else repo(db).requireCommitted(stale,bound,createProof());}
                catch(IllegalStateException expected){rejected=true;}
                catch(SQLException conflict){assertTrue(conflict.getSQLState().startsWith("40"),conflict.getSQLState());rejected=true;}
                assertTrue(rejected);stale.rollback();
            }
            assertEquals("DESTROYED",lifecycle(db));assertEquals(2,f.scalar(db,"SELECT COUNT(*) FROM aoo_room_aa_creation_outcome"));assertEquals(0,f.scalar(db,"SELECT COUNT(*) FROM aoo_ledger"));
        }
    }

}
