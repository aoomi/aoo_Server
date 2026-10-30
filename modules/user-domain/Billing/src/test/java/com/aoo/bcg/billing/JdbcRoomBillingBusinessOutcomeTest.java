package com.aoo.bcg.billing;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.JdbcRoomBillingSourceBindingTest.*;
import static com.aoo.bcg.billing.RoomBillingBusinessOutcome.EventType.*;

final class JdbcRoomBillingBusinessOutcomeTest {
    static JdbcRoomBillingBusinessOutcomeRepository outcomes(){return new JdbcRoomBillingBusinessOutcomeRepository(repo());}
    static RoomBillingBusinessOutcome commit(){return new RoomBillingBusinessOutcome(CREATE_COMMITTED,1,9,"instance",1L,2L,3L,1,"test-v1",TIME);}
    @Test void snapshotAndReceiptRollbackAndExactReplay()throws Exception {
        var ds=fixture();var bound=seed(ds);try(var c=ds.getConnection()){c.setAutoCommit(false);try(var q=c.createStatement()){q.execute("INSERT INTO business_snapshot VALUES(1,'snapshot')");}outcomes().recordCommit(c,bound,commit());c.rollback();}
        assertEquals(0,count(ds,"business_snapshot"));assertEquals(0,count(ds,"aoo_room_billing_business_receipt"));
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertTrue(outcomes().find(c,key(),CREATE_COMMITTED).isEmpty());assertEquals(commit(),outcomes().recordCommit(c,bound,commit()));c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertEquals(commit(),outcomes().recordCommit(c,bound,new RoomBillingBusinessOutcome(CREATE_COMMITTED,1,9,"instance",1L,2L,3L,1,"test-v1",TIME.plusSeconds(5))));assertThrows(IllegalArgumentException.class,()->outcomes().recordCommit(c,bound,new RoomBillingBusinessOutcome(CREATE_COMMITTED,1,9,"other",1L,2L,3L,1,"test-v1",TIME)));assertThrows(IllegalStateException.class,()->outcomes().abortBeforeCommit(c,key()));c.rollback();}
    }
    @Test void durableAbortFencesLateSnapshotAndPreservesNullIdentity()throws Exception {
        var ds=fixture();var bound=seed(ds);try(var c=ds.getConnection()){c.setAutoCommit(false);var aborted=outcomes().abortBeforeCommit(c,key());assertNull(aborted.roomInstanceId());assertNull(aborted.lifecycleEpoch());assertNull(aborted.fencingToken());assertNull(aborted.stateVersion());c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);try(var q=c.createStatement()){q.execute("INSERT INTO business_snapshot VALUES(1,'late')");}assertThrows(IllegalStateException.class,()->outcomes().recordCommit(c,bound,commit()));c.rollback();}
        assertEquals(0,count(ds,"business_snapshot"));assertEquals(1,count(ds,"aoo_room_billing_business_receipt"));
    }
    @Test void destroyRequiresExactCommittedIdentityAndCallerOwnsRollback()throws Exception {
        var ds=fixture();var bound=seed(ds);var destroy=new RoomBillingBusinessOutcome(CREATE_DESTROYED,1,9,"instance",1L,2L,4L,1,"test-v1",TIME);
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->outcomes().recordDestroy(c,bound,destroy));outcomes().recordCommit(c,bound,commit());c.commit();}
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->outcomes().recordDestroy(c,bound,new RoomBillingBusinessOutcome(CREATE_DESTROYED,1,9,"instance",1L,3L,4L,1,"test-v1",TIME)));outcomes().recordDestroy(c,bound,destroy);c.rollback();}
        assertEquals(1,count(ds,"aoo_room_billing_business_receipt"));try(var c=ds.getConnection()){c.setAutoCommit(false);assertEquals(destroy,outcomes().recordDestroy(c,bound,destroy));c.commit();}assertEquals(2,count(ds,"aoo_room_billing_business_receipt"));
    }
    @Test void twoConnectionsCommitAndAbortMutuallyExclusive()throws Exception {
        var ds=fixture();var bound=seed(ds);var started=new CountDownLatch(1);
        try(var c=ds.getConnection();var executor=Executors.newSingleThreadExecutor()){c.setAutoCommit(false);outcomes().recordCommit(c,bound,commit());var future=executor.submit(()->{try(var other=ds.getConnection()){other.setAutoCommit(false);started.countDown();try{outcomes().abortBeforeCommit(other,key());other.commit();return true;}catch(IllegalStateException denied){other.rollback();return false;}}});assertTrue(started.await(1,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->future.get(100,TimeUnit.MILLISECONDS));c.commit();assertFalse(future.get(2,TimeUnit.SECONDS));}assertEquals(1,count(ds,"aoo_room_billing_business_receipt"));
    }
}
