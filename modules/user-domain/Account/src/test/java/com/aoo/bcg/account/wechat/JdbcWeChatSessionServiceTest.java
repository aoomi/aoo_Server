package com.aoo.bcg.account.wechat;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcWeChatSessionServiceTest {
    @Test void committedSingleDeviceLoginRevokesOldSessionThenNotifiesGateway() throws Exception {
        JdbcDataSource db=new JdbcDataSource();
        db.setURL("jdbc:h2:mem:"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        schema(db);
        String openHash=hash("wx-app:OPEN");
        try(Connection c=db.getConnection();Statement s=c.createStatement()){
            s.execute("INSERT INTO aoo_account(account_id,auth_generation,created_at,updated_at) VALUES(7,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            s.execute("INSERT INTO aoo_account_identity(identity_type,value_hash,account_id,verified,status) VALUES('WECHAT_OPEN','"+openHash+"',7,TRUE,'ACTIVE')");
        }
        var oauth=new WeChatOAuthClient("wx-app","test-secret-long-enough".toCharArray(),WeChatOAuthClient.DEFAULT_ENDPOINT,Duration.ofSeconds(2),
                (uri,timeout)->new WeChatOAuthClient.Response(200,"{\"access_token\":\"provider\",\"expires_in\":7200,\"openid\":\"OPEN\"}"));
        AtomicReference<String> replacement=new AtomicReference<>();
        var service=new JdbcWeChatSessionService(db,oauth,Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"),ZoneOffset.UTC),
                (accountId,sessionId,generation)->replacement.set(accountId+":"+sessionId+":"+generation));
        var old=(JdbcWeChatSessionService.TokenPair)service.login("old-code","old-phone","ios","1","127.0.0.1","MULTI_DEVICE");
        assertEquals(7,service.authenticate(old.accessToken()).accountId());
        var current=(JdbcWeChatSessionService.TokenPair)service.login("new-code","new-phone","android","1","127.0.0.2","SINGLE_DEVICE");
        assertThrows(SecurityException.class,()->service.authenticate(old.accessToken()));
        assertEquals(7,service.authenticate(current.accessToken()).accountId());
        assertTrue(replacement.get().startsWith("7:"));
        assertTrue(replacement.get().endsWith(":1"));
    }

    private static void schema(JdbcDataSource db)throws Exception{try(Connection c=db.getConnection();Statement s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,auth_generation BIGINT DEFAULT 0 NOT NULL,banned_until TIMESTAMP(3),created_at TIMESTAMP(3),updated_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_account_device(account_id BIGINT,device_id VARCHAR(128),channel_name VARCHAR(64),client_version VARCHAR(64),first_seen_at TIMESTAMP(3),last_seen_at TIMESTAMP(3),revoked_at TIMESTAMP(3),PRIMARY KEY(account_id,device_id))");
        s.execute("CREATE TABLE aoo_account_session(session_id CHAR(36) PRIMARY KEY,account_id BIGINT,device_id VARCHAR(128),token_family CHAR(36),access_hash CHAR(64),refresh_hash CHAR(64),access_expires_at TIMESTAMP(3),refresh_expires_at TIMESTAMP(3),auth_generation BIGINT,revoked_at TIMESTAMP(3),revoke_reason VARCHAR(64),created_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_account_audit(audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT,action_name VARCHAR(64),detail_value VARCHAR(255),source_ip VARCHAR(64),occurred_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_account_identity(identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,identity_type VARCHAR(24),value_hash CHAR(64),account_id BIGINT,verified BOOLEAN,status VARCHAR(16))");
    }}
    private static String hash(String value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
}
