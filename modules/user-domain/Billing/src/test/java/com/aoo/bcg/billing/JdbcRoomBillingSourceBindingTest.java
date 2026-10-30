package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.StringReader;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomBillingSourceBinding.*;

final class JdbcRoomBillingSourceBindingTest {
    static final Instant TIME=Instant.parse("2026-10-17T00:00:00Z");
    static void ddl(Connection c,String name)throws Exception{RunScript.execute(c,new StringReader(Files.readString(Path.of("server/database/migrations",name))));}
    static SourceKey key(){return new SourceKey("HALL_CREATE","source-request-0001",RoomChargeOperation.hash("REQUEST"),9);}
    static AuthorityReference authority(){return new AuthorityReference(11,12,"b".repeat(64),"TEST");}
    static JdbcRoomChargeCoordinator.Subject subject(){return new JdbcRoomChargeCoordinator.Subject(9,9,1);}
    static RoomChargeOperation operation(long payer){return new RoomChargeOperation(1,"attempt","CREATE",7,payer,1,"test-v1","TEST",RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.CLUB,RoomChargeOperation.Payer.CLUB,"ROUND_COUNT",8,4,0);}
    static RoomBillingIntent intent(){return new RoomBillingIntent(operation(7),TIME);}
    static JdbcRoomBillingSourceBindingRepository repo(){return new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal());}
    static JdbcDataSource fixture()throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=1000");
        try(var c=ds.getConnection();var q=c.createStatement()) {
            q.execute("CREATE TABLE aoo_room_create_saga(request_id VARCHAR(128) PRIMARY KEY,request_hash CHAR(64),account_id BIGINT,room_id BIGINT)");
            try(var insert=c.prepareStatement("INSERT INTO aoo_room_create_saga VALUES(?,?,9,1)")){insert.setString(1,key().requestId());insert.setString(2,key().requestHash());insert.executeUpdate();}
            ddl(c,"V20261017_01__room_card_billing_intent_journal.sql");ddl(c,"V20261017_02__room_billing_subject_source_binding.sql");ddl(c,"V20261017_03__room_billing_business_outcome_receipt.sql");
            q.execute("CREATE TABLE business_snapshot(id BIGINT PRIMARY KEY,snapshot_text VARCHAR)");
        }return ds;
    }
    static BoundIntent seed(JdbcDataSource ds)throws Exception{try(var c=ds.getConnection()){c.setAutoCommit(false);var bound=repo().bind(c,key(),intent(),subject(),authority());c.commit();return bound;}}
    static long count(JdbcDataSource ds,String table)throws Exception{try(var c=ds.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM "+table)){assertTrue(r.next());return r.getLong(1);}}
    @Test void realDdlRoundTripOriginalTimeAndOwnerIndependentRead()throws Exception {
        var ds=fixture();var original=seed(ds);
        try(var c=ds.getConnection()){c.setAutoCommit(false);var same=repo().bind(c,key(),new RoomBillingIntent(operation(7),TIME.plusSeconds(1)),subject(),authority());assertEquals(original,same);c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertEquals(original,repo().find(c,key(),true).orElseThrow());c.rollback();}
        assertEquals(1,count(ds,"aoo_room_billing_intent_journal"));assertEquals(1,count(ds,"aoo_room_billing_source_binding"));
    }
    @Test void allSourceSubjectOperationAuthorityConflictsReject()throws Exception {
        var ds=fixture();seed(ds);
        try(var c=ds.getConnection()){c.setAutoCommit(false);
            for(var s:new JdbcRoomChargeCoordinator.Subject[]{new JdbcRoomChargeCoordinator.Subject(10,7,1),new JdbcRoomChargeCoordinator.Subject(9,8,1),new JdbcRoomChargeCoordinator.Subject(9,7,2)})assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),intent(),s,authority()));
            assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),new RoomBillingIntent(operation(8),TIME),subject(),authority()));
            for(var a:new AuthorityReference[]{new AuthorityReference(13,12,"b".repeat(64),"TEST"),new AuthorityReference(11,13,"b".repeat(64),"TEST"),new AuthorityReference(11,12,"c".repeat(64),"TEST")})assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),intent(),subject(),a));
            assertThrows(IllegalArgumentException.class,()->repo().find(c,new SourceKey("HALL_CREATE",key().requestId(),"d".repeat(64),9),true));assertThrows(IllegalArgumentException.class,()->repo().find(c,new SourceKey("HALL_CREATE",key().requestId(),key().requestHash(),10),true));c.rollback();}
        assertEquals(1,count(ds,"aoo_room_billing_intent_journal"));
    }
    @Test void rollbackAndSagaProofAndStoredFingerprintGuard()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),intent(),subject(),authority()));c.setAutoCommit(false);repo().bind(c,key(),intent(),subject(),authority());c.rollback();}assertEquals(0,count(ds,"aoo_room_billing_source_binding"));assertEquals(0,count(ds,"aoo_room_billing_intent_journal"));
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->repo().find(c,new SourceKey("HALL_CREATE","missing-request-0001",key().requestHash(),9),true));try(var q=c.createStatement()){q.execute("UPDATE aoo_room_create_saga SET room_id=2");}assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),intent(),subject(),authority()));c.rollback();}
        seed(ds);try(var c=ds.getConnection();var q=c.createStatement()){q.execute("UPDATE aoo_room_billing_source_binding SET binding_hash='"+"0".repeat(64)+"'");}try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->repo().find(c,key(),false));c.rollback();}
    }
    @Test void concurrentSameSourceSerializedByCallerSagaLock()throws Exception {
        var ds=fixture();var original=seed(ds);var entered=new CountDownLatch(1);
        try(var c=ds.getConnection();var executor=Executors.newSingleThreadExecutor()){c.setAutoCommit(false);repo().find(c,key(),true);var future=executor.submit(()->{try(var other=ds.getConnection()){other.setAutoCommit(false);entered.countDown();var bound=repo().bind(other,key(),intent(),subject(),authority());other.commit();return bound;}});assertTrue(entered.await(1,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->future.get(100,TimeUnit.MILLISECONDS));c.rollback();assertEquals(original,future.get(2,TimeUnit.SECONDS));}
    }
    @Test void authenticatedCreateCreatorMismatchCannotPersistAnything()throws Exception {
        var ds=fixture();try(var c=ds.getConnection()){c.setAutoCommit(false);
            assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),intent(),new JdbcRoomChargeCoordinator.Subject(9,7,1),authority()));
            c.commit();
        }
        assertEquals(0,count(ds,"aoo_room_billing_intent_journal"));assertEquals(0,count(ds,"aoo_room_billing_source_binding"));assertEquals(0,count(ds,"aoo_room_billing_business_receipt"));
    }

}
