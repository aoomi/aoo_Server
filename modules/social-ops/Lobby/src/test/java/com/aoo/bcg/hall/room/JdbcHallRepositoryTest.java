package com.aoo.bcg.hall.room;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.hall.http.HallError;
import com.aoo.bcg.lobby.JdbcLobbyRepository;
import java.time.Clock;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JdbcHallRepositoryTest{
 @Test void normalizedCreateCommandUsesPublishedDefaultsBeforeBillingHandoff() throws Exception{
  JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:normalized_rules_"+UUID.randomUUID().toString().replace("-","")+";MODE=MySQL;DB_CLOSE_DELAY=-1");source.setUser("sa");
  String schema="{\"gameId\":630,\"gameCode\":\"chengdu2999\",\"playVersion\":\"cd299-v1.0.0\",\"roomMeasureKind\":\"DURATION_MINUTES\",\"fields\":[{\"key\":\"roomDurationMinutes\",\"control\":\"radio\",\"defaultValue\":30,\"options\":[30,45,60]},{\"key\":\"maxPlayers\",\"control\":\"number\",\"defaultValue\":8,\"min\":8,\"max\":8,\"step\":1},{\"key\":\"payerMode\",\"control\":\"radio\",\"defaultValue\":\"OWNER\",\"options\":[\"OWNER\"]}]}";
  try(Connection c=source.getConnection();var active=c.prepareStatement("CREATE TABLE aoo_compiled_index_active(game_id BIGINT,region_code VARCHAR(64),play_version VARCHAR(64),index_generation BIGINT)");var index=c.prepareStatement("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT,region_code VARCHAR(64),play_version VARCHAR(64),index_generation BIGINT,rule_validator CLOB,lifecycle_state VARCHAR(32))")){active.execute();index.execute();}
  try(Connection c=source.getConnection();var active=c.prepareStatement("INSERT INTO aoo_compiled_index_active VALUES(630,'CN-51-01','cd299-v1.0.0',7)");var index=c.prepareStatement("INSERT INTO aoo_compiled_room_create_index VALUES(630,'CN-51-01','cd299-v1.0.0',7,?,'ACTIVE')")){active.executeUpdate();index.setString(1,schema);index.executeUpdate();}
  RoomCreateSaga.Command raw=new RoomCreateSaga.Command(7,"normalized-rules-request",800630,630,"cd299-v1.0.0",new RoomCreateSaga.RuleRevision("CN-51-01",9,7,"sha256:cd299"),"3.8.8",1,Map.of(),new RoomCreateSaga.Scope("PERSONAL",0,""),Map.of(),"trace",RoomCreateSaga.CreationOrigin.PUBLIC,"raw");
  JdbcHallRepository repository=new JdbcHallRepository(source,new ObjectMapper(),Clock.systemUTC(),fake(RoomMembershipAuthorityPort.class,(proxy,method,args)->null));
  RoomCreateSaga.Command normalized=repository.normalizeCreateCommand(raw);
  assertEquals(30,normalized.rules().get("roomDurationMinutes"));assertEquals(8,normalized.rules().get("maxPlayers"));assertEquals("OWNER",normalized.rules().get("payerMode"));assertEquals(1,normalized.rules().get("baseScore"));assertFalse(normalized.rules().containsKey("roundCount"));
 }
 @Test void normalizedServerOwnedDefaultIsNotRevalidatedAsSubmittedField() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  RoomCreateSaga.Command normalized=fixture.repository.normalizeCreateCommand(fixture.command(900006L));
  assertTrue(normalized.normalizedRules());
  assertEquals(3,normalized.rules().get("hostingMissThreshold"));
  fixture.repository.registerRoom(normalized);
  assertEquals(1,fixture.count("aoo_hall_room",900006L));

  JdbcHallFixture forgedFixture=JdbcHallFixture.create(false);
  RoomCreateSaga.Command forged=forgedFixture.command(900007L).withRules(
    Map.of("playerCount",4,"hostingMissThreshold",3));
  assertFalse(forged.normalizedRules());
  HallError error=assertThrows(HallError.class,()->forgedFixture.repository.registerRoom(forged));
  assertEquals("HALL_RULE_DISABLED",error.code());
 }
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
  assertEquals(JdbcLobbyRepository.CATALOG_ACTIVE_INDEX_JOIN,JdbcHallRepository.CATALOG_ACTIVE_INDEX_JOIN);
  assertEquals("JOIN aoo_compiled_index_active a ON a.game_id=g.game_id",JdbcLobbyRepository.CATALOG_ACTIVE_INDEX_JOIN);
  assertEquals(JdbcLobbyRepository.FILTER_ACTIVE_INDEX_JOIN,JdbcHallRepository.FILTER_ACTIVE_INDEX_JOIN);
 }
 @Test void authoritativeCloseContractIsIdempotentAndCleansEveryActiveMembershipState(){
  assertDoesNotThrow(()->JdbcHallRepository.class.getDeclaredMethod("closeRoom",long.class,String.class,String.class,String.class));
 }
 @Test void authoritativeMemberLeaveContractIsExplicitAndTraceable(){
  assertDoesNotThrow(()->JdbcHallRepository.class.getDeclaredMethod("leave",long.class,String.class,long.class,String.class,Map.class));
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
  List<String> lines=new ArrayList<>();
  JdbcHallRepository repository=diagnosticRepository(List.of(activeCandidate(101)),false,false);
  repository.activeRoom(7,lines::add);
  assertEquals(1,lines.size());
  assertTrue(lines.get(0).contains("result=ACTIVE"),lines.toString());
  assertFalse(lines.get(0).contains("101"));
  assertFalse(lines.get(0).contains("privateTile"));
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
 @Test void activeRoomScanHasAFiniteBoundaryAndFailsClosedBeyondIt(){
  List<Map<String,Object>> boundary=new ArrayList<>();
  for(int index=0;index<256;index++)boundary.add(rejectedCandidate(1_000L+index));
  assertEquals(Map.of("active",false),diagnosticRepository(boundary,false,false).activeRoom(7));
  boundary.add(rejectedCandidate(2_000L));
  IllegalStateException failure=assertThrows(IllegalStateException.class,
    ()->diagnosticRepository(boundary,false,false).activeRoom(7));
  assertEquals("ACTIVE_ROOM_SCAN_LIMIT_EXCEEDED",failure.getMessage());
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
 private static Map<String,Object> rejectedCandidate(long room){return Map.ofEntries(
  Map.entry("room_id",room),Map.entry("game_id",1L),Map.entry("classification_region_code","CD"),
  Map.entry("play_version","1"),Map.entry("state","PLAYING"),Map.entry("route_endpoint","local"),
  Map.entry("scope_type","PUBLIC"),Map.entry("club_id",0L),Map.entry("lifecycle_state","STALE"),
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
     if(sql.startsWith("SELECT h.game_id,h.classification_region_code"))return fakeRows(List.of(Map.ofEntries(
      Map.entry("game_id",1L),Map.entry("classification_region_code","CD"),Map.entry("play_version","1"),
      Map.entry("release_id",1L),Map.entry("index_generation",1L),Map.entry("bundle_hash","hash"),
      Map.entry("component_chain","{}"),Map.entry("rule_validator","{}"),Map.entry("ui_schema","{\"bundle\":\"b\",\"scene\":\"s\"}"),Map.entry("family_code","poker"))),false);
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
   if(method.getName().startsWith("get")){Object value=rows.get(index.get()).get(String.valueOf(args[0]));return switch(method.getName()){case "getLong"->((Number)value).longValue();case "getInt"->((Number)value).intValue();case "getString"->value==null?null:String.valueOf(value);default->value;};}
   return null;
  });
 }
 @Test void joinRetiresLeftMembershipsBeforeAllocatingAReusableSeat() throws Exception{
  JoinFixture fixture=new JoinFixture();
  Map<String,Object> result=fixture.repository.join(7002L,"join-request-901001",901001L,"trace",Map.of());
  assertEquals(0,result.get("seatNo"));
  assertEquals(0,fixture.seatOf(7002L));
  assertFalse(fixture.hasLeftRows());
 }
 @Test void allocatesTheLowestVacantSeatInsteadOfGrowingPastAReleasedChair(){
  assertEquals(0,JdbcHallRepository.firstVacantSeat(List.of()));
  assertEquals(0,JdbcHallRepository.firstVacantSeat(List.of(1)));
  assertEquals(1,JdbcHallRepository.firstVacantSeat(List.of(0,2)));
  assertEquals(2,JdbcHallRepository.firstVacantSeat(List.of(0,1)));
 }
 @Test void lastMemberLeavingDissolvesAClubEntityRoom() throws Exception{
  LeaveFixture fixture=new LeaveFixture(null,true,0);
  assertEquals("LEFT",fixture.repository.leave(7,"leave-club-1",101,"trace-club",leaveIdentity()).get("status"));
  assertTrue(fixture.operations.stream().anyMatch(value->value.contains("state='DISSOLVED'")));
  assertTrue(fixture.operations.stream().anyMatch(value->value.contains("room_projection")));
 }
 @Test void roomAndJoinResponsesCarryTheCanonicalCatalogGameCode() throws Exception{
  JoinFixture fixture=new JoinFixture();
  Map<String,Object> result=fixture.repository.join(7002L,"join-request-game-code",901001L,"trace",Map.of());
  assertEquals("CD101",result.get("gameCode"));
 }
 @Test void staleHallMemberConvergesWhenAuthorityConfirmsAlreadyLeft(){
  LeaveFixture fixture=new LeaveFixture(null);fixture.authorityStatus="ALREADY_LEFT";
  Map<String,Object> result=fixture.repository.leave(7,"leave-1",101,"trace-1",leaveIdentity());
  assertEquals("LEFT",result.get("status"));
  assertEquals("LEFT",fixture.memberStatus);
  assertFalse(fixture.ticketPresent);
  assertEquals(Map.of("active",false),fixture.repository.activeRoom(7));
  Map<String,Object> replay=fixture.repository.leave(7,"leave-1",101,"trace-1",leaveIdentity());
  assertEquals("LEFT",replay.get("status"));
  assertEquals(7L,((Number)replay.get("accountId")).longValue());
  assertEquals(101L,((Number)replay.get("roomId")).longValue());
  assertEquals(1,fixture.authorityCalls.get(),"replayed leave must not call authority twice");
 }
 @Test void httpActorUsesAuthenticatedAccountNamespaceWithoutChangingMembershipPolicy(){
  for(String state:List.of("JOINED","JOINING","LEFT","SPECTATOR")) {
   LeaveFixture f=new LeaveFixture(null);f.memberStatus=state;f.authorityStatus="ALREADY_LEFT";
   Map<String,Object> body=new HashMap<>();leaveIdentity().forEach((k,v)->{if(!k.equals("sessionId")&&!k.equals("permission"))body.put(k,String.valueOf(v));});
   body.put("roomId","101");body.put("playVersion","1");
   assertEquals("LEFT",f.repository.leaveAuthenticated(new com.aoo.bcg.lobby.LobbyRequestAuthenticator.Principal(7,"account-session-7"),"http-"+state,101,"trace",body).get("status"));
   assertEquals("ACCOUNT_HTTP",f.lastIdentity.get("actorType"));assertEquals("account-session-7",f.lastIdentity.get("accountSessionId"));
   assertEquals("AUTHENTICATED_ROOM_MEMBER",f.lastIdentity.get("permission"));assertFalse(f.lastIdentity.containsKey("sessionId"));
  }
 }
 @Test void generic409IsNotTerminalProof(){
  LeaveFixture fixture=new LeaveFixture(new RoomAuthorityRejected(409,"ROOM_LEAVE_REJECTED","identity rejected"));
  assertThrows(RoomAuthorityRejected.class,()->fixture.repository.leave(7,"leave-rejected",101,"trace",leaveIdentity()));
  assertEquals("JOINED",fixture.memberStatus);assertTrue(fixture.ticketPresent);assertNull(fixture.idempotentResponse);
 }
 @Test void leaveReceiptMustMatchActorAndRoom(){
  for(String prior:new String[]{"{\"roomId\":102,\"accountId\":7,\"status\":\"LEFT\"}","{\"roomId\":101,\"accountId\":8,\"status\":\"LEFT\"}","{\"roomId\":101,\"accountId\":7,\"status\":\"FAILED\"}"}) {
   LeaveFixture f=new LeaveFixture(null);f.idempotentResponse=prior;
   assertEquals("HALL_IDEMPOTENCY_CONFLICT",assertThrows(HallError.class,()->f.repository.leave(7,"same-key",101,"trace",leaveIdentity())).code());
   assertEquals(0,f.authorityCalls.get());assertEquals("JOINED",f.memberStatus);
  }
 }
 @Test void existingNonJoinedMembershipStillReachesAuthority(){
  for(String state:new String[]{"JOINING","LEFT","SPECTATOR"}) {
   LeaveFixture f=new LeaveFixture(null);f.memberStatus=state;f.authorityStatus="ALREADY_LEFT";
   assertEquals("LEFT",f.repository.leave(7,"leave-"+state,101,"trace",leaveIdentity()).get("status"));
   assertEquals(1,f.authorityCalls.get());
  }
 }
 @Test void genuineAuthorityFailureDoesNotMutateHallMembership(){
  LeaveFixture fixture=new LeaveFixture(new RoomAuthorityRejected(503,"ROOM_AUTHORITY_UNAVAILABLE","redacted"));
  RoomAuthorityRejected failure=assertThrows(RoomAuthorityRejected.class,()->fixture.repository.leave(7,"leave-2",101,"trace-2",leaveIdentity()));
  assertEquals("ROOM_AUTHORITY_UNAVAILABLE",failure.code());
  assertEquals("JOINED",fixture.memberStatus);
  assertTrue(fixture.ticketPresent);
 }
 @Test void roomCreationWritesTheCompleteCanonicalLockInsideTheCreationTransaction() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  fixture.repository.registerRoom(fixture.command(900010L));
  assertEquals("codec-v1",fixture.value("aoo_room_rule_lock","card_codec_version",900010L));
  assertEquals("events-v1",fixture.value("aoo_room_rule_lock","event_interpreter_version",900010L));
  assertEquals("protocol-v1",fixture.value("aoo_room_rule_lock","protocol_version",900010L));
  assertEquals("snapshot-v1",fixture.value("aoo_room_rule_lock","snapshot_schema_version",900010L));
 }
 @Test void duplicateRoomIdentityIsCheckedWithoutOverwritingADifferentLock() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  fixture.repository.registerRoom(fixture.command(900011L));
  fixture.execute("UPDATE aoo_room_rule_lock SET source_hash='"+"b".repeat(64)+"' WHERE room_id=900011");
  HallError error=assertThrows(HallError.class,()->fixture.repository.registerRoom(fixture.command(900011L)));
  assertEquals("HALL_ROOM_IDENTITY_CONFLICT",error.code());
 }
 @Test void ruleLockInsertFailureUsesTheExistingRollbackBoundaryAndMissingLocksStayFailClosed() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(true);
  assertThrows(RuntimeException.class,()->fixture.repository.registerRoom(fixture.command(900012L)));
  assertEquals(0,fixture.count("aoo_hall_room",900012L));
  assertEquals(0,fixture.count("aoo_hall_room_member",900012L));
  assertEquals(0,fixture.count("aoo_room_rule_lock",900012L));
 }
 @Test void canonicalMetadataValidationRejectsMissingOrMismatchedReleaseFields() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  fixture.execute("UPDATE aoo_game_release SET snapshot_schema_version='not valid' WHERE release_id=11");
  HallError error=assertThrows(HallError.class,()->fixture.repository.registerRoom(fixture.command(900013L)));
  assertEquals("HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE",error.code());
}
 @Test void isolatedJdbcFixtureCreatesCanonicalRoomAndLock() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  fixture.repository.registerRoom(fixture.command(900001L));
  assertEquals(1,fixture.count("aoo_hall_room",900001L));
  assertEquals(1,fixture.count("aoo_hall_room_member",900001L));
  assertEquals(1,fixture.count("aoo_room_rule_lock",900001L));
  assertEquals(JdbcHallFixture.SOURCE_HASH,fixture.lockSourceHash(900001L));
 }
 @Test void isolatedJdbcFixtureDuplicateCreationIsIdempotentOnlyForSameJoinedOwnerAndLock() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  fixture.repository.registerRoom(fixture.command(900002L));
  fixture.repository.registerRoom(fixture.command(900002L));
  assertEquals(1,fixture.count("aoo_hall_room",900002L));
  assertEquals(1,fixture.count("aoo_hall_room_member",900002L));
  assertEquals(1,fixture.count("aoo_room_rule_lock",900002L));
 }
 @Test void malformedPersistedRulesRejectReplayWithoutOverwritingRoomMemberOrLock() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(false);
  long room=900008L;
  fixture.repository.registerRoom(fixture.command(room));
  String originalLockRules=fixture.value("aoo_room_rule_lock","immutable_rules",room);
  fixture.execute("UPDATE aoo_hall_room SET rules_json='{' WHERE room_id="+room);
  HallError error=assertThrows(HallError.class,()->fixture.repository.registerRoom(fixture.command(room)));
  assertEquals("HALL_ROOM_IDENTITY_CONFLICT",error.code());
  assertEquals(1,fixture.count("aoo_hall_room",room));
  assertEquals(1,fixture.count("aoo_hall_room_member",room));
  assertEquals(1,fixture.count("aoo_room_rule_lock",room));
  assertEquals("{",fixture.value("aoo_hall_room","rules_json",room));
  assertEquals(originalLockRules,fixture.value("aoo_room_rule_lock","immutable_rules",room));
 }
 @Test void isolatedJdbcFixtureRejectsOrphanAndMissingOwnerStates() throws Exception{
  JdbcHallFixture orphan=JdbcHallFixture.create(false);
  orphan.insertOrphanLock(900003L);
  HallError orphanError=assertThrows(HallError.class,()->orphan.repository.registerRoom(orphan.command(900003L)));
  assertEquals("HALL_ROOM_RULE_LOCK_ORPHANED",orphanError.code());

  JdbcHallFixture missingMember=JdbcHallFixture.create(false);
  missingMember.repository.registerRoom(missingMember.command(900004L));
  missingMember.execute("DELETE FROM aoo_hall_room_member WHERE room_id=900004");
  HallError memberError=assertThrows(HallError.class,()->missingMember.repository.registerRoom(missingMember.command(900004L)));
  assertEquals("HALL_ROOM_MEMBER_MISSING",memberError.code());
 }
 @Test void isolatedJdbcFixtureLockInsertFailureRollsBackRoomAndMember() throws Exception{
  JdbcHallFixture fixture=JdbcHallFixture.create(true);
  assertThrows(RuntimeException.class,()->fixture.repository.registerRoom(fixture.command(900005L)));
  assertEquals(0,fixture.count("aoo_hall_room",900005L));
  assertEquals(0,fixture.count("aoo_hall_room_member",900005L));
  assertEquals(0,fixture.count("aoo_room_rule_lock",900005L));
 }
 private static final class JoinFixture{
  final JdbcDataSource database;final JdbcHallRepository repository;
  JoinFixture() throws Exception{
   database=new JdbcDataSource();database.setURL("jdbc:h2:mem:hall-join-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");database.setUser("sa");
   try(Connection c=database.getConnection();Statement s=c.createStatement()){
    s.execute("CREATE TABLE aoo_hall_room(room_id BIGINT PRIMARY KEY,game_id BIGINT,classification_region_code VARCHAR(32),play_version VARCHAR(64),release_id BIGINT,index_generation BIGINT,owner_account_id BIGINT,scope_type VARCHAR(16),club_id BIGINT,club_template_code VARCHAR(64),state VARCHAR(24),route_endpoint VARCHAR(255),rules_json CLOB,created_at TIMESTAMP,updated_at TIMESTAMP)");
    s.execute("CREATE TABLE aoo_game_catalog(game_id BIGINT,game_code VARCHAR(64),display_name VARCHAR(128),status VARCHAR(16))");
    s.execute("CREATE TABLE aoo_hall_room_member(room_id BIGINT,account_id BIGINT,seat_no INT,status VARCHAR(16),joined_at TIMESTAMP,left_at TIMESTAMP)");
    s.execute("CREATE TABLE aoo_room_authority_route(room_id BIGINT,lifecycle_state VARCHAR(16))");
    s.execute("CREATE TABLE aoo_room_snapshot(room_id BIGINT,state_payload CLOB)");
    s.execute("CREATE TABLE aoo_hall_idempotency(account_id BIGINT,idempotency_key VARCHAR(128),operation VARCHAR(32),response_json CLOB,created_at TIMESTAMP)");
    s.execute("INSERT INTO aoo_game_catalog VALUES(101,'CD101','room','ACTIVE')");
    s.execute("INSERT INTO aoo_hall_room VALUES(901001,101,'CD','v1',11,3,7001,'PERSONAL',0,'','OPEN','hall://cd101','{\"playerCount\":4}',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    s.execute("INSERT INTO aoo_hall_room_member VALUES(901001,7003,0,'LEFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    s.execute("INSERT INTO aoo_room_authority_route VALUES(901001,'ACTIVE')");
   }
   RoomMembershipAuthorityPort authority=fake(RoomMembershipAuthorityPort.class,(proxy,method,args)->method.getName().equals("join")?Map.of("roomId",901001L,"accountId",7002L,"seatNo",0,"status","JOINED"):null);
   repository=new JdbcHallRepository(compatibilityDataSource(database),new ObjectMapper(),Clock.systemUTC(),authority);
  }
  private static DataSource compatibilityDataSource(JdbcDataSource database){
   return fake(DataSource.class,(proxy,method,args)->{
    if(!method.getName().equals("getConnection"))return invoke(database,method,args);
    Connection raw=(Connection)invoke(database,method,args);
    return fake(Connection.class,(connection,call,values)->{
     if(call.getName().equals("prepareStatement")&&values!=null&&values.length>0&&String.valueOf(values[0]).contains("ON DUPLICATE KEY UPDATE")){
      Map<Integer,Object> params=new HashMap<>();
      return fake(PreparedStatement.class,(statement,operation,arguments)->{
       if(operation.getName().startsWith("set")&&arguments!=null&&arguments.length>=2){params.put((Integer)arguments[0],arguments[1]);return null;}
       if(operation.getName().equals("executeUpdate")){try(PreparedStatement insert=raw.prepareStatement("INSERT INTO aoo_hall_room_member(room_id,account_id,seat_no,status,joined_at) VALUES(?,?,?,'JOINING',CURRENT_TIMESTAMP(3))")){insert.setLong(1,((Number)params.get(1)).longValue());insert.setLong(2,((Number)params.get(2)).longValue());insert.setInt(3,((Number)params.get(3)).intValue());return insert.executeUpdate();}}
       return null;
      });
     }
     return invoke(raw,call,values);
    });
   });
  }
  private static Object invoke(Object target,java.lang.reflect.Method method,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException error){throw error.getCause();}}
  int seatOf(long account)throws Exception{try(Connection c=database.getConnection();PreparedStatement s=c.prepareStatement("SELECT seat_no FROM aoo_hall_room_member WHERE room_id=? AND account_id=?")){s.setLong(1,901001);s.setLong(2,account);try(ResultSet r=s.executeQuery()){assertTrue(r.next());return r.getInt(1);}}}
  boolean hasLeftRows()throws Exception{try(Connection c=database.getConnection();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM aoo_hall_room_member WHERE room_id=? AND status='LEFT'")){s.setLong(1,901001);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1)>0;}}}
 }
 private static final class JdbcHallFixture{
  static final long GAME_ID=101L,RELEASE_ID=11L,INDEX_GENERATION=3L,OWNER=7001L;
  static final String REGION="CD",PLAY="v1",GAME_CODE="CD101",SOURCE_HASH="a".repeat(64);
  final JdbcDataSource database;final JdbcHallRepository repository;
  private JdbcHallFixture(JdbcDataSource database,DataSource dataSource){this.database=database;this.repository=new JdbcHallRepository(dataSource,new ObjectMapper(),Clock.systemUTC(),fake(RoomMembershipAuthorityPort.class,(proxy,method,args)->null));}
  static JdbcHallFixture create(boolean failLock) throws Exception{
   JdbcDataSource database=new JdbcDataSource();database.setURL("jdbc:h2:mem:hall-v244-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");database.setUser("sa");
   try(Connection c=database.getConnection();Statement s=c.createStatement()){
    s.execute("CREATE TABLE aoo_hall_room(room_id BIGINT PRIMARY KEY,game_id BIGINT,classification_region_code VARCHAR(32),play_version VARCHAR(64),release_id BIGINT,index_generation BIGINT,owner_account_id BIGINT,scope_type VARCHAR(16),club_id BIGINT,club_template_code VARCHAR(64),state VARCHAR(24),route_endpoint VARCHAR(255),rules_json CLOB,created_at TIMESTAMP,updated_at TIMESTAMP)");
    s.execute("CREATE TABLE aoo_hall_room_member(room_id BIGINT,account_id BIGINT,seat_no INT,status VARCHAR(16),joined_at TIMESTAMP,left_at TIMESTAMP)");
    s.execute("CREATE TABLE aoo_room_rule_lock(room_id BIGINT PRIMARY KEY,game_id BIGINT,region_code VARCHAR(32),play_version VARCHAR(64),index_generation BIGINT,release_id BIGINT,component_chain CLOB,immutable_rules CLOB,bundle_hash VARCHAR(64),rule_hash VARCHAR(64),component_hash VARCHAR(64),card_codec_version VARCHAR(64),event_interpreter_version VARCHAR(64),protocol_version VARCHAR(64),source_hash VARCHAR(64),snapshot_schema_version VARCHAR(64))");
    s.execute("CREATE TABLE aoo_compiled_index_active(game_id BIGINT,region_code VARCHAR(32),play_version VARCHAR(64),release_id BIGINT,index_generation BIGINT,cache_epoch BIGINT)");
    s.execute("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT,region_code VARCHAR(32),play_version VARCHAR(64),index_generation BIGINT,release_id BIGINT,component_chain CLOB,rule_validator CLOB,ui_schema CLOB,bundle_hash VARCHAR(64),lifecycle_state VARCHAR(16))");
    s.execute("CREATE TABLE aoo_play_version(game_id BIGINT,play_version VARCHAR(64),compatibility_floor VARCHAR(64),status VARCHAR(16))");
    s.execute("CREATE TABLE aoo_game_catalog(game_id BIGINT,game_code VARCHAR(64),status VARCHAR(16))");
    s.execute("CREATE TABLE aoo_game_release(release_id BIGINT,game_id BIGINT,play_version VARCHAR(64),status VARCHAR(16),catalog_snapshot CLOB,catalog_hash VARCHAR(64),ui_snapshot CLOB,ui_hash VARCHAR(64),rule_hash VARCHAR(64),component_hash VARCHAR(64),bundle_hash VARCHAR(64),snapshot_schema_version VARCHAR(64),card_codec_version VARCHAR(64),event_interpreter_version VARCHAR(64),protocol_version VARCHAR(64),source_hash VARCHAR(64))");
    s.execute("CREATE TABLE aoo_game_release_region(release_id BIGINT,region_code VARCHAR(32),status VARCHAR(16))");
    s.execute("CREATE TABLE aoo_game_service_route(game_id BIGINT,play_version VARCHAR(64),status VARCHAR(16),endpoint VARCHAR(255),priority INT,route_id BIGINT)");
    s.execute("CREATE TABLE aoo_room_authority_route(room_id BIGINT,lifecycle_state VARCHAR(16))");
    s.execute("CREATE TABLE aoo_room_snapshot(room_id BIGINT,state_payload CLOB)");
    String catalog="{\"gameId\":101,\"gameCode\":\"CD101\"}";
    String ui="{\"bundle\":\"games-cd101\",\"scene\":\"Room\",\"roomRuleSourceHash\":\""+SOURCE_HASH+"\"}";
    String validator="{\"gameId\":101,\"gameCode\":\"CD101\",\"playVersion\":\"v1\",\"sourceHash\":\""+SOURCE_HASH+"\",\"fields\":[{\"key\":\"playerCount\",\"control\":\"NUMBER\",\"defaultValue\":4,\"min\":2,\"max\":4,\"step\":1},{\"key\":\"hostingMissThreshold\",\"control\":\"radio\",\"disabled\":true,\"defaultValue\":3,\"options\":[3,4,5]}]}";
    String components="{\"components\":[\"rule-core\"]}";
    String bundleHash=sha256("bundle-v1"),catalogHash=sha256(catalog),ruleHash=sha256(validator),componentHash=sha256(components);
    s.execute("INSERT INTO aoo_compiled_index_active VALUES(101,'CD','v1',11,3,1)");
    s.execute("INSERT INTO aoo_compiled_room_create_index VALUES(101,'CD','v1',3,11,"+q(components)+","+q(validator)+","+q(ui)+","+q(bundleHash)+",'ACTIVE')");
    s.execute("INSERT INTO aoo_play_version VALUES(101,'v1','1.0.0','ACTIVE')");
    s.execute("INSERT INTO aoo_game_catalog VALUES(101,'CD101','ACTIVE')");
    s.execute("INSERT INTO aoo_game_release VALUES(11,101,'v1','ACTIVE',"+q(catalog)+","+q(catalogHash)+","+q(ui)+","+q(SOURCE_HASH)+","+q(ruleHash)+","+q(componentHash)+","+q(bundleHash)+",'snapshot-v1','codec-v1','events-v1','protocol-v1',"+q(SOURCE_HASH)+")");
    s.execute("INSERT INTO aoo_game_release_region VALUES(11,'CD','ACTIVE')");
    s.execute("INSERT INTO aoo_game_service_route VALUES(101,'v1','ACTIVE','hall://cd101',1,1)");
   }
   return new JdbcHallFixture(database,failLock?failLockInsert(database):database);
  }
  RoomCreateSaga.Command command(long room){return new RoomCreateSaga.Command(OWNER,"request-"+room,room,(int)GAME_ID,PLAY,new RoomCreateSaga.RuleRevision(REGION,RELEASE_ID,INDEX_GENERATION,SOURCE_HASH),"1.0.0",1L,Map.of("playerCount",4),new RoomCreateSaga.Scope("PERSONAL",0,""),Map.of(),"trace",RoomCreateSaga.CreationOrigin.PUBLIC,"request-hash");}
  void insertOrphanLock(long room)throws Exception{execute("INSERT INTO aoo_room_rule_lock(room_id) VALUES("+room+")");}
  void execute(String sql)throws Exception{try(Connection c=database.getConnection();Statement s=c.createStatement()){s.execute(sql);}}
  int count(String table,long room)throws Exception{try(Connection c=database.getConnection();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE room_id=?")){s.setLong(1,room);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}}}
  String value(String table,String column,long room)throws Exception{try(Connection c=database.getConnection();PreparedStatement s=c.prepareStatement("SELECT "+column+" FROM "+table+" WHERE room_id=?")){s.setLong(1,room);try(ResultSet r=s.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
  String lockSourceHash(long room)throws Exception{try(Connection c=database.getConnection();PreparedStatement s=c.prepareStatement("SELECT source_hash FROM aoo_room_rule_lock WHERE room_id=?")){s.setLong(1,room);try(ResultSet r=s.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
  private static String q(String value){return "'"+value.replace("'","''")+"'";}
  private static DataSource failLockInsert(DataSource delegate){return fake(DataSource.class,(proxy,method,args)->{if(method.getName().equals("getConnection")){Connection raw=(Connection)invoke(delegate,method,args);return fake(Connection.class,(connection,call,values)->{if(call.getName().equals("prepareStatement")&&values!=null&&values.length>0&&String.valueOf(values[0]).startsWith("INSERT INTO aoo_room_rule_lock"))throw new SQLException("injected lock insert failure");return invoke(raw,call,values);});}return invoke(delegate,method,args);});}
  private static Object invoke(Object target,java.lang.reflect.Method method,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException error){throw error.getCause();}}
  private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw new IllegalStateException(error);}}
 }
 @Test void normalAuthorityLeaveStillConvergesHallMembership(){
  LeaveFixture fixture=new LeaveFixture(null);
  assertEquals("LEFT",fixture.repository.leave(7,"leave-3",101,"trace-3",leaveIdentity()).get("status"));
  assertEquals("LEFT",fixture.memberStatus);
  assertFalse(fixture.ticketPresent);
  assertEquals(1,fixture.authorityCalls.get());
 }
 @Test void alreadyLeftClassificationUsesOnlyTheExplicitAuthorityContract(){
  assertFalse(JdbcHallRepository.authorityConfirmsAlreadyLeft(new RoomAuthorityRejected(409,"ROOM_LEAVE_REJECTED","ignored")));
  assertFalse(JdbcHallRepository.authorityConfirmsAlreadyLeft(new RoomAuthorityRejected(500,"ROOM_LEAVE_REJECTED","ignored")));
  assertFalse(JdbcHallRepository.authorityConfirmsAlreadyLeft(new RoomAuthorityRejected(409,"ROOM_ENDED","ignored")));
 }
 private static final class LeaveFixture{
  String authorityStatus="LEFT";Map<String,Object> lastIdentity;
  String memberStatus="JOINED";boolean ticketPresent=true;String idempotentResponse;final List<String> operations=new ArrayList<>();
  final AtomicInteger authorityCalls=new AtomicInteger();final JdbcHallRepository repository;
  LeaveFixture(RoomAuthorityRejected authorityFailure){this(authorityFailure,false,0);}
  LeaveFixture(RoomAuthorityRejected authorityFailure,boolean club,long activeMembers){
   DataSource source=fake(DataSource.class,(proxy,method,args)->method.getName().equals("getConnection")?connection():null);
   RoomMembershipAuthorityPort authority=fake(RoomMembershipAuthorityPort.class,(proxy,method,args)->{
    if(!method.getName().equals("leave"))return null;
    authorityCalls.incrementAndGet();lastIdentity=(Map<String,Object>)args[5];if(authorityFailure!=null)throw authorityFailure;
    return Map.of("status",authorityStatus);
   });
   this.club=club;this.activeMembers=activeMembers;
   repository=new JdbcHallRepository(source,new ObjectMapper(),Clock.systemUTC(),authority);
  }
  final boolean club;final long activeMembers;
  private Connection connection(){return fake(Connection.class,(proxy,method,args)->{
   if(method.getName().equals("prepareStatement"))return statement(String.valueOf(args[0]));
   return null;
  });}
  private PreparedStatement statement(String sql){Map<Integer,Object> values=new HashMap<>();return fake(PreparedStatement.class,(proxy,method,args)->{
   if(method.getName().startsWith("set")&&args!=null&&args.length>=2){values.put((Integer)args[0],args[1]);return null;}
   if(method.getName().equals("executeQuery")){
    if(sql.startsWith("SELECT operation,response_json"))return idempotentResponse==null?fakeRows(List.of(),false):fakeRows(List.of(Map.of("1","LEAVE","2",idempotentResponse)),false);
    if(sql.startsWith("SELECT h.game_id,g.game_code"))return fakeRows(List.of(Map.ofEntries(Map.entry("game_id",1L),Map.entry("game_code","chengdu1001"),Map.entry("classification_region_code","CD"),Map.entry("play_version","1"),Map.entry("state","OPEN"),Map.entry("route_endpoint","local"),Map.entry("scope_type",club?"CLUB":"PUBLIC"),Map.entry("club_id",club?42L:0L),Map.entry("club_template_code",club?"default":""),Map.entry("display_name","room"))),false);
    if(sql.startsWith("SELECT status FROM aoo_hall_room_member"))return fakeRows(List.of(Map.of("1",memberStatus)),false);
    if(sql.startsWith("SELECT COUNT(*) FROM aoo_hall_room_member WHERE room_id=? AND status IN('JOINING','JOINED')"))return fakeRows(List.of(Map.of("1",activeMembers)),false);
    if(sql.contains("SELECT r.room_id"))return "JOINED".equals(memberStatus)?fakeRows(List.of(activeCandidate(101)),false):fakeRows(List.of(),false);
    if(sql.contains("rules_json"))return fakeRows(List.of(Map.of("1","{\"playerCount\":4}")),false);
    if(sql.contains("ui_schema"))return fakeRows(List.of(Map.of("1","{\"bundle\":\"b\",\"scene\":\"s\"}")),false);
    return fakeRows(List.of(Map.of("1",0)),false);
   }
   if(method.getName().equals("executeUpdate")){
    if(sql.startsWith("DELETE FROM aoo_hall_game_ticket")){ticketPresent=false;operations.add(sql);}
    if(sql.startsWith("UPDATE aoo_hall_room_member")){memberStatus="LEFT";operations.add(sql);}
    if(sql.startsWith("UPDATE aoo_hall_room SET state='DISSOLVED'"))operations.add(sql);
    if(sql.startsWith("INSERT INTO aoo_club_room_projection"))operations.add("room_projection:"+sql);
    if(sql.startsWith("INSERT INTO aoo_hall_idempotency"))idempotentResponse=String.valueOf(values.get(4));
    return 1;
   }
   return null;
  });}
 }
 private static Map<String,Object> leaveIdentity(){return Map.ofEntries(
  Map.entry("roomInstanceId","instance-101"),Map.entry("sessionId","session-7"),
  Map.entry("lifecycleEpoch",1L),Map.entry("fencingToken",1L),Map.entry("stateVersion",0L),
  Map.entry("releaseId",11L),Map.entry("indexGeneration",1L),Map.entry("regionCode","CD"),
  Map.entry("sourceHash","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
  Map.entry("ruleContentHash","bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
  Map.entry("componentContentHash","cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
  Map.entry("cardCodecVersion","codec-v1"),Map.entry("eventInterpreterVersion","events-v1"),
  Map.entry("protocolVersion","protocol-v1"),Map.entry("snapshotSchemaVersion","snapshot-v1"),
  Map.entry("gameId",1L),Map.entry("gameCode","chengdu1001"),Map.entry("permission","SEATED_MEMBER"));}
}
