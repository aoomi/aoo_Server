package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.hall.room.RoomCreateSaga;
import java.sql.Connection;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

final class JdbcRoomSagaBillingPortTest {
    @Test void ownerPaymentUsesDurablePolicyLedgerAndReservation() throws Exception {
        JdbcDataSource source = database("owner_payment");
        schema(source);
        try (Connection c=source.getConnection();var s=c.createStatement()) {
            s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("INSERT INTO aoo_currency_balance VALUES(7,'ROOM_CARD',0,100,0,CURRENT_TIMESTAMP)");
            s.execute("INSERT INTO aoo_room_cost_policy VALUES(8,'1.0.0',8,3,'OWNER','ROOM_CARD',10,'ACTIVE')");
        }
        AtomicLong ids=new AtomicLong(100);
        JdbcRoomSagaBillingPort port=new JdbcRoomSagaBillingPort(source,ids::incrementAndGet,
                Clock.systemUTC());
        RoomCreateSaga.Command command=command("OWNER");
        port.reserve(command);
        port.confirm(command);
        try (Connection c=source.getConnection();var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT balance FROM aoo_currency_balance WHERE player_id=7")){assertTrue(r.next());assertEquals(90,r.getLong(1));}
            try(var r=s.executeQuery("SELECT state FROM aoo_room_billing_reservation WHERE request_id='owner-payment-request'")){assertTrue(r.next());assertEquals("CONFIRMED",r.getString(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM aoo_ledger WHERE business_id='ROOM_CREATE_RESERVE:owner-payment-request'")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
        }
        port.reserve(command);
        try (Connection c=source.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT balance FROM aoo_currency_balance WHERE player_id=7")){assertTrue(r.next());assertEquals(90,r.getLong(1));}
    }

    @Test void participantPaymentCannotSilentlyUseOwnerOnlySaga() throws Exception {
        JdbcDataSource source=database("split_payment");schema(source);
        try(Connection c=source.getConnection();var s=c.createStatement()){
            s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("INSERT INTO aoo_room_cost_policy VALUES(8,'1.0.0',8,3,'SPLIT','ROOM_CARD',10,'ACTIVE')");
        }
        JdbcRoomSagaBillingPort port=new JdbcRoomSagaBillingPort(source,()->101,Clock.systemUTC());
        assertThrows(IllegalStateException.class,()->port.reserve(command("SPLIT")));
    }

    private static RoomCreateSaga.Command command(String payer){
        return new RoomCreateSaga.Command(7,"owner-payment-request",800001,8,"1.0.0",
                "3.8.8",1,Map.of("roundCount",8,"playerCount",3,"payerMode",payer),
                new RoomCreateSaga.Scope("PERSONAL",0,""),Map.of(),"test-trace","hash");
    }
    private static JdbcDataSource database(String name){JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+name+";MODE=MySQL;DB_CLOSE_DELAY=-1");source.setUser("sa");return source;}
    private static void schema(JdbcDataSource source)throws Exception{try(Connection c=source.getConnection();var s=c.createStatement()){
        s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");
        s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");
        s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR DEFAULT 'POSTED',channel_code VARCHAR DEFAULT 'SYSTEM',created_at TIMESTAMP)");
        s.execute("CREATE TABLE aoo_room_cost_policy(game_id BIGINT,play_version VARCHAR,round_count INT,player_count INT,payer_mode VARCHAR,currency_code VARCHAR,cost_minor BIGINT,status VARCHAR)");
        s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP)");
    }}
}
