package com.aoo.bcg.gateway;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class JdbcWsTicketServiceTest {
    DataSource source; MutableClock clock;
    @BeforeEach void schema()throws Exception{JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:ticket"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");source=ds;clock=new MutableClock(Instant.parse("2026-08-24T00:00:00Z"));try(Connection c=source.getConnection();Statement s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,auth_generation BIGINT NOT NULL,banned_until TIMESTAMP NULL)");
        s.execute("CREATE TABLE aoo_account_device(account_id BIGINT,device_id VARCHAR(128),revoked_at TIMESTAMP NULL,PRIMARY KEY(account_id,device_id))");
        s.execute("CREATE TABLE aoo_account_session(session_id CHAR(36) PRIMARY KEY,account_id BIGINT,device_id VARCHAR(128),access_hash CHAR(64),access_expires_at TIMESTAMP,auth_generation BIGINT,revoked_at TIMESTAMP NULL)");
        s.execute("CREATE TABLE gateway_ws_ticket(ticket_hash CHAR(64) PRIMARY KEY,account_id BIGINT,session_id CHAR(36),device_id VARCHAR(128),allowed_origin VARCHAR(255),idempotency_key VARCHAR(128),expires_at TIMESTAMP,issued_at TIMESTAMP,UNIQUE(account_id,idempotency_key))");
        s.execute("CREATE TABLE risk_event(risk_event_id BIGINT PRIMARY KEY,player_id BIGINT,room_id BIGINT,risk_score SMALLINT,status VARCHAR(24),created_at TIMESTAMP)");
        s.execute("CREATE TABLE telemetry_signal(signal_id BIGINT PRIMARY KEY,player_id BIGINT,room_id BIGINT,device_hash CHAR(64),geo_cell VARCHAR(32),received_at TIMESTAMP)");
        s.execute("CREATE TABLE risk_admission_decision(request_id VARCHAR(128),action_name VARCHAR(24),player_id BIGINT,device_hash CHAR(64),room_id BIGINT,decision_code VARCHAR(16),risk_score SMALLINT,reason_codes VARCHAR(255),decided_at TIMESTAMP,PRIMARY KEY(request_id,action_name))");
        s.execute("INSERT INTO aoo_account VALUES(7,3,NULL)");s.execute("INSERT INTO aoo_account_device VALUES(7,'device-a',NULL)");
        try(PreparedStatement p=c.prepareStatement("INSERT INTO aoo_account_session VALUES('00000000-0000-0000-0000-000000000007',7,'device-a',?,?,3,NULL)")){p.setString(1,hash("access-secret"));p.setTimestamp(2,Timestamp.from(clock.instant().plus(Duration.ofMinutes(10))));p.executeUpdate();}
    }}
    @Test void authenticatesIssuesAndAtomicallyRejectsReplay(){var service=new JdbcWsTicketService(source,clock);var issued=service.issue("access-secret","device-a","https://game.example","page-1","request-1");assertEquals(30,Duration.between(clock.instant(),issued.expiresAt()).toSeconds());assertEquals(7,service.consume(issued.ticket(),"device-a","https://game.example","page-1").userId());assertEquals(GatewayErrorCode.WS_TICKET_REJECTED,assertThrows(GatewayTicketException.class,()->service.consume(issued.ticket(),"device-a","https://game.example","page-1")).code());}
    @Test void rejectsWrongDeviceAndOriginWithoutBurningValidTicket(){var service=new JdbcWsTicketService(source,clock);var issued=service.issue("access-secret","device-a","https://game.example","page-2","request-2");assertEquals(GatewayErrorCode.WS_TICKET_REJECTED,assertThrows(GatewayTicketException.class,()->service.consume(issued.ticket(),"device-b","https://game.example","page-2")).code());assertEquals(GatewayErrorCode.WS_TICKET_REJECTED,assertThrows(GatewayTicketException.class,()->service.consume(issued.ticket(),"device-a","https://evil.example","page-2")).code());assertEquals(7,service.consume(issued.ticket(),"device-a","https://game.example","page-2").userId());}
    @Test void rejectsExpiredTicketAndDuplicateIssueRequest(){var service=new JdbcWsTicketService(source,clock);var issued=service.issue("access-secret","device-a","https://game.example","page-3","request-3");assertEquals(GatewayErrorCode.IDEMPOTENCY_CONFLICT,assertThrows(GatewayTicketException.class,()->service.issue("access-secret","device-a","https://game.example","page-3","request-3")).code());clock.advance(Duration.ofSeconds(30));assertEquals(GatewayErrorCode.WS_TICKET_EXPIRED,assertThrows(GatewayTicketException.class,()->service.consume(issued.ticket(),"device-a","https://game.example","page-3")).code());}
    @Test void rejectsMismatchedBearerDevice(){var service=new JdbcWsTicketService(source,clock);assertEquals(GatewayErrorCode.UNAUTHORIZED,assertThrows(GatewayTicketException.class,()->service.issue("access-secret","device-b","https://game.example","page-4","request-4")).code());}
    @Test void browserUpgradeUsesTicketBoundDeviceAndRechecksLiveAuthority()throws Exception{
        var service=new JdbcWsTicketService(source,clock);
        var valid=service.issue("access-secret","device-a","https://game.example","page-browser-1","browser-1");
        assertEquals("device-a",service.consume(valid.ticket(),"https://game.example","page-browser-1").deviceFingerprint());
        var revoked=service.issue("access-secret","device-a","https://game.example","page-browser-2","browser-2");
        try(Connection c=source.getConnection();Statement s=c.createStatement()){s.execute("UPDATE aoo_account_session SET revoked_at=CURRENT_TIMESTAMP WHERE session_id='00000000-0000-0000-0000-000000000007'");}
        assertEquals(GatewayErrorCode.WS_TICKET_REJECTED,assertThrows(GatewayTicketException.class,()->service.consume(revoked.ticket(),"https://game.example","page-browser-2")).code());
    }
    static String hash(String v)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}
    static final class MutableClock extends Clock{Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration d){now=now.plus(d);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}}
}
