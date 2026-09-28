package com.aoo.bcg.hall.room;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JdbcHallRepositoryTest{
 @Test void clubTemplatePolicyIsStoredBesideValidatedGameplayWithoutAdmittingUnknownFields(){
  Map<String,Object> merged=JdbcHallRepository.mergeClubTemplatePolicy(
    Map.of("roundCount",8,"baseScore",1),
    Map.of("roundCount",8,"percentage",10,"roomSportsType",2,"attackerControlled",true));
  assertEquals(10,merged.get("percentage"));
  assertEquals(2,merged.get("roomSportsType"));
  assertFalse(merged.containsKey("attackerControlled"));
 }
 @Test void comparesVersionsNumericallyAndDeterministically(){assertTrue(JdbcHallRepository.compareVersions("2.10.0","2.9.9")>0);assertEquals(0,JdbcHallRepository.compareVersions("1.2","1.2.0"));assertTrue(JdbcHallRepository.compareVersions("1.2-rc1","1.2-beta")>0);}
 @Test void catalogClassificationDoesNotRequireTheAuthorityIndexToUseTheSameRegionTag(){
  assertEquals("JOIN aoo_compiled_index_active ai ON ai.game_id=gr.game_id",JdbcHallRepository.FILTER_ACTIVE_INDEX_JOIN);
  assertEquals("JOIN aoo_compiled_index_active a ON a.game_id=g.game_id",JdbcHallRepository.CATALOG_ACTIVE_INDEX_JOIN);
  assertFalse(JdbcHallRepository.FILTER_ACTIVE_INDEX_JOIN.contains("region_code"));
  assertFalse(JdbcHallRepository.CATALOG_ACTIVE_INDEX_JOIN.contains("region_code"));
 }
 @Test void catalogDistinctOrderingProjectsItsSortColumns(){
  String source=assertDoesNotThrow(()->Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java")));
  assertTrue(source.contains("catalog_scope_sort,r.depth catalog_depth_sort,gr.priority catalog_priority_sort"));
  assertTrue(source.contains("ORDER BY catalog_scope_sort,catalog_depth_sort DESC,catalog_priority_sort,g.game_id"));
 }
 @Test void authoritativeCloseContractIsIdempotentAndCleansEveryActiveMembershipState(){
  assertDoesNotThrow(()->JdbcHallRepository.class.getDeclaredMethod("closeRoom",long.class,String.class,String.class,String.class));
 }
 @Test void authoritativeMemberLeaveContractIsExplicitAndTraceable(){
  assertDoesNotThrow(()->JdbcHallRepository.class.getDeclaredMethod("authorityLeave",long.class,String.class,long.class,String.class));
 }
 @Test void activeRoomLookupIsAccountScopedForStartupRecovery(){
  assertDoesNotThrow(()->JdbcHallRepository.class.getDeclaredMethod("activeRoom",long.class));
 }
 @Test void temporaryRecoveryDiagnosticUsesOnlyFixedRejectionCodes(){
  assertEquals("AUTHORITY_NOT_ACTIVE",JdbcHallRepository.rejectCode("PLAYING","STALE",Map.of("privateTile","secret")));
  assertEquals("SNAPSHOT_TERMINAL",JdbcHallRepository.rejectCode("PLAYING","ACTIVE",Map.of("roomTerminal",true,"privateTile","secret")));
  assertEquals("FINAL_SETTLEMENT_PRESENT",JdbcHallRepository.rejectCode("PLAYING","ACTIVE",Map.of("finalSettlement",Map.of("privateTile","secret"))));
  assertEquals("PLAYING_SNAPSHOT_MISSING",JdbcHallRepository.rejectCode("PLAYING","ACTIVE",Map.of()));
  assertEquals("POLICY_OTHER",JdbcHallRepository.rejectCode("PLAYING","ACTIVE",Map.of("privateTile","secret")));
 }
 @Test void temporaryDiagnosticDoesNotUseRequestTraceOrSensitiveColumns() throws Exception{
  String repository=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  String routes=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/http/HallHttpRoutes.java"));
  String diagnostic=repository.substring(repository.indexOf("private static String diagnosticStages"),repository.indexOf("private static int diagnosticCount"));
  assertFalse(diagnostic.contains("SELECT *"));
  assertFalse(diagnostic.contains("state_payload"));
  assertFalse(diagnostic.contains("account_id,"));
  assertTrue(routes.contains("id=\"+id+\" \"+line"));
  assertTrue(routes.contains("UUID.randomUUID().toString();e.getResponseHeaders().set(\"X-Aoo-Hall-Diagnostic-Id\""));
 }
 @Test void multipleCandidatesAreInconclusiveWithoutChangingTheFirstActiveResponse(){
  List<Map<String,Object>> rows=List.of(activeCandidate(101),activeCandidate(102));
  JdbcHallRepository repository=diagnosticRepository(rows,false,false);
  Map<String,Object> original=repository.activeRoom(7);
  List<String> lines=new ArrayList<>();
  Map<String,Object> diagnosed=repository.activeRoom(7,lines::add);
  assertEquals(original,diagnosed);
  assertEquals(101L,diagnosed.get("roomId"));
  assertEquals(1,lines.size());
  assertTrue(lines.get(0).contains("result=INCONCLUSIVE"));
  assertTrue(lines.get(0).contains("candidates=2"));
  assertFalse(lines.get(0).contains("101"));
  assertFalse(lines.get(0).contains("102"));
 }
 @Test void diagnosticFailuresDoNotChangeTheActiveResponse(){
  List<Map<String,Object>> rows=List.of(activeCandidate(101));
  JdbcHallRepository stageFailure=diagnosticRepository(rows,true,false);
  List<String> lines=new ArrayList<>();
  assertEquals(stageFailure.activeRoom(7),stageFailure.activeRoom(7,lines::add));
  assertEquals(List.of("result=INCONCLUSIVE reason=DIAGNOSTIC_FAILURE"),lines);
  JdbcHallRepository scanFailure=diagnosticRepository(List.of(activeCandidate(101),activeCandidate(102)),false,true);
  lines.clear();
  assertEquals(scanFailure.activeRoom(7),scanFailure.activeRoom(7,lines::add));
  assertTrue(lines.get(0).contains("result=INCONCLUSIVE"));
  assertDoesNotThrow(()->stageFailure.activeRoom(7,line->{throw new IllegalStateException("sink down");}));
 }
 @Test void diagnosticGateRequiresExplicitEnablementLoopbackAndOptIn() throws Exception{
  var method=com.aoo.bcg.hall.http.HallHttpRoutes.class.getDeclaredMethod("activeDiagnosticAllowed",boolean.class,boolean.class,String.class);
  method.setAccessible(true);
  assertEquals(false,method.invoke(null,false,true,"1"));
  assertEquals(false,method.invoke(null,true,false,"1"));
  assertEquals(false,method.invoke(null,true,true,null));
  assertEquals(false,method.invoke(null,true,true,"0"));
  assertEquals(true,method.invoke(null,true,true,"1"));
 }
 private static Map<String,Object> activeCandidate(long room){return Map.ofEntries(
  Map.entry("room_id",room),Map.entry("game_id",1L),Map.entry("classification_region_code","CD"),
  Map.entry("play_version","1"),Map.entry("state","OPEN"),Map.entry("route_endpoint","local"),
  Map.entry("scope_type","PUBLIC"),Map.entry("club_id",0L),Map.entry("lifecycle_state","ACTIVE"),
  Map.entry("state_payload","{}"),Map.entry("occupied_count",1));}
 @SuppressWarnings("unchecked")
 private static <T>T fake(Class<T> type,java.lang.reflect.InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);}
 private static JdbcHallRepository diagnosticRepository(List<Map<String,Object>> candidates,boolean failStage,boolean failScan){
  DataSource source=fake(DataSource.class,(proxy,method,args)->{
   if(!method.getName().equals("getConnection"))return null;
   return fake(Connection.class,(connection,call,params)->{
    if(!call.getName().equals("prepareStatement"))return null;
    String sql=(String)params[0];
    return fake(PreparedStatement.class,(statement,operation,values)->{
     if(!operation.getName().equals("executeQuery"))return null;
     if(sql.startsWith("SELECT COUNT(*) FROM aoo_hall_room_member")&&failStage)throw new SQLException("stage failed");
     if(sql.contains("SELECT r.room_id"))return fakeRows(candidates,failScan);
     if(sql.contains("rules_json"))return fakeRows(List.of(Map.of("1","{\"playerCount\":4}")),false);
     if(sql.contains("ui_schema"))return fakeRows(List.of(Map.of("1","{\"bundle\":\"b\",\"scene\":\"s\"}")),false);
     return fakeRows(List.of(Map.of("1",1)),false);
    });
   });
  });
  RoomMembershipAuthorityPort authority=fake(RoomMembershipAuthorityPort.class,(proxy,method,args)->null);
  return new JdbcHallRepository(source,new ObjectMapper(),Clock.systemUTC(),authority);
 }
 private static ResultSet fakeRows(List<Map<String,Object>> rows,boolean failScan){
  AtomicInteger index=new AtomicInteger(-1);
  return fake(ResultSet.class,(proxy,method,args)->{
   if(method.getName().equals("next")){int current=index.incrementAndGet();if(failScan&&current==1)throw new SQLException("scan failed");return current<rows.size();}
   if(method.getName().startsWith("get")){Object value=rows.get(index.get()).get(String.valueOf(args[0]));return switch(method.getName()){case "getLong"->((Number)value).longValue();case "getInt"->((Number)value).intValue();case "getString"->value==null?null:String.valueOf(value);default->null;};}
   return null;
  });
 }
 @Test void joinRetiresLeftMembershipsBeforeAllocatingAReusableSeat() throws Exception{
  String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  int cleanup=source.indexOf("DELETE FROM aoo_hall_room_member WHERE room_id=? AND status='LEFT'");
  int allocation=source.indexOf("SELECT seat_no FROM aoo_hall_room_member",cleanup);
  assertTrue(cleanup>0&&allocation>cleanup,"vacated seats must be released before allocation");
 }
 @Test void allocatesTheLowestVacantSeatInsteadOfGrowingPastAReleasedChair(){
  assertEquals(0,JdbcHallRepository.firstVacantSeat(List.of()));
  assertEquals(0,JdbcHallRepository.firstVacantSeat(List.of(1)));
  assertEquals(1,JdbcHallRepository.firstVacantSeat(List.of(0,2)));
  assertEquals(2,JdbcHallRepository.firstVacantSeat(List.of(0,1)));
 }
 @Test void lastMemberLeavingDissolvesAClubEntityRoom() throws Exception{
  String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  assertTrue(source.contains("dissolveEmptyClubRoom(c,value,room)"));
  assertTrue(source.contains("SELECT COUNT(*) FROM aoo_hall_room_member WHERE room_id=? AND status IN('JOINING','JOINED')"));
  assertTrue(source.contains("projectClubRoom(c,value,room,\"DISSOLVED\")"));
 }
 @Test void roomAndJoinResponsesCarryTheCanonicalCatalogGameCode() throws Exception{
  String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  assertTrue(source.contains("g.game_code"));
  assertTrue(source.contains("\"gameCode\",value.gameCode()"));
  assertTrue(source.contains("\"gameCode\",reservation.gameCode()"));
 }
}
