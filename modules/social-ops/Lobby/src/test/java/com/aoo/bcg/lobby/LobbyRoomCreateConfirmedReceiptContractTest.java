package com.aoo.bcg.lobby;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.api.Trigger;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.logging.Logger;

final class LobbyRoomCreateConfirmedReceiptContractTest {
 static final String HASH="a".repeat(64), REQUEST="confirmed-request-0001";
 static final ObjectMapper JSON=new ObjectMapper();
 static Map<String,Object> request(){return Map.of("roomId",7001L,"gameId",2001,"playVersion","chengdu2001-v1.0.0","clientVersion","3.8.8","stateVersion",1L,"rules",Map.of("playerCount",3),"ruleRevision",Map.of("classificationRegionCode","CN-51-01","releaseId",11L,"indexGeneration",21L,"sourceHash",HASH));}
 static LobbyRoomCreateSaga.Command command(){return LobbyRoomCreateSaga.Command.parse(98,REQUEST,request()).withValidatedRules(Map.of("playerCount",3));}
 public static final class ImmutableReceipt implements Trigger {
  public void fire(Connection c,Object[] before,Object[] after)throws SQLException{throw new SQLException("immutable final receipt","45000");}
 }
 static final class FaultDataSource implements DataSource {
  final JdbcDataSource delegate=new JdbcDataSource();String fault="";boolean receiptTouched,unreadable;
  FaultDataSource(){delegate.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");}
  public Connection getConnection()throws SQLException{
   if(unreadable)throw new SQLException("injected reconciliation unavailable");
   Connection c=delegate.getConnection();c.createStatement().execute("SET TIME ZONE 'UTC'");
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(proxy,m,args)->{
    try{
     if(m.getName().equals("prepareStatement")&&args[0] instanceof String sql){
      // H2 fixture adaptation of existing MySQL date functions, not replacement persistence logic.
      args[0]=sql.replace("DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL ? SECOND)","DATEADD('SECOND',?,CURRENT_TIMESTAMP(3))").replace("DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 15 SECOND)","DATEADD('SECOND',-15,CURRENT_TIMESTAMP(3))").replace("UTC_TIMESTAMP(3)","CURRENT_TIMESTAMP(3)");
      PreparedStatement ps=(PreparedStatement)m.invoke(c,args);
      if(sql.startsWith("INSERT INTO aoo_room_create_confirmed_receipt"))return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(pp,pm,pa)->{
       try{if(pm.getName().equals("executeUpdate")){receiptTouched=true;if(fault.equals("insert")){fault="";throw new SQLException("injected receipt insert failure");}}return pm.invoke(ps,pa);}catch(InvocationTargetException x){throw x.getCause();}
      });
      return ps;
     }
     if(m.getName().equals("commit")&&receiptTouched&&!fault.isEmpty()){
      String f=fault;fault="";receiptTouched=false;
      if(f.equals("before")){c.rollback();throw new SQLException("injected commit before durable commit");}
      if(f.equals("after")||f.equals("unknown")){c.commit();if(f.equals("unknown"))unreadable=true;throw new SQLException("injected lost commit response");}
     }
     if(m.getName().equals("rollback"))receiptTouched=false;
     return m.invoke(c,args);
    }catch(InvocationTargetException x){throw x.getCause();}
   });
  }
  public Connection getConnection(String u,String p)throws SQLException{return getConnection();}
  public PrintWriter getLogWriter(){return null;}public void setLogWriter(PrintWriter p){}public void setLoginTimeout(int t){}public int getLoginTimeout(){return 0;}public Logger getParentLogger(){return Logger.getGlobal();}public <T>T unwrap(Class<T>c)throws SQLException{throw new SQLException("unsupported");}public boolean isWrapperFor(Class<?>c){return false;}
 }
 static final class Fixture {
  final FaultDataSource ds=new FaultDataSource();int removed,released,destroyed,legacyConfirm;boolean failBilling;final List<Boolean> replayFlags=new ArrayList<>();
  Fixture()throws Exception{
   sql("CREATE TABLE aoo_room_create_saga(request_id VARCHAR(128) PRIMARY KEY,request_hash VARCHAR(128),request_json CLOB,account_id BIGINT,room_id BIGINT UNIQUE,game_id BIGINT,play_version VARCHAR(64),state_version BIGINT,step VARCHAR(32),trace_id VARCHAR(64),created_at TIMESTAMP(3),updated_at TIMESTAMP(3),response_json CLOB,failure_code VARCHAR(128),attempt_count INT DEFAULT 0,next_retry_at TIMESTAMP,processing_token VARCHAR(128),processing_until TIMESTAMP,compensation_origin_step VARCHAR(32))");
   sql("CREATE TABLE aoo_room_create_confirmed_receipt(event_id CHAR(64) PRIMARY KEY,source_request_id VARCHAR(128) UNIQUE,request_hash VARCHAR(128),creator_id BIGINT,room_id BIGINT UNIQUE,game_id BIGINT,play_version VARCHAR(64),occurred_at TIMESTAMP(3),schema_version INT CHECK(schema_version=1))");
   sql("CREATE TRIGGER immutable_receipt BEFORE UPDATE, DELETE ON aoo_room_create_confirmed_receipt FOR EACH ROW CALL '"+ImmutableReceipt.class.getName()+"'");
   sql("CREATE TABLE aoo_game_catalog(game_id BIGINT PRIMARY KEY,game_code VARCHAR,display_name VARCHAR,family_code VARCHAR)");
   sql("INSERT INTO aoo_game_catalog VALUES(2001,'chengdu2001','public fixture','poker:pao-de-kuai')");
   sql("CREATE TABLE aoo_hall_room(room_id BIGINT PRIMARY KEY,game_id BIGINT,classification_region_code VARCHAR,play_version VARCHAR,release_id BIGINT,index_generation BIGINT,owner_account_id BIGINT,scope_type VARCHAR,club_id BIGINT,club_template_code VARCHAR,state VARCHAR,route_endpoint VARCHAR,rules_json CLOB,updated_at TIMESTAMP)");
   sql("INSERT INTO aoo_hall_room VALUES(7001,2001,'CN-51-01','chengdu2001-v1.0.0',11,21,98,'PERSONAL',NULL,NULL,'CREATING','ws://local','{\"playerCount\":3}',CURRENT_TIMESTAMP)");
   sql("CREATE TABLE aoo_hall_room_member(room_id BIGINT,account_id BIGINT,seat_no INT,status VARCHAR,PRIMARY KEY(room_id,account_id))");sql("INSERT INTO aoo_hall_room_member VALUES(7001,98,0,'JOINED')");
   sql("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT,region_code VARCHAR,play_version VARCHAR,index_generation BIGINT,release_id BIGINT,component_chain CLOB,rule_validator CLOB,ui_schema CLOB,bundle_hash VARCHAR)");
   sql("INSERT INTO aoo_compiled_room_create_index VALUES(2001,'CN-51-01','chengdu2001-v1.0.0',21,11,'[]','{}','{\"bundle\":\"paodekuai-common\",\"scene\":\"GameRoom2D\"}','"+HASH+"')");
   sql("CREATE TABLE aoo_room_runtime_identity_authority(room_id BIGINT PRIMARY KEY,room_instance_id VARCHAR,lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,lifecycle_state VARCHAR)");sql("INSERT INTO aoo_room_runtime_identity_authority VALUES(7001,'instance-fixture',1,1,0,'ACTIVE')");
   sql("CREATE TABLE aoo_room_rule_lock(room_id BIGINT PRIMARY KEY,game_id BIGINT,region_code VARCHAR,play_version VARCHAR,release_id BIGINT,index_generation BIGINT,source_hash VARCHAR,rule_hash VARCHAR,component_hash VARCHAR,card_codec_version VARCHAR,event_interpreter_version VARCHAR,protocol_version VARCHAR,snapshot_schema_version VARCHAR,immutable_rules CLOB)");
   sql("INSERT INTO aoo_room_rule_lock VALUES(7001,2001,'CN-51-01','chengdu2001-v1.0.0',11,21,'"+HASH+"','"+HASH+"','"+HASH+"','v1','v1','v1','v1','{\"playerCount\":3}')");
   sql("CREATE TABLE fixture_billing(request_id VARCHAR PRIMARY KEY,confirmed INT,confirm_writes INT)");
  }
  void sql(String sql)throws Exception{try(Connection c=ds.getConnection();Statement s=c.createStatement()){s.execute(sql);}}
  long number(String sql)throws Exception{try(Connection c=ds.getConnection();Statement s=c.createStatement();ResultSet v=s.executeQuery(sql)){v.next();return v.getLong(1);}}
  String text(String sql)throws Exception{try(Connection c=ds.getConnection();Statement s=c.createStatement();ResultSet v=s.executeQuery(sql)){v.next();return v.getString(1);}}
  LobbyRoomCreateSagaStore store(){return new LobbyRoomCreateSagaStore(ds,JSON);}
  JdbcLobbyRepository repo(){LobbyMembershipAuthorityPort member=(LobbyMembershipAuthorityPort)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{LobbyMembershipAuthorityPort.class},(o,m,a)->{throw new AssertionError("membership authority not called by completion");});return new JdbcLobbyRepository(ds,JSON,Clock.systemUTC(),member);}
  Map<String,Object> lease(){return Map.of("roomId",7001L,"gameId",2001,"playVersion","chengdu2001-v1.0.0","fencingToken",1L);}
  LobbyRoomCreateSaga saga(){
   JdbcLobbyRepository repository=repo();
   return new LobbyRoomCreateSaga(store(),new LobbyRoomCreateSaga.BillingPort(){
    public void reserve(LobbyRoomCreateSaga.Command c){try{sql("INSERT INTO fixture_billing VALUES('"+REQUEST+"',0,0)");}catch(Exception x){throw new IllegalStateException(x);}}
    public void confirm(LobbyRoomCreateSaga.Command c){if(failBilling)throw new IllegalStateException("injected billing failure");try{sql("UPDATE fixture_billing SET confirmed=1,confirm_writes=confirm_writes+1 WHERE request_id='"+REQUEST+"' AND confirmed=0");}catch(Exception x){throw new IllegalStateException(x);}}
    public void release(LobbyRoomCreateSaga.Command c){released++;}
   },new LobbyRoomCreateSaga.RoomPort(){
    public void register(LobbyRoomCreateSaga.Command c){}
    public Map<String,Object> confirm(LobbyRoomCreateSaga.Command c,long v){legacyConfirm++;throw new AssertionError("typed production completion required");}
    public LobbyRoomCreateSaga.RoomConfirmation confirmCompletion(LobbyRoomCreateSaga.Command c,long v){var out=repository.confirmRoomCompletion(c,v);replayFlags.add(out.previouslyConfirmed());return out;}
    public void remove(LobbyRoomCreateSaga.Command c){removed++;}
   },c->c.withValidatedRules(Map.of("playerCount",3)),new LobbyRoomCreateSaga.AuthorityPort(){
    public Map<String,Object> create(LobbyRoomCreateSaga.Command c){return lease();}public Map<String,Object> recover(LobbyRoomCreateSaga.Command c){return lease();}public Map<String,Object> destroy(LobbyRoomCreateSaga.Command c,Map<String,Object>x){destroyed++;return x;}
   });
  }
  void authorityState()throws Exception{var s=store();s.begin(command());s.advance(REQUEST,1,LobbyRoomCreateSaga.Step.RESERVED,null);s.advance(REQUEST,2,LobbyRoomCreateSaga.Step.REGISTERED,null);s.advance(REQUEST,3,LobbyRoomCreateSaga.Step.AUTHORITY_CREATED,lease());}
  void legacyRequestFormat()throws Exception{setJson("request_json",new LinkedHashMap<>(command().requestBody()));}
  void setJson(String column,Map<String,Object> value)throws Exception{if(!Set.of("request_json","response_json").contains(column))throw new AssertionError("fixture column");try(Connection c=ds.getConnection();PreparedStatement q=c.prepareStatement("UPDATE aoo_room_create_saga SET "+column+"=? WHERE request_id=?")){q.setString(1,JSON.writeValueAsString(value));q.setString(2,REQUEST);q.executeUpdate();}}
  Map<String,Object> storedJson(String column)throws Exception{return JSON.readValue(text("SELECT "+column+" FROM aoo_room_create_saga"),new com.fasterxml.jackson.core.type.TypeReference<>(){});}
  void noCompensation(){assertEquals(0,removed);assertEquals(0,released);assertEquals(0,destroyed);}
 }
 @Test void finalStateAndReceiptCommitOnceAndReplayKeepsOccurrence()throws Exception{
  Fixture f=new Fixture();var response=f.saga().create(98,REQUEST,request());String time=f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt");assertEquals("OPEN",response.get("state"));assertEquals("CONFIRMED",f.text("SELECT step FROM aoo_room_create_saga"));assertEquals(1,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));assertEquals(98,f.number("SELECT creator_id FROM aoo_room_create_confirmed_receipt"));
  f.saga().create(98,REQUEST,request());assertEquals(time,f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt"));assertEquals(1,f.number("SELECT confirm_writes FROM fixture_billing"));assertEquals(List.of(false),f.replayFlags);assertEquals(0,f.legacyConfirm);f.noCompensation();
 }
 @Test void receiptInsertRollbackThenFreshInstancesRecoverPersistedOpen()throws Exception{
  Fixture f=new Fixture();f.ds.fault="insert";assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.saga().create(98,REQUEST,request()));
  assertEquals("AUTHORITY_CREATED",f.text("SELECT step FROM aoo_room_create_saga"));assertEquals("OPEN",f.text("SELECT state FROM aoo_hall_room"));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));f.noCompensation();
  f.sql("UPDATE aoo_room_create_saga SET updated_at=TIMESTAMP '2000-01-01 00:00:00'");var fresh=f.saga();var report=fresh.recoverPending(8);assertEquals(1,report.completed());assertEquals(0,report.pending());assertEquals(List.of(false,true),f.replayFlags);assertEquals(1,f.number("SELECT confirm_writes FROM fixture_billing"));assertEquals(1,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));f.noCompensation();
 }
 @Test void lostSuccessfulCommitResponseReconcilesBothDurableFacts()throws Exception{
  Fixture f=new Fixture();f.ds.fault="after";f.saga().create(98,REQUEST,request());assertEquals(1,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));f.noCompensation();
 }
 @Test void failedCommitBeforeDurabilityRecoversWithoutCompensation()throws Exception{
  Fixture f=new Fixture();f.ds.fault="before";assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.saga().create(98,REQUEST,request()));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));f.saga().create(98,REQUEST,request());assertEquals(List.of(false,true),f.replayFlags);assertEquals(1,f.number("SELECT confirm_writes FROM fixture_billing"));f.noCompensation();
 }
 @Test void unknownCommitAndUnreadableReconciliationNeverCompensates()throws Exception{
  Fixture f=new Fixture();f.ds.fault="unknown";assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()));f.ds.unreadable=false;assertEquals("HALL_CREATE_IN_PROGRESS",assertThrows(com.aoo.bcg.hall.http.HallError.class,()->f.saga().create(98,REQUEST,request())).code());f.sql("UPDATE aoo_room_create_saga SET processing_until=TIMESTAMP '2000-01-01 00:00:00'");assertEquals(1,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));String time=f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt");f.saga().create(98,REQUEST,request());assertEquals(time,f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt"));f.noCompensation();
 }
 @Test void replayBillingFailureIsNotCompensated()throws Exception{
  Fixture f=new Fixture();f.ds.fault="insert";assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()));f.failBilling=true;assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.saga().create(98,REQUEST,request()));assertEquals("AUTHORITY_CREATED",f.text("SELECT step FROM aoo_room_create_saga"));f.noCompensation();
 }
 @Test void firstBillingFailureKeepsOriginalCompensation()throws Exception{
  Fixture f=new Fixture();f.failBilling=true;assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()));assertEquals(1,f.removed);assertEquals(1,f.released);assertEquals(1,f.destroyed);assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));
 }
 @Test void everyDurableBindingMismatchAndNonOpenStateRejects()throws Exception{
  String[] changes={"UPDATE aoo_room_create_saga SET request_hash='wrong'","UPDATE aoo_room_create_saga SET account_id=99","UPDATE aoo_room_create_saga SET game_id=2003","UPDATE aoo_room_create_saga SET play_version='other'","UPDATE aoo_hall_room SET owner_account_id=99","UPDATE aoo_hall_room SET release_id=12","UPDATE aoo_hall_room SET index_generation=22","UPDATE aoo_hall_room SET classification_region_code='other'","UPDATE aoo_hall_room SET scope_type='CLUB'","UPDATE aoo_hall_room SET rules_json='{}'","UPDATE aoo_room_rule_lock SET source_hash='other'","UPDATE aoo_hall_room_member SET seat_no=1","UPDATE aoo_hall_room_member SET status='LEFT'","UPDATE aoo_hall_room SET state='DISSOLVED'"};
  for(String change:changes){Fixture f=new Fixture();f.authorityState();f.sql(change);assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.repo().confirmRoomCompletion(command(),5),change);assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));}
 }
 @Test void requestVersionAndArbitraryOpenCannotBypassBinding()throws Exception{
  Fixture f=new Fixture();f.authorityState();f.sql("UPDATE aoo_hall_room SET state='OPEN'");assertThrows(RuntimeException.class,()->f.repo().confirmRoom(98,7001));assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.repo().confirmRoomCompletion(command(),6));var bad=new HashMap<>(request());bad.put("roomId",7002L);var c=LobbyRoomCreateSaga.Command.parse(98,"different-request-0002",bad).withValidatedRules(Map.of("playerCount",3));assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.repo().confirmRoomCompletion(c,5));
 }
 @Test void receiptIsImmutableAndRetainedAndUniquePerRoom()throws Exception{
  Fixture f=new Fixture();f.saga().create(98,REQUEST,request());assertThrows(SQLException.class,()->f.sql("UPDATE aoo_room_create_confirmed_receipt SET occurred_at=CURRENT_TIMESTAMP"));assertThrows(SQLException.class,()->f.sql("DELETE FROM aoo_room_create_confirmed_receipt"));assertThrows(SQLException.class,()->f.sql("INSERT INTO aoo_room_create_confirmed_receipt SELECT REPEAT('b',64),'different-request',request_hash,creator_id,room_id,game_id,play_version,occurred_at,1 FROM aoo_room_create_confirmed_receipt"));
 }
 @Test void finalCasConflictAndCorruptReceiptCannotReportSuccess()throws Exception{
  Fixture f=new Fixture();f.authorityState();assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.store().advance(REQUEST,3,LobbyRoomCreateSaga.Step.CONFIRMED,Map.of()));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));
 }
 @Test void oldConfirmedResponseDoesNotInventHistoricOccurrence()throws Exception{
  Fixture f=new Fixture();f.authorityState();f.legacyRequestFormat();f.sql("UPDATE aoo_room_create_saga SET step='CONFIRMED',response_json='{\"state\":\"OPEN\"}'");assertEquals("OPEN",f.saga().create(98,REQUEST,request()).get("state"));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));
 }
 @Test void occurrenceIsUtcMillisecondsAndHalfOpenBoundary()throws Exception{
  Fixture f=new Fixture();Instant before=Instant.now().minusMillis(1);f.saga().create(98,REQUEST,request());try(Connection c=f.ds.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT occurred_at FROM aoo_room_create_confirmed_receipt")){r.next();Instant time=r.getTimestamp(1,Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant();assertFalse(time.isBefore(before));assertFalse(time.isAfter(Instant.now()));assertEquals(0,time.getNano()%1000000);try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt WHERE occurred_at>=? AND occurred_at<?")){q.setTimestamp(1,Timestamp.from(time));q.setTimestamp(2,Timestamp.from(time));try(ResultSet v=q.executeQuery()){v.next();assertEquals(0,v.getInt(1));}q.setTimestamp(2,Timestamp.from(time.plusMillis(1)));try(ResultSet v=q.executeQuery()){v.next();assertEquals(1,v.getInt(1));}}}
 }
 @Test void forwardDdlContainsOnlyNewReceiptAndImmutableGuards()throws Exception{
  String ddl=java.nio.file.Files.readString(java.nio.file.Path.of("server/database/migrations/V20261017_07__room_create_confirmed_receipt.sql"));assertTrue(ddl.contains("CREATE TABLE aoo_room_create_confirmed_receipt"));assertTrue(ddl.contains("BEFORE UPDATE"));assertTrue(ddl.contains("BEFORE DELETE"));assertFalse(ddl.contains("ALTER TABLE"));assertFalse(ddl.contains("INSERT INTO"));
 }
 @Test void serverProvenanceCannotComeFromClientAndNeverLeaksPublicResponse()throws Exception{
  Fixture f=new Fixture();var client=new HashMap<>(request());client.put("__aooRoomCreateReceipt",Map.of("schemaVersion",99));Map<String,Object> response=f.saga().create(98,REQUEST,client);
  assertFalse(response.containsKey("__aooRoomCreateReceipt"));assertFalse(f.store().find(REQUEST).response().containsKey("__aooRoomCreateReceipt"));
  assertEquals(command().requestHash(),f.text("SELECT request_hash FROM aoo_room_create_saga"));
  Map<?,?> requestMarker=(Map<?,?>)f.storedJson("request_json").get("__aooRoomCreateReceipt"),responseMarker=(Map<?,?>)f.storedJson("response_json").get("__aooRoomCreateReceipt");assertEquals(1,requestMarker.get("schemaVersion"));assertEquals(requestMarker,responseMarker);assertEquals(command().requestHash(),requestMarker.get("requestHash"));
  Fixture pending=new Fixture();pending.authorityState();pending.sql("UPDATE aoo_room_create_saga SET updated_at=TIMESTAMP '2000-01-01 00:00:00'");assertFalse(pending.store().pending(1).get(0).request().containsKey("__aooRoomCreateReceipt"));
 }
 @Test void newConfirmedMissingResponseProvenanceIsNotGuessedLegacy()throws Exception{
  Fixture f=new Fixture();f.saga().create(98,REQUEST,request());var response=f.storedJson("response_json");response.remove("__aooRoomCreateReceipt");f.setJson("response_json",response);
  assertThrows(RuntimeException.class,()->f.store().find(REQUEST));assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()));f.noCompensation();
 }
 @Test void earlyConfirmedReplayRejectsMissingOrCrossIdentityCorruptReceipts()throws Exception{
  String[] changes={"DELETE FROM aoo_room_create_confirmed_receipt","UPDATE aoo_room_create_confirmed_receipt SET creator_id=99","UPDATE aoo_room_create_confirmed_receipt SET request_hash='wrong'","UPDATE aoo_room_create_confirmed_receipt SET game_id=2003","UPDATE aoo_room_create_confirmed_receipt SET event_id=REPEAT('b',64)","UPDATE aoo_room_create_confirmed_receipt SET occurred_at=NULL"};
  for(String change:changes){Fixture f=new Fixture();f.saga().create(98,REQUEST,request());f.sql("DROP TRIGGER immutable_receipt");f.sql(change);assertThrows(RuntimeException.class,()->f.store().find(REQUEST),change);assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()),change);assertEquals("CONFIRMED",f.text("SELECT step FROM aoo_room_create_saga"));f.noCompensation();}
 }
 @Test void unknownOrDamagedRequestProvenanceFailsClosed()throws Exception{
  for(String field:List.of("schemaVersion","producer","requestHash","eventId")){Fixture f=new Fixture();f.saga().create(98,REQUEST,request());var request=f.storedJson("request_json");Map<String,Object> marker=new HashMap<>((Map<String,Object>)request.get("__aooRoomCreateReceipt"));marker.put(field,field.equals("schemaVersion")?99:"wrong");request.put("__aooRoomCreateReceipt",marker);f.setJson("request_json",request);assertThrows(RuntimeException.class,()->f.store().find(REQUEST));f.noCompensation();}
 }
 @Test void unknownOrDamagedResponseProvenanceFailsClosed()throws Exception{
  for(String field:List.of("schemaVersion","producer","requestHash","eventId")){Fixture f=new Fixture();f.saga().create(98,REQUEST,request());var response=f.storedJson("response_json");Map<String,Object> marker=new HashMap<>((Map<String,Object>)response.get("__aooRoomCreateReceipt"));marker.put(field,field.equals("schemaVersion")?99:"wrong");response.put("__aooRoomCreateReceipt",marker);f.setJson("response_json",response);assertThrows(RuntimeException.class,()->f.store().find(REQUEST));f.noCompensation();}
 }
 @Test void genuineLegacyPendingGetsNewReceiptButNoLegacyHistoryIsBackfilled()throws Exception{
  Fixture f=new Fixture();f.authorityState();f.legacyRequestFormat();f.sql("INSERT INTO fixture_billing VALUES('"+REQUEST+"',1,1)");Map<String,Object> response=f.saga().create(98,REQUEST,request());assertEquals("OPEN",response.get("state"));assertTrue(f.storedJson("request_json").containsKey("__aooRoomCreateReceipt"));assertTrue(f.storedJson("response_json").containsKey("__aooRoomCreateReceipt"));String first=f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt");f.saga().create(98,REQUEST,request());assertEquals(first,f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt"));f.sql("DROP TRIGGER immutable_receipt");f.sql("DELETE FROM aoo_room_create_confirmed_receipt");assertThrows(RuntimeException.class,()->f.store().find(REQUEST));f.noCompensation();
 }

 @Test void legacyPendingMissingFinalMarkerCannotRegressToLegacyBypass()throws Exception{
  for(boolean removeReceipt:List.of(false,true)){Fixture f=new Fixture();f.authorityState();f.legacyRequestFormat();f.sql("INSERT INTO fixture_billing VALUES('"+REQUEST+"',1,1)");f.saga().create(98,REQUEST,request());assertTrue(f.storedJson("request_json").containsKey("__aooRoomCreateReceipt"));var response=f.storedJson("response_json");response.remove("__aooRoomCreateReceipt");f.setJson("response_json",response);if(removeReceipt){f.sql("DROP TRIGGER immutable_receipt");f.sql("DELETE FROM aoo_room_create_confirmed_receipt");}assertThrows(RuntimeException.class,()->f.store().find(REQUEST));assertThrows(RuntimeException.class,()->f.saga().create(98,REQUEST,request()));f.noCompensation();}
 }
 @Test void legacyPendingIsUpgradedOnlyAfterOriginalCanonicalIdentityAndShapeValidation()throws Exception{
  for(String field:List.of("roomId","gameId","playVersion","clientVersion","rules","admission","_creationOrigin","unexpected")){Fixture f=new Fixture();f.authorityState();f.legacyRequestFormat();var body=f.storedJson("request_json");Object wrong=switch(field){case "roomId"->7002L;case "gameId"->2003;case "rules"->Map.of("playerCount",2);case "admission"->Map.of("ipAddress","different");default->"wrong";};body.put(field,wrong);f.setJson("request_json",body);assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.store().advance(REQUEST,4,LobbyRoomCreateSaga.Step.CONFIRMED,Map.of("state","OPEN")),field);assertEquals("AUTHORITY_CREATED",f.text("SELECT step FROM aoo_room_create_saga"));assertFalse(f.storedJson("request_json").containsKey("__aooRoomCreateReceipt"));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));}
 }
 @Test void legacyPendingUpgradeAndFinalResponseAndReceiptRollbackTogether()throws Exception{
  Fixture f=new Fixture();f.authorityState();f.legacyRequestFormat();f.ds.fault="insert";assertThrows(LobbyRoomCreateSaga.FinalConfirmationRecoveryException.class,()->f.store().advance(REQUEST,4,LobbyRoomCreateSaga.Step.CONFIRMED,Map.of("state","OPEN")));assertEquals("AUTHORITY_CREATED",f.text("SELECT step FROM aoo_room_create_saga"));assertFalse(f.storedJson("request_json").containsKey("__aooRoomCreateReceipt"));assertFalse(f.storedJson("response_json").containsKey("__aooRoomCreateReceipt"));assertEquals(0,f.number("SELECT COUNT(*) FROM aoo_room_create_confirmed_receipt"));
  var state=f.store().advance(REQUEST,4,LobbyRoomCreateSaga.Step.CONFIRMED,Map.of("state","OPEN"));assertFalse(state.response().containsKey("__aooRoomCreateReceipt"));assertTrue(f.storedJson("request_json").containsKey("__aooRoomCreateReceipt"));assertTrue(f.storedJson("response_json").containsKey("__aooRoomCreateReceipt"));String time=f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt");f.store().find(REQUEST);assertEquals(time,f.text("SELECT occurred_at FROM aoo_room_create_confirmed_receipt"));
 }

}
