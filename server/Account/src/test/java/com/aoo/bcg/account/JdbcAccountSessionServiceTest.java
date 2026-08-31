package com.aoo.bcg.account;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class JdbcAccountSessionServiceTest {
    private static final Instant NOW=Instant.parse("2026-08-24T12:00:00Z");
    private JdbcDataSource db; private JdbcAccountSessionService service;
    private final JdbcAccountSessionService.Client phone=new JdbcAccountSessionService.Client("phone-1","ios","1.0","127.0.0.1");
    @BeforeEach void setup()throws Exception{db=new JdbcDataSource();db.setURL("jdbc:h2:mem:"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");schema();service=new JdbcAccountSessionService(db,Clock.fixed(NOW,ZoneOffset.UTC));}

    @Test void durableRegistrationRotationLogoutRecoveryAndAudit(){
        long id=service.register("alice","correct horse battery staple".toCharArray(),"recover-alice",phone).accountId();
        var first=service.login("alice","correct horse battery staple".toCharArray(),phone,false);
        assertEquals(id,service.authorize(first.accessToken(),phone).accountId());
        var rotated=service.refresh(first.refreshToken(),phone);assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.refresh(first.refreshToken(),phone));
        service.logout(rotated.accessToken(),phone);assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.authorize(rotated.accessToken(),phone));
        service.recover("recover-alice","new correct horse battery".toCharArray(),phone);
        assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.login("alice","correct horse battery staple".toCharArray(),phone,false));
        assertEquals(id,service.authorize(service.login("alice","new correct horse battery".toCharArray(),phone,true).accessToken(),phone).accountId());
        assertTrue(service.audit(id).stream().anyMatch(a->a.action().equals("ACCOUNT_RECOVERED")));
    }
    @Test void guestUpgradeSingleDeviceAndBanRevokeSessions(){
        long id=service.registerGuest("guest-secret",phone).accountId();var guest=service.loginGuest("guest-secret",phone,false);
        service.upgrade(id,"bob","correct horse battery staple".toCharArray(),"bob-recovery",phone);
        assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.authorize(guest.accessToken(),phone));
        var first=service.login("bob","correct horse battery staple".toCharArray(),phone,false);
        var tablet=new JdbcAccountSessionService.Client("tablet","android","1.0","127.0.0.2");service.login("bob","correct horse battery staple".toCharArray(),tablet,true);
        assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.authorize(first.accessToken(),phone));
        service.ban(id,NOW.plusSeconds(60),"fraud","127.0.0.9");
        assertThrows(JdbcAccountSessionService.Forbidden.class,()->service.login("bob","correct horse battery staple".toCharArray(),phone,false));
    }
    @Test void logoutCannotBeTriggeredWithAMismatchedDevice(){
        service.register("carol","correct horse battery staple".toCharArray(),"recover-carol",phone);
        var tokens=service.login("carol","correct horse battery staple".toCharArray(),phone,false);
        var attacker=new JdbcAccountSessionService.Client("unknown-device","web","1.0","127.0.0.8");
        assertThrows(JdbcAccountSessionService.Unauthorized.class,()->service.logout(tokens.accessToken(),attacker));
        assertDoesNotThrow(()->service.authorize(tokens.accessToken(),phone));
    }
    private void schema()throws SQLException{try(Connection c=db.getConnection();Statement s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT AUTO_INCREMENT PRIMARY KEY,password_hash VARCHAR(255),guest_credential_hash CHAR(64) UNIQUE,recovery_credential_hash CHAR(64) UNIQUE,guest BOOLEAN NOT NULL,auth_generation BIGINT DEFAULT 0 NOT NULL,banned_until TIMESTAMP(3),ban_reason VARCHAR(255),created_at TIMESTAMP(3),updated_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_account_device(account_id BIGINT,device_id VARCHAR(128),channel_name VARCHAR(64),client_version VARCHAR(64),first_seen_at TIMESTAMP(3),last_seen_at TIMESTAMP(3),revoked_at TIMESTAMP(3),PRIMARY KEY(account_id,device_id))");
        s.execute("CREATE TABLE aoo_account_session(session_id CHAR(36) PRIMARY KEY,account_id BIGINT,device_id VARCHAR(128),token_family CHAR(36),access_hash CHAR(64) UNIQUE,refresh_hash CHAR(64) UNIQUE,access_expires_at TIMESTAMP(3),refresh_expires_at TIMESTAMP(3),auth_generation BIGINT,revoked_at TIMESTAMP(3),revoke_reason VARCHAR(64),created_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_account_audit(audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT,action_name VARCHAR(64),detail_value VARCHAR(255),source_ip VARCHAR(64),occurred_at TIMESTAMP(3))");
        s.execute("CREATE TABLE aoo_display_id_segment(digit_length INT PRIMARY KEY,range_start DECIMAL(65),range_end DECIMAL(65),capacity BIGINT,occupied_count BIGINT,allocation_threshold_bps INT,status VARCHAR(16),updated_at TIMESTAMP)");
        s.execute("INSERT INTO aoo_display_id_segment VALUES(6,100000,999999,900000,0,8500,'ACTIVE',CURRENT_TIMESTAMP)");
        s.execute("CREATE TABLE aoo_account_identity(identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,identity_type VARCHAR(24),normalized_value VARCHAR(255),value_hash CHAR(64),account_id BIGINT,verified BOOLEAN,status VARCHAR(16),created_at TIMESTAMP,updated_at TIMESTAMP,UNIQUE(identity_type,value_hash,status))");
        s.execute("CREATE TABLE aoo_display_id_history(history_id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT,old_display_id VARCHAR(65),new_display_id VARCHAR(65),valid_from TIMESTAMP,valid_until TIMESTAMP,operator_id BIGINT,reason VARCHAR(255),source_ip VARCHAR(64),trace_id VARCHAR(128))");}}
}
