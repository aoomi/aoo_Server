package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcRoomBillingIntentJournalTest{
    private static final Instant TIME=Instant.parse("2026-01-01T00:00:00.123Z");
    private RoomChargeOperation operation(long room,int players){return new RoomChargeOperation(room,"attempt","unit",1,1,2001,"pdk-v1","chengdu",RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.OWNER,"ROUND_COUNT",8,players,0);}
    private RoomBillingIntent intent(long room){return new RoomBillingIntent(operation(room,2),TIME);}
    private JdbcDataSource source()throws Exception{
        var source=new JdbcDataSource();source.setURL("jdbc:h2:mem:billingintent"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000");
        try(var c=source.getConnection();var q=c.createStatement()){q.execute(Files.readString(Path.of(System.getProperty("aoo.intent.ddl"))));}return source;
    }
    @Test void completeTypedIntentRoundTripsAndReplayKeepsOriginalTime()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();var intent=intent(1);
        try(var c=source.getConnection()){
            c.setAutoCommit(false);var first=journal.append(c,intent);
            assertEquals(first,journal.append(c,new RoomBillingIntent(intent.operation(),TIME.plusSeconds(10))));
            assertEquals(intent,first.intent());c.commit();assertEquals(first,journal.find(c,intent.operation().operationKey()));
        }
    }
    @Test void sameStableKeyDifferentCommandIsRejected()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        try(var c=source.getConnection()){
            c.setAutoCommit(false);var first=journal.append(c,intent(1));
            var changed=new RoomBillingIntent(operation(1,3),TIME);assertEquals(first.intent().operation().operationKey(),changed.operation().operationKey());
            assertThrows(IllegalArgumentException.class,()->journal.append(c,changed));assertEquals(first,journal.find(c,changed.operation().operationKey()));c.rollback();
        }
    }
    @Test void callerRollbackDoesNotPersistNewIntentAndAutocommitIsRefused()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        try(var c=source.getConnection()){
            assertThrows(IllegalArgumentException.class,()->journal.append(c,intent(1)));c.setAutoCommit(false);journal.append(c,intent(1));c.rollback();
            assertNull(journal.find(c,intent(1).operation().operationKey()));
        }
    }
    @Test void committedPrechargeIntentSurvivesLaterFailureAndNewConnection()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        try(var c=source.getConnection()){c.setAutoCommit(false);journal.append(c,intent(1));c.commit();}
        try(var failedBusiness=source.getConnection()){failedBusiness.setAutoCommit(false);failedBusiness.rollback();}
        try(var restarted=source.getConnection()){assertEquals(intent(1),new JdbcRoomBillingIntentJournal().find(restarted,intent(1).operation().operationKey()).intent());}
    }
    @Test void orderedScanUsesAllocatedSequenceWithBoundedPages()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        try(var c=source.getConnection()){
            c.setAutoCommit(false);var first=journal.append(c,intent(1));var second=journal.append(c,intent(2));c.commit();
            assertEquals(first,journal.scanAfter(c,0,1).getFirst());assertEquals(second,journal.scanAfter(c,first.sequence(),1).getFirst());
            assertThrows(IllegalArgumentException.class,()->journal.scanAfter(c,-1,1));assertThrows(IllegalArgumentException.class,()->journal.scanAfter(c,0,201));
        }
    }
    @Test void reverseCommitCanSkipCursorButSettledFreshScanCoversBoth()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        try(var late=source.getConnection();var early=source.getConnection();var reader=source.getConnection()){
            late.setAutoCommit(false);early.setAutoCommit(false);var first=journal.append(late,intent(1));var second=journal.append(early,intent(2));
            assertTrue(first.sequence()<second.sequence());early.commit();var visible=journal.scanAfter(reader,0,200);assertEquals(java.util.List.of(second),visible);
            late.commit();assertTrue(journal.scanAfter(reader,second.sequence(),200).isEmpty());
            assertEquals(java.util.List.of(first,second),journal.scanAfter(reader,0,200));
        }
    }
    @Test void corruptedCommandHashFailsClosedAndTimestampIsExact()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();
        assertThrows(IllegalArgumentException.class,()->new RoomBillingIntent(operation(1,2),TIME.plusNanos(1)));
        try(var c=source.getConnection()){
            c.setAutoCommit(false);journal.append(c,intent(1));c.commit();try(var q=c.createStatement()){q.executeUpdate("UPDATE aoo_room_billing_intent_journal SET command_hash='"+"a".repeat(64)+"'");}
            assertThrows(IllegalStateException.class,()->journal.find(c,intent(1).operation().operationKey()));
        }
    }
    @Test void concurrentDuplicateAppendReturnsOneOriginalIntent()throws Exception{
        var source=source();var journal=new JdbcRoomBillingIntentJournal();var entered=new CountDownLatch(1);
        try(var first=source.getConnection();var executor=Executors.newSingleThreadExecutor()){
            first.setAutoCommit(false);var original=journal.append(first,intent(1));
            var duplicate=executor.submit(()->{try(var other=source.getConnection()){other.setAutoCommit(false);entered.countDown();var result=journal.append(other,new RoomBillingIntent(operation(1,2),TIME.plusSeconds(1)));other.commit();return result;}});
            assertTrue(entered.await(1,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->duplicate.get(100,TimeUnit.MILLISECONDS));first.commit();assertEquals(original,duplicate.get(2,TimeUnit.SECONDS));
        }
    }
}
