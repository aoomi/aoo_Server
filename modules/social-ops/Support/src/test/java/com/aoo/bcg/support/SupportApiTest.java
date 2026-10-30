package com.aoo.bcg.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SupportApiTest {
 static final String SECRET="01234567890123456789012345678901";
 JdbcSupportRepository repo;
 Clock clock=Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC);
 @BeforeEach void setup()throws Exception {
  var ds=new JdbcDataSource();
  ds.setURL("jdbc:h2:mem:support"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  try(var c=ds.getConnection();var s=c.createStatement()){
   s.execute("CREATE TABLE support_case(id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT NOT NULL,case_kind VARCHAR(20),subject VARCHAR(120),description VARCHAR(4000),status VARCHAR(20),resolution_summary VARCHAR(1000),created_at TIMESTAMP,updated_at TIMESTAMP,row_version BIGINT)");
   s.execute("CREATE TABLE support_case_evidence(id BIGINT AUTO_INCREMENT PRIMARY KEY,case_id BIGINT,evidence_type VARCHAR(20),reference_id VARCHAR(256),label VARCHAR(80))");
   s.execute("CREATE TABLE support_case_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,case_id BIGINT,actor_type VARCHAR(20),actor_id BIGINT,event_type VARCHAR(40),case_status VARCHAR(20),created_at TIMESTAMP)");
   s.execute("CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT)");
   s.execute("CREATE TABLE replay_participant_archive(room_id BIGINT,set_id INT,player_id BIGINT)");
   s.execute("INSERT INTO replay_participant VALUES(99,2,7)");
  }
  repo=new JdbcSupportRepository(ds,new ObjectMapper().findAndRegisterModules(),clock);
 }
 @Test void playerLifecycleIsOwnerScopedAndResolutionCannotBeMutated(){
  var c=repo.create(7,SupportCase.Kind.REPORT,"cheating","suspected collusion",List.of(new SupportCase.Evidence(SupportCase.Evidence.Type.REPLAY,"99:2","round two")));
  assertEquals(SupportCase.Status.OPEN,c.status());assertTrue(repo.find(8,c.id()).isEmpty());
  assertThrows(SecurityException.class,()->repo.create(8,SupportCase.Kind.APPEAL,"appeal","review",List.of(new SupportCase.Evidence(SupportCase.Evidence.Type.REPLAY,"99:2","x"))));
  var withdrawn=repo.withdraw(7,c.id(),c.version());assertEquals(SupportCase.Status.WITHDRAWN,withdrawn.status());
  assertThrows(IllegalStateException.class,()->repo.withdraw(7,c.id(),withdrawn.version()));assertEquals(2,repo.events(7,0).size());
 }
 @Test void bearerUsesAccountAssertionAndRejectsReplayWindowAbuse()throws Exception{
  var auth=new PlayerBearerAuthenticator(SECRET,clock);String value="7."+clock.instant().plusSeconds(60).getEpochSecond();
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  String token=value+"."+HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  assertEquals(7,auth.authenticate("Bearer "+token));assertThrows(SecurityException.class,()->auth.authenticate("Bearer 7."+clock.instant().plusSeconds(301).getEpochSecond()+".00"));
 }
}
