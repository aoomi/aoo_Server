package com.aoo.bcg.bootstrap;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.sql.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.common.settlement.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.nio.file.*;
public final class JdbcGatewayRoomAuthorityAtomicCommitTest{
 static final Path E=System.getProperty("probe.evidence") == null ? null : Path.of(System.getProperty("probe.evidence"));
 static final ObjectMapper JSON=new ObjectMapper();
 static final String H1="a".repeat(64),H2="b".repeat(64),H3="c".repeat(64);
 static RoomRuntimeIdentity id(long v){return new RoomRuntimeIdentity("synthetic-instance-42",7,11,v);}
 static void sql(DataSource d,String s)throws Exception{try(var c=d.getConnection();var q=c.createStatement()){q.execute(s);}}
 static List<Object> persisted(DataSource d)throws Exception{try(var c=d.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT i.state_version,s.state_version,s.last_event_sequence,b.status FROM aoo_room_runtime_identity_authority i JOIN aoo_room_snapshot s ON s.room_id=i.room_id JOIN aoo_business_idempotency b ON b.room_id=i.room_id WHERE i.room_id=42")){assertTrue(r.next());return List.of(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4));}}
 static JdbcGatewayGameCommandCommitter committer(DataSource d){
  var games=GameRegistryFactory.production();
  var settle=new SettlementExecutor(new SettlementRepository(){public SettlementResult save(String id,SettlementResult s){throw new AssertionError("settlement outside scope");}public Optional<SettlementResult> find(String id){throw new AssertionError("settlement outside scope");}});
  return new JdbcGatewayGameCommandCommitter(d,JSON,Clock.systemUTC(),Duration.ofHours(24),new DurableGameSettlementService(games,settle));
 }
 static DataSource failing(DataSource d,String target,AtomicInteger hits){
  return (DataSource)Proxy.newProxyInstance(JdbcGatewayRoomAuthorityAtomicCommitTest.class.getClassLoader(),new Class[]{DataSource.class},(p,m,a)->{
   try{Object out=m.invoke(d,a);if(m.getName().equals("getConnection")){Connection real=(Connection)out;return Proxy.newProxyInstance(JdbcGatewayRoomAuthorityAtomicCommitTest.class.getClassLoader(),new Class[]{Connection.class},(cp,cm,ca)->{if (cm.getName().equals("commit") && target.equals("ackLost") && hits.getAndIncrement() == 0) { real.commit(); throw new SQLException("synthetic acknowledgement lost after actual commit"); }
     if (target.equals("preCAS") && cm.getName().equals("prepareStatement") && String.valueOf(ca[0]).startsWith("UPDATE aoo_room_runtime_identity_authority") && hits.getAndIncrement() == 0) throw new SQLException("synthetic failure before CAS update");
     if(cm.getName().equals(target)&&hits.getAndIncrement()==0)throw new SQLException("synthetic one-shot "+target+" failure before actual commit");try{return cm.invoke(real,ca);}catch(InvocationTargetException x){throw x.getCause();}});}return out;}catch(InvocationTargetException x){throw x.getCause();}
  });
 }
 static JdbcGatewayRoomAuthority authority(DataSource d){
  var clock=Clock.systemUTC();return new JdbcGatewayRoomAuthority(d,GameRegistryFactory.production(),new com.aoo.bcg.gateway.RuntimeGameRoomRegistry(),clock,"synthetic-node","unit-test://no-network",JSON,committer(d),new com.aoo.bcg.gateway.GatewayRoomBroadcastHub((r,a)->{throw new AssertionError("broadcast outside scope");},JSON,clock),new HallRoomLifecycleClient(java.net.URI.create("unit-test://no-network"),"synthetic-only-not-live-token-xxxxxxxx",JSON));
 }
 static String recovery(DataSource d,long snapshotVersion)throws Exception{
  try(var a=authority(d)){a.close();var m=JdbcGatewayRoomAuthority.class.getDeclaredMethod("advanceIdentityIfRequired",long.class,RoomRuntimeIdentity.class,long.class);m.setAccessible(true);try{var out=m.invoke(a,42L,new RoomRuntimeIdentityStore(d).require(42).identity(),snapshotVersion);return "RECOVERED:"+((RoomRuntimeIdentity)out).stateVersion();}catch(InvocationTargetException x){return x.getCause().getClass().getName()+":"+x.getCause().getMessage();}}
 }

 static final String FIXTURE = """
CREATE TABLE aoo_room_runtime_identity_authority(room_id BIGINT PRIMARY KEY,room_instance_id VARCHAR(128),lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,lifecycle_state VARCHAR(16),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE aoo_room_authority_route(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),last_request_id VARCHAR(100),trace_id VARCHAR(100),node_id VARCHAR(100),lifecycle_state VARCHAR(100),lease_expires_at TIMESTAMP,fencing_token BIGINT,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,first_round_started_at TIMESTAMP,last_business_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE aoo_hall_room(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),classification_region_code VARCHAR(100),release_id BIGINT,owner_account_id BIGINT,rules_json CLOB,state VARCHAR(100));
CREATE TABLE aoo_game_catalog(game_id INT,game_code VARCHAR(100));
CREATE TABLE aoo_room_rule_lock(room_id BIGINT,game_id INT,region_code VARCHAR(100),play_version VARCHAR(100),index_generation BIGINT,release_id BIGINT,rule_hash VARCHAR(64),component_hash VARCHAR(64),card_codec_version VARCHAR(100),event_interpreter_version VARCHAR(100),protocol_version VARCHAR(100),snapshot_schema_version VARCHAR(100),source_hash VARCHAR(64),immutable_rules CLOB,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE aoo_room_snapshot(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),release_id BIGINT,component_version VARCHAR(100),rule_content_hash VARCHAR(64),component_content_hash VARCHAR(64),card_codec_version VARCHAR(100),event_interpreter_version VARCHAR(100),protocol_version VARCHAR(100),fencing_token BIGINT,state_version BIGINT,last_event_sequence BIGINT,captured_at TIMESTAMP,state_payload CLOB);
CREATE TABLE aoo_business_idempotency(request_id VARCHAR(1024) PRIMARY KEY,user_id VARCHAR(100),operation VARCHAR(100),room_id BIGINT,round_no INT,client_request_id VARCHAR(100),status VARCHAR(100),response_code INT,response_version VARCHAR(100),response_schema_version INT,response_payload CLOB,expires_at TIMESTAMP,request_hash VARCHAR(64),created_at TIMESTAMP);
CREATE ALIAS SHA2 FOR "com.aoo.bcg.bootstrap.JdbcGatewayRoomAuthorityAtomicCommitTest.sha2";
""";
 public static String sha2(String s,int bits)throws Exception{if(bits!=256)throw new IllegalArgumentException();return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
 @Test void actualExecuteCommitsOrRollsBackAllDurableVersions()throws Exception{
  var origins=new LinkedHashMap<String,String>();for(var c:List.of(RoomRuntimeIdentityStore.class,JdbcGatewayGameCommandCommitter.class,JdbcGatewayRoomAuthority.class))origins.put(c.getName(),c.getProtectionDomain().getCodeSource().getLocation().toString());if (E != null) JSON.writeValue(E.resolve("class-origin.json").toFile(),origins);
  for (String location : origins.values()) assertTrue(location.contains("/classes/"));
  var obs=new ArrayList<Map<String,Object>>();
  for(String mode:List.of("control","preCAS","commitWindow","ackLost")){
   var db=new JdbcDataSource();db.setURL("jdbc:h2:mem:aoo_identity_snapshot_window_execute_"+mode+"_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");JdbcGatewayRoomAuthority a=null;
   try{
    for(String s:FIXTURE.split(";"))if(!s.isBlank())sql(db,s);
    sql(db,"INSERT INTO aoo_room_authority_route(room_id,game_id,play_version,last_request_id,trace_id,node_id,lifecycle_state,lease_expires_at,fencing_token)VALUES(42,2003,'liangshan2003-v1.0.0','initial','trace','synthetic-node','ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP),11)");
    sql(db,"INSERT INTO aoo_hall_room VALUES(42,2003,'liangshan2003-v1.0.0','LS',11,17,'{}','CLOSED')");
    sql(db,"INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash,immutable_rules)VALUES(42,2003,'LS','liangshan2003-v1.0.0',1,11,'"+H1+"','"+H2+"','codec-v1','events-v1','protocol-v1','snapshot-v1','"+H3+"','{}')");
    sql(db,"INSERT INTO aoo_room_snapshot(room_id,fencing_token,state_version,last_event_sequence,state_payload) VALUES(42,11,4,4,'{}')");new RoomRuntimeIdentityStore(db).create(42,id(4));
    JdbcRoomBillingLifecyclePortTest.seed(db,42,17,2003,"liangshan2003-v1.0.0","LS",id(4));
    AtomicLong version=new AtomicLong(4);AtomicInteger mutations=new AtomicInteger(),hits=new AtomicInteger();
    var session=(AuthoritativeGameSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{AuthoritativeGameSession.class},(p,m,args)->switch(m.getName()){
     case "stateVersion"->version.get();case "authoritativeState"->Map.of("stateVersion",version.get(),"phase","WAITING","roundNo",0,"players",Map.of(1,17L));
     case "execute"->{mutations.incrementAndGet();version.incrementAndGet();GameCommandRequest r=(GameCommandRequest)args[0];yield new GameCommandResult("synthetic.state_rsp",r.requestId(),CommandPayload.copyOf(Map.of("synthetic",true)),System.currentTimeMillis(),Map.of());}
     default->throw new AssertionError("unexpected synthetic session call "+m.getName());});
    var room=new GameRoomHandle(42,2003,"liangshan2003-v1.0.0",session,null,new RoomSnapshotMetadataSource(11L,H1,H2,"codec-v1","events-v1","protocol-v1","snapshot-v1",1L,"LS",H3),id(4));
    var registry=new com.aoo.bcg.gateway.RuntimeGameRoomRegistry();registry.bindRuntime(room);
    var games=GameRegistryFactory.production();games.register(new CatalogGameProvider(new GameDescriptor(2003,"liangshan2003","synthetic-provider",GameCategory.POKER,"poker",RegionScope.CITY,"SC","LS","liangshan2003-v1.0.0"),true,"synthetic-fixture"));
    var c=committer(switch (mode) { case "commitWindow" -> failing(db,"commit",hits); case "preCAS" -> failing(db,"preCAS",hits); case "ackLost" -> failing(db,"ackLost",hits); default -> db; });var clock=Clock.systemUTC();
    a=new JdbcGatewayRoomAuthority(db,games,registry,clock,"synthetic-node","unit-test://no-network",JSON,c,new com.aoo.bcg.gateway.GatewayRoomBroadcastHub((r,account)->{throw new AssertionError("no broadcast");},JSON,clock),new HallRoomLifecycleClient(java.net.URI.create("unit-test://no-network"),"synthetic-only-not-live-token-xxxxxxxx",JSON));a.close();
    var request=new GameCommandRequest("synthetic.state_req","request-1",1,42,0,"liangshan2003-v1.0.0","17",1,Map.of("action","state"));
    var command=new RoomOrchestrator.ExecuteCommand(new RoomOrchestrator.RoomIdentity(42,"synthetic-instance-42",2003,"liangshan2003","liangshan2003-v1.0.0",7,11,4),"synthetic-session",request);
    String failure="NONE";if(mode.equals("control") || mode.equals("ackLost")){var out=a.execute(command);assertEquals(5,out.identity().stateVersion());assertEquals(List.of(5L,5L,5L,"COMPLETED"),persisted(db));}
    else{JdbcGatewayRoomAuthority actual=a;var ex=assertThrows(IllegalStateException.class,()->actual.execute(command));failure=ex.getClass().getName()+":"+ex.getMessage()+" cause="+ex.getCause().getClass().getName()+":"+ex.getCause().getMessage();assertEquals(1,hits.get());assertEquals(List.of(4L,4L,4L,"UNKNOWN"),persisted(db));}
    assertEquals(1,mutations.get());
    long durableVersion = mode.equals("control") || mode.equals("ackLost") ? 5L : 4L;
    assertEquals(durableVersion,registry.requireRuntime(42).runtimeIdentity().stateVersion());
    assertEquals("RECOVERED:" + durableVersion, recovery(db, durableVersion));
    if (mode.equals("ackLost")) assertEquals(1,hits.get());
    // A failed transaction leaves the mutated in-memory session fail-closed; no fake rollback.
    if (durableVersion == 4) { JdbcGatewayRoomAuthority actual = a; assertThrows(IllegalStateException.class, () -> actual.execute(command)); assertEquals(1, mutations.get()); }
    else { JdbcGatewayRoomAuthority actual = a; assertThrows(IllegalStateException.class, () -> actual.execute(command)); }
    obs.add(Map.of("case",mode,"before",List.of(4L,4L,4L,"NO_RECEIPT"),"after",persisted(db),"sessionMutations",mutations.get(),"sessionVersion",version.get(),"registryVersion",registry.requireRuntime(42).runtimeIdentity().stateVersion(),"commitInjectionHits",hits.get(),"exception",failure));if (E != null) JSON.writeValue(E.resolve("observations.json").toFile(),obs);
   }finally{if(a!=null)a.close();sql(db,"SHUTDOWN");}
  }
 }
}
