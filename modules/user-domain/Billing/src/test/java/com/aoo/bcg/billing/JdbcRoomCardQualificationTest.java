package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.time.Clock;
import java.util.concurrent.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcRoomCardQualificationTest {
    private JdbcDataSource source(){
        var source=new JdbcDataSource();source.setURL("jdbc:h2:mem:qualification"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000");
        try(var c=source.getConnection();var q=c.createStatement()){
            q.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR(32) PRIMARY KEY,scope_type VARCHAR(16),status VARCHAR(16))");
            q.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE'),('GOLD','GLOBAL','ACTIVE')");
            q.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR(32),currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");
            q.execute("CREATE TABLE aoo_ledger(business_id VARCHAR(128))");
            q.execute("INSERT INTO aoo_currency_balance VALUES(1,'ROOM_CARD',0,500,0,CURRENT_TIMESTAMP),(2,'ROOM_CARD',0,499,0,CURRENT_TIMESTAMP),(2,'GOLD',0,999,0,CURRENT_TIMESTAMP),(2,'CRYSTAL',0,999,0,CURRENT_TIMESTAMP)");
        }catch(Exception failure){throw new AssertionError(failure);}return source;
    }
    private JdbcBillingService billing(JdbcDataSource source){return new JdbcBillingService(source,()->1,Clock.systemUTC());}
    private long scalar(Connection c,String sql)throws Exception{try(var q=c.createStatement();var row=q.executeQuery(sql)){assertTrue(row.next());return row.getLong(1);}}
    @Test void qualificationLocksWithoutDebitOrLedgerAndCallerRollsBack()throws Exception{
        var source=source();var billing=billing(source);
        try(var c=source.getConnection()){
            c.setAutoCommit(false);assertEquals(500,billing.requireRoomCardBalance(c,1,500));
            assertFalse(c.getAutoCommit());assertEquals(500,scalar(c,"SELECT balance FROM aoo_currency_balance WHERE player_id=1 AND currency='ROOM_CARD'"));
            assertEquals(0,scalar(c,"SELECT COUNT(*) FROM aoo_ledger"));c.rollback();
            assertEquals(500,scalar(c,"SELECT balance FROM aoo_currency_balance WHERE player_id=1 AND currency='ROOM_CARD'"));
        }
    }
    @Test void insufficientRoomCardCannotUseGoldOrRetiredBalance()throws Exception{
        var source=source();var billing=billing(source);
        try(var c=source.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->billing.requireRoomCardBalance(c,2,500));assertEquals(499,scalar(c,"SELECT balance FROM aoo_currency_balance WHERE player_id=2 AND currency='ROOM_CARD'"));assertEquals(0,scalar(c,"SELECT COUNT(*) FROM aoo_ledger"));c.rollback();}
    }
    @Test void zeroMissingBalanceAndInvalidInputsPreserveCallerControl()throws Exception{
        var source=source();var billing=billing(source);
        try(var c=source.getConnection()){
            assertThrows(IllegalArgumentException.class,()->billing.requireRoomCardBalance(c,1,500));
            c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->billing.requireRoomCardBalance(c,1,-1));
            assertEquals(0,billing.requireRoomCardBalance(c,3,0));c.rollback();
            assertEquals(0,scalar(c,"SELECT COUNT(*) FROM aoo_currency_balance WHERE player_id=3"));
        }
    }
    @Test void anotherTransactionCannotAcquireBalanceUntilCallerReleases()throws Exception{
        var source=source();var billing=billing(source);var entered=new CountDownLatch(1);
        try(var c=source.getConnection();var executor=Executors.newSingleThreadExecutor()){
            c.setAutoCommit(false);billing.requireRoomCardBalance(c,1,500);
            var next=executor.submit(()->{try(var other=source.getConnection()){other.setAutoCommit(false);entered.countDown();long balance=billing.requireRoomCardBalance(other,1,500);other.rollback();return balance;}});
            assertTrue(entered.await(1,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->next.get(100,TimeUnit.MILLISECONDS));
            c.rollback();assertEquals(500,next.get(2,TimeUnit.SECONDS));
        }
    }
}
