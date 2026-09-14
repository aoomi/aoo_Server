package com.aoo.bcg.support;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import java.sql.Statement;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class SupportLiveSessionTest {
    private JdbcSupportLiveRepository repository;
    @BeforeEach void setup()throws Exception{
        JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:live"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        try(var c=ds.getConnection(); Statement s=c.createStatement()){
            s.execute("CREATE TABLE support_case(id BIGINT PRIMARY KEY,player_id BIGINT NOT NULL,status VARCHAR(20))");
            s.execute("INSERT INTO support_case VALUES(10,7,'OPEN')");
            s.execute("CREATE TABLE support_live_session(id BIGINT AUTO_INCREMENT PRIMARY KEY,case_id BIGINT,player_id BIGINT,agent_id BIGINT,status VARCHAR(20),last_message_id BIGINT,close_reason VARCHAR(500),created_at TIMESTAMP,updated_at TIMESTAMP,closed_at TIMESTAMP,row_version BIGINT)");
            s.execute("CREATE UNIQUE INDEX uq_live_open ON support_live_session(case_id,status)");
            s.execute("CREATE TABLE support_live_message(id BIGINT AUTO_INCREMENT PRIMARY KEY,session_id BIGINT,sender_type VARCHAR(20),sender_id BIGINT,body VARCHAR(2000),media_reference VARCHAR(160),created_at TIMESTAMP)");
            s.execute("CREATE TABLE support_live_audit(id BIGINT AUTO_INCREMENT PRIMARY KEY,session_id BIGINT,actor_type VARCHAR(20),actor_id BIGINT,event_type VARCHAR(40),event_detail VARCHAR(500),created_at TIMESTAMP)");
            s.execute("CREATE TABLE support_live_idempotency(actor_id BIGINT,operation_key VARCHAR(80),idempotency_key VARCHAR(128),session_id BIGINT,created_at TIMESTAMP,PRIMARY KEY(actor_id,operation_key,idempotency_key))");
            s.execute("CREATE TABLE media_asset(id BIGINT PRIMARY KEY,state VARCHAR(16),kind VARCHAR(16))");
            s.execute("CREATE TABLE media_asset_access(asset_id BIGINT,owner_id BIGINT)");
            s.execute("INSERT INTO media_asset VALUES(101,'READY','EVIDENCE')");
            s.execute("INSERT INTO media_asset_access VALUES(101,7)");
        }
        repository=new JdbcSupportLiveRepository(ds,Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"),ZoneOffset.UTC));
    }
    @Test void queueClaimMessageReconnectAndCloseAreDurableAndOwned(){
        var queued=repository.enqueue(7,10,"enqueue-0001");assertEquals(SupportLiveSession.Status.QUEUED,queued.status());assertEquals(queued.id(),repository.enqueue(7,10,"enqueue-0001").id());
        var active=repository.claim(99,queued.id(),queued.version(),"claim-000001");assertEquals(99,active.agentId());
        assertThrows(JdbcSupportLiveRepository.Missing.class,()->repository.agentView(98,queued.id(),0));
        var sent=repository.send(7,"PLAYER",queued.id(),"hello","media:101","message-0001");assertEquals("hello",sent.messages().getFirst().body());
        var disconnected=repository.disconnect(7,queued.id(),sent.version(),"disconnect01");assertEquals(SupportLiveSession.Status.DISCONNECTED,disconnected.status());
        var reconnected=repository.reconnect(7,queued.id(),sent.lastMessageId(),"reconnect001");assertEquals(SupportLiveSession.Status.ACTIVE,reconnected.status());assertTrue(reconnected.messages().isEmpty());
        var closed=repository.close(99,queued.id(),reconnected.version(),"close-000001","answered");assertEquals(SupportLiveSession.Status.CLOSED,closed.status());
        assertThrows(JdbcSupportLiveRepository.Conflict.class,()->repository.send(7,"PLAYER",queued.id(),"late",null,"message-0002"));
    }
    @Test void playerCannotUseAnotherPlayersCase(){assertThrows(JdbcSupportLiveRepository.Missing.class,()->repository.enqueue(8,10,"enqueue-0002"));}
}
