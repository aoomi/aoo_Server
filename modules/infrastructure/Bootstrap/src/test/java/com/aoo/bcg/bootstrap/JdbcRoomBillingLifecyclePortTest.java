package com.aoo.bcg.bootstrap;
import com.aoo.bcg.billing.*;import com.aoo.bcg.billing.RoomBillingSourceBinding.*;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import javax.sql.DataSource;import java.sql.*;import java.nio.file.*;import java.io.StringReader;import java.time.*;import java.util.*;
import org.h2.jdbcx.JdbcDataSource;import org.h2.tools.RunScript;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
final class JdbcRoomBillingLifecyclePortTest {
 static final Path ROOT=Path.of("/Users/aoo/Code/Game/BCG/Aoo");
 static JdbcDataSource database(){var d=new JdbcDataSource();d.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");return d;}
 static JdbcRoomBillingLifecyclePort port(DataSource d){return new JdbcRoomBillingLifecyclePort(d,()->{throw new AssertionError("no fund operation expected");},Clock.systemUTC());}
 static BoundIntent seed(DataSource d,long room,long actor,long game,String play,String region,RoomRuntimeIdentity id)throws Exception {
  try(var c=d.getConnection();var q=c.createStatement()){
   q.execute("CREATE TABLE IF NOT EXISTS aoo_room_create_saga(request_id VARCHAR(128) PRIMARY KEY,request_hash CHAR(64),account_id BIGINT,room_id BIGINT)");
   for(String n:List.of("V20261017_01__room_card_billing_intent_journal.sql","V20261017_02__room_billing_subject_source_binding.sql","V20261017_03__room_billing_business_outcome_receipt.sql"))RunScript.execute(c,new StringReader(Files.readString(ROOT.resolve("server/database/migrations/"+n))));
   q.execute("INSERT INTO aoo_room_create_saga VALUES('create-source-request-0001','"+"a".repeat(64)+"',"+actor+","+room+")");c.setAutoCommit(false);
   var key=new SourceKey("HALL_CREATE","create-source-request-0001","a".repeat(64),actor);
   var op=new RoomChargeOperation(room,"attempt","CREATE",actor,actor,game,play,region,RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.OWNER,"ROUND_COUNT",8,3,0);
   var bound=new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal()).bind(c,key,new RoomBillingIntent(op,Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)),new JdbcRoomChargeCoordinator.Subject(actor,actor,0),new AuthorityReference(11,1,"c".repeat(64),region));
   if(id!=null)port(d).snapshotCommitted(c,bound,id);c.commit();return bound;
  }
 }
 @Test void sourceAbsenceNeverBecomesLegacyPermission()throws Exception {var d=database();try(var c=d.getConnection();var q=c.createStatement()){q.execute("CREATE TABLE aoo_room_create_saga(request_id VARCHAR,request_hash VARCHAR,account_id BIGINT,room_id BIGINT)");c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->port(d).requireRoom(c,42));c.rollback();}}
 @Test void abortFencesRecoveryAndCommitProofRollback()throws Exception {var d=database();var b=seed(d,42,17,2003,"liangshan2003-v1.0.0","LS",null);var id=new RoomRuntimeIdentity("instance",7,11,4);
  try(var c=d.getConnection()){c.setAutoCommit(false);port(d).snapshotCommitted(c,port(d).requireRoom(c,42),id);c.rollback();}
  var outcomes=new JdbcRoomBillingBusinessOutcomeRepository(new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal()));
  try(var c=d.getConnection()){c.setAutoCommit(false);assertTrue(outcomes.find(c,b.key(),RoomBillingBusinessOutcome.EventType.CREATE_COMMITTED).isEmpty());outcomes.abortBeforeCommit(c,b.key());c.commit();}
  try(var c=d.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->port(d).requireRoom(c,42));c.rollback();}
 }
 @Test void tombstoneAndDestroyReceiptShareCallerRollback()throws Exception {var d=database();var id=new RoomRuntimeIdentity("instance",7,11,4);var b=seed(d,42,17,2003,"liangshan2003-v1.0.0","LS",id);
  try(var c=d.getConnection();var q=c.createStatement()){q.execute("CREATE TABLE aoo_room_runtime_identity_authority(room_id BIGINT PRIMARY KEY,room_instance_id VARCHAR,lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,lifecycle_state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP)");}
  var identities=new RoomRuntimeIdentityStore(d);identities.create(42,id);
  try(var c=d.getConnection()){c.setAutoCommit(false);port(d).destroy(c,identities,42,id,null);c.rollback();}
  assertTrue(identities.require(42).active());var outcomes=new JdbcRoomBillingBusinessOutcomeRepository(new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal()));
  try(var c=d.getConnection()){c.setAutoCommit(false);assertTrue(outcomes.find(c,b.key(),RoomBillingBusinessOutcome.EventType.CREATE_DESTROYED).isEmpty());port(d).destroy(c,identities,42,id,null);c.commit();}
  assertFalse(identities.require(42).active());
 }
 @Test void ordinaryWriterCannotCreateFirstReceiptAndOwnedAbortOnlyTombstones()throws Exception {
  var d=database();var id=new RoomRuntimeIdentity("owned",7,11,4);var bound=seed(d,42,17,2003,"liangshan2003-v1.0.0","LS",null);
  var receipts=new JdbcRoomBillingBusinessOutcomeRepository(new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal()));
  try(var c=d.getConnection();var q=c.createStatement()){q.execute("CREATE TABLE aoo_room_runtime_identity_authority(room_id BIGINT PRIMARY KEY,room_instance_id VARCHAR,lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,lifecycle_state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP)");c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->port(d).requireCommitted(c,bound,id));receipts.abortBeforeCommit(c,bound.key());c.commit();}
  var identities=new RoomRuntimeIdentityStore(d);identities.create(42,id);
  try(var c=d.getConnection()){c.setAutoCommit(false);assertThrows(IllegalStateException.class,()->port(d).destroy(c,identities,42,id,null));assertThrows(SecurityException.class,()->port(d).cleanupOwnedCreate(c,identities,42,id,"wrong-source-request",null));port(d).cleanupOwnedCreate(c,identities,42,id,bound.key().requestId(),null);c.rollback();}
  assertTrue(identities.require(42).active());
  try(var c=d.getConnection()){c.setAutoCommit(false);port(d).cleanupOwnedCreate(c,identities,42,id,bound.key().requestId(),null);assertTrue(receipts.find(c,bound.key(),RoomBillingBusinessOutcome.EventType.CREATE_DESTROYED).isEmpty());c.commit();}
  assertFalse(identities.require(42).active());
 }
 @Test void nanosecondClockProducesMillisecondReceiptThroughRealDao()throws Exception {
  var d=database();var bound=seed(d,42,17,2003,"liangshan2003-v1.0.0","LS",null);
  var nano=Instant.parse("2026-10-17T08:00:00.123456789Z");
  var lifecycle=new JdbcRoomBillingLifecyclePort(d,()->{throw new AssertionError("no debit expected");},Clock.fixed(nano,ZoneOffset.UTC));
  try(var c=d.getConnection()){c.setAutoCommit(false);lifecycle.snapshotCommitted(c,bound,new RoomRuntimeIdentity("nanoclock-instance",7,11,4));c.commit();}
  var receipts=new JdbcRoomBillingBusinessOutcomeRepository(new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal()));
  try(var c=d.getConnection()){c.setAutoCommit(false);var actual=receipts.find(c,bound.key(),RoomBillingBusinessOutcome.EventType.CREATE_COMMITTED).orElseThrow();assertEquals(Instant.parse("2026-10-17T08:00:00.123Z"),actual.createdAt());assertEquals(0,actual.createdAt().getNano()%1_000_000);c.rollback();}
 }
 @Test void typedCreateSnapshotBindsRequestAndOwnerBeforeWriting()throws Exception {
  var d=database();for(String statement:JdbcGatewayRoomAuthorityAtomicCommitTest.FIXTURE.split(";"))if(!statement.isBlank())JdbcGatewayRoomAuthorityAtomicCommitTest.sql(d,statement);
  seed(d,42,17,2003,"liangshan2003-v1.0.0","LS",null);
  JdbcGatewayRoomAuthorityAtomicCommitTest.sql(d,"INSERT INTO aoo_room_authority_route(room_id,game_id,play_version,last_request_id,trace_id,node_id,lifecycle_state,lease_expires_at,fencing_token) VALUES(42,2003,'liangshan2003-v1.0.0','create-source-request-0001','trace','synthetic-node','ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP),11)");
  JdbcGatewayRoomAuthorityAtomicCommitTest.sql(d,"INSERT INTO aoo_hall_room VALUES(42,2003,'liangshan2003-v1.0.0','LS',11,17,'{}','CLOSED')");
  JdbcGatewayRoomAuthorityAtomicCommitTest.sql(d,"INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash,immutable_rules) VALUES(42,2003,'LS','liangshan2003-v1.0.0',1,11,'"+"a".repeat(64)+"','"+"b".repeat(64)+"','codec-v1','events-v1','protocol-v1','snapshot-v1','"+"c".repeat(64)+"','{}')");
  var identity=new RoomRuntimeIdentity("source-matched-instance",7,11,4);new RoomRuntimeIdentityStore(d).create(42,identity);
  var session=(com.aoo.bcg.gamespi.AuthoritativeGameSession)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.aoo.bcg.gamespi.AuthoritativeGameSession.class},(proxy,method,args)->switch(method.getName()){case "stateVersion"->4L;case "authoritativeState"->Map.of("stateVersion",4L,"phase","WAITING");default->throw new AssertionError(method.getName());});
  var metadata=new com.aoo.bcg.gamespi.RoomSnapshotMetadataSource(11L,"a".repeat(64),"b".repeat(64),"codec-v1","events-v1","protocol-v1","snapshot-v1",1L,"LS","c".repeat(64));
  var room=new com.aoo.bcg.gamespi.GameRoomHandle(42,2003,"liangshan2003-v1.0.0",session,null,metadata,identity);var committer=JdbcGatewayRoomAuthorityAtomicCommitTest.committer(d);
  assertThrows(SecurityException.class,()->committer.saveCreateSnapshot(room,11,"other-source-request-0002",17));
  assertThrows(SecurityException.class,()->committer.saveCreateSnapshot(room,11,"create-source-request-0001",18));
  try(var c=d.getConnection();var q=c.createStatement()){for(String table:List.of("aoo_room_snapshot","aoo_room_billing_business_receipt")){try(var row=q.executeQuery("SELECT COUNT(*) FROM "+table)){assertTrue(row.next());assertEquals(0,row.getLong(1));}}}
  committer.saveCreateSnapshot(room,11,"create-source-request-0001",17);
  try(var c=d.getConnection();var q=c.createStatement()){for(String table:List.of("aoo_room_snapshot","aoo_room_billing_business_receipt")){try(var row=q.executeQuery("SELECT COUNT(*) FROM "+table)){assertTrue(row.next());assertEquals(1,row.getLong(1));}}}
 }
}
