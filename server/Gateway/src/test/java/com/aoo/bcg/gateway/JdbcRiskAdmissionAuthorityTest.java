package com.aoo.bcg.gateway;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class JdbcRiskAdmissionAuthorityTest {
    DataSource source;Clock clock=Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void schema()throws Exception{JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:risk-admission"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");source=ds;try(Connection c=source.getConnection();Statement s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,banned_until TIMESTAMP NULL)");
        s.execute("CREATE TABLE db_player(id BIGINT PRIMARY KEY,account_id BIGINT NOT NULL)");
        s.execute("CREATE TABLE aoo_account_device(account_id BIGINT,device_id VARCHAR(128),revoked_at TIMESTAMP NULL,PRIMARY KEY(account_id,device_id))");
        s.execute("CREATE TABLE risk_event(risk_event_id BIGINT PRIMARY KEY,player_id BIGINT,room_id BIGINT,risk_score SMALLINT,status VARCHAR(24),created_at TIMESTAMP)");
        s.execute("CREATE TABLE telemetry_signal(signal_id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT,room_id BIGINT,device_hash CHAR(64),geo_cell VARCHAR(32),received_at TIMESTAMP)");
        s.execute("CREATE TABLE risk_admission_decision(request_id VARCHAR(128),action_name VARCHAR(24),player_id BIGINT,device_hash CHAR(64),room_id BIGINT,decision_code VARCHAR(16),risk_score SMALLINT,reason_codes VARCHAR(255),decided_at TIMESTAMP,PRIMARY KEY(request_id,action_name))");
        s.execute("INSERT INTO aoo_account VALUES(7,NULL),(8,NULL)");s.execute("INSERT INTO aoo_account_device VALUES(7,'device-a',NULL),(8,'device-b',NULL)");
    }}
    @Test void allowsCleanAuthoritativeLoginAndPersistsDecision()throws Exception{var authority=new JdbcRiskAdmissionAuthority(source,clock);assertTrue(authority.decide(RiskAdmissionAuthority.Action.LOGIN,7,"device-a",null,"login-1").allowed());try(Connection c=source.getConnection();ResultSet r=c.createStatement().executeQuery("SELECT decision_code,reason_codes FROM risk_admission_decision WHERE request_id='login-1'")){assertTrue(r.next());assertEquals("ALLOW",r.getString(1));assertEquals("",r.getString(2));}}
    @Test void rejectsBanWithoutExposingReason()throws Exception{try(Connection c=source.getConnection()){c.createStatement().execute("UPDATE aoo_account SET banned_until='2026-08-25 00:00:00' WHERE account_id=7");}var decision=new JdbcRiskAdmissionAuthority(source,clock).decide(RiskAdmissionAuthority.Action.LOGIN,7,"device-a",null,"login-ban");assertFalse(decision.allowed());assertEquals("ADMISSION_REJECTED",decision.publicCode());}
    @Test void resolvesPlayerToAccountForRoomBan()throws Exception{try(Connection c=source.getConnection()){c.createStatement().execute("INSERT INTO aoo_account VALUES(9,'2026-08-25 00:00:00')");c.createStatement().execute("INSERT INTO db_player VALUES(77,9)");}assertFalse(new JdbcRiskAdmissionAuthority(source,clock).decide(RiskAdmissionAuthority.Action.JOIN_ROOM,77,null,99L,"join-ban").allowed());}
    @Test void rejectsAuthoritativeSharedLocationOnJoin()throws Exception{try(Connection c=source.getConnection()){c.createStatement().execute("INSERT INTO telemetry_signal(player_id,room_id,geo_cell,received_at) VALUES(7,99,'31.23,121.47','2026-08-24 00:00:00'),(8,99,'31.23,121.47','2026-08-24 00:00:00')");}assertFalse(new JdbcRiskAdmissionAuthority(source,clock).decide(RiskAdmissionAuthority.Action.JOIN_ROOM,7,null,99L,"join-1").allowed());}
    @Test void rejectsServerCountedFrequency()throws Exception{try(Connection c=source.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO risk_admission_decision VALUES(?, 'CREATE_ROOM',7,NULL,99,'ALLOW',0,'',?)")){for(int i=0;i<10;i++){p.setString(1,"old-"+i);p.setTimestamp(2,Timestamp.from(clock.instant()));p.addBatch();}p.executeBatch();}assertFalse(new JdbcRiskAdmissionAuthority(source,clock).decide(RiskAdmissionAuthority.Action.CREATE_ROOM,7,null,99L,"create-limit").allowed());}
}
