package com.aoo.bcg.telemetry;
import com.fasterxml.jackson.databind.ObjectMapper;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.*;import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class TelemetryRiskServiceTest {
 JdbcDataSource ds;JdbcTelemetryRiskService service;Instant now=Instant.parse("2026-08-24T00:00:00Z");
 @BeforeEach void setup()throws Exception{ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:t"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");try(var c=ds.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE telemetry_signal(signal_id BIGINT AUTO_INCREMENT PRIMARY KEY,event_id VARCHAR(64),player_id BIGINT,signal_kind VARCHAR(24),occurred_at TIMESTAMP,received_at TIMESTAMP,session_id VARCHAR(64),room_id BIGINT,device_hash CHAR(64),network_prefix VARCHAR(64),geo_cell VARCHAR(32),attributes_json JSON,UNIQUE(player_id,event_id))");s.execute("CREATE TABLE risk_event(risk_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT,room_id BIGINT,source_signal_id BIGINT UNIQUE,risk_score SMALLINT,reasons_json JSON,status VARCHAR(24),created_at TIMESTAMP)");s.execute("CREATE TABLE risk_review_directive(directive_id BIGINT AUTO_INCREMENT PRIMARY KEY,risk_event_id BIGINT UNIQUE,directive_type VARCHAR(32),status VARCHAR(16),created_at TIMESTAMP)");}service=new JdbcTelemetryRiskService(ds,new ObjectMapper(),Clock.fixed(now,ZoneOffset.UTC));}
 @Test void deduplicatesAndCropsPrivateData()throws Exception{var s=signal("device0001",TelemetrySignal.Kind.DEVICE,null,"raw-device",Map.of("rooted",false,"email","secret@example.test"));assertEquals("ACCEPTED",service.ingest(7,s).status());assertEquals("DUPLICATE",service.ingest(7,s).status());try(var c=ds.getConnection();var q=c.prepareStatement("SELECT device_hash,network_prefix,geo_cell,attributes_json FROM telemetry_signal");var r=q.executeQuery()){assertTrue(r.next());assertNotEquals("raw-device",r.getString(1));assertEquals("10.2.3.0/24",r.getString(2));assertEquals("31.23,121.47",r.getString(3));assertFalse(r.getString(4).contains("secret"));}}
 @Test void emitsReviewOnlyNotPunishment()throws Exception{var r=service.ingest(9,signal("risk00001",TelemetrySignal.Kind.DEVICE,42L,"dev",Map.of("rooted",true,"mockLocation",true)));assertNotNull(r.riskEventId());try(var c=ds.getConnection();var q=c.prepareStatement("SELECT directive_type,status FROM risk_review_directive");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("REVIEW_TABLE",rows.getString(1));assertEquals("OPEN",rows.getString(2));}}
 @Test void correlatesPlayersAtSameTableByDevice()throws Exception{service.ingest(1,signal("seat00001",TelemetrySignal.Kind.DEVICE,88L,"shared",Map.of()));var r=service.ingest(2,signal("seat00002",TelemetrySignal.Kind.DEVICE,88L,"shared",Map.of()));assertNotNull(r.riskEventId());try(var c=ds.getConnection();var q=c.prepareStatement("SELECT reasons_json FROM risk_event");var rows=q.executeQuery()){assertTrue(rows.next());assertTrue(rows.getString(1).contains("COSEAT_DEVICE_ASSOCIATION"));}}
 // MySQL vendor/state errors below are injected at INSERT; surrounding SQL and rollback use H2.
 @Test void propagatesNonDuplicateIntegrityFailuresWithCauseAndRollback()throws Exception{
  for(int code:new int[]{1048,1452,0}){
   var fault=new java.sql.SQLException("nonduplicate integrity fixture","23000",code);
   var rollbacks=new java.util.concurrent.atomic.AtomicInteger();
   var error=assertThrows(IllegalStateException.class,()->withInsertFault(fault,rollbacks).ingest(7,signal("failure01",TelemetrySignal.Kind.DEVICE,null,null,Map.of())));
   assertSame(fault,error.getCause());assertEquals(1,rollbacks.get());assertEquals(0,persistedSignals());
  }
 }
 @Test void retainsSupportedUniqueViolationClassificationAndRollback()throws Exception{
  for(var fault:List.of(new java.sql.SQLException("MySQL duplicate fixture","23000",1062),new java.sql.SQLException("H2 unique state fixture","23505",23505))){
   var rollbacks=new java.util.concurrent.atomic.AtomicInteger();
   assertEquals("DUPLICATE",withInsertFault(fault,rollbacks).ingest(7,signal("duplicate1",TelemetrySignal.Kind.DEVICE,null,null,Map.of())).status());
   assertEquals(1,rollbacks.get());assertEquals(0,persistedSignals());
  }
 }
 @AfterEach void closeIsolatedDatabase()throws Exception{try(var c=ds.getConnection();var s=c.createStatement()){s.execute("SHUTDOWN");}}
 private long persistedSignals()throws Exception{try(var c=ds.getConnection();var q=c.prepareStatement("SELECT COUNT(*) FROM telemetry_signal");var r=q.executeQuery()){assertTrue(r.next());return r.getLong(1);}}
 private JdbcTelemetryRiskService withInsertFault(java.sql.SQLException fault,java.util.concurrent.atomic.AtomicInteger rollbacks){
  var source=(javax.sql.DataSource)java.lang.reflect.Proxy.newProxyInstance(javax.sql.DataSource.class.getClassLoader(),new Class[]{javax.sql.DataSource.class},(proxy,method,args)->{
   if(method.getName().equals("getConnection")){
    var delegate=ds.getConnection();
    return java.lang.reflect.Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(),new Class[]{java.sql.Connection.class},(connection,m,values)->{
     if(m.getName().equals("rollback"))rollbacks.incrementAndGet();
     if(m.getName().equals("prepareStatement")&&values[0] instanceof String sql&&sql.startsWith("INSERT INTO telemetry_signal")){
      var statement=(java.sql.PreparedStatement)invokeDelegate(m,delegate,values);
      return java.lang.reflect.Proxy.newProxyInstance(java.sql.PreparedStatement.class.getClassLoader(),new Class[]{java.sql.PreparedStatement.class},(prepared,call,parameters)->{
       if(call.getName().equals("executeUpdate"))throw fault;
       return invokeDelegate(call,statement,parameters);
      });
     }
     return invokeDelegate(m,delegate,values);
    });
   }
   return invokeDelegate(method,ds,args);
  });
  return new JdbcTelemetryRiskService(source,new ObjectMapper(),Clock.fixed(now,ZoneOffset.UTC));
 }
 private static Object invokeDelegate(java.lang.reflect.Method method,Object target,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}}
 private TelemetrySignal signal(String id,TelemetrySignal.Kind kind,Long room,String device,Map<String,Object>a){return new TelemetrySignal(id,kind,now,"s",room,device,"10.2.3.9",31.2304,121.4737,a);}
}
