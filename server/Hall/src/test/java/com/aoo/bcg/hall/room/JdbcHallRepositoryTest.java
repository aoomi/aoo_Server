package com.aoo.bcg.hall.room;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JdbcHallRepositoryTest{
 @Test void comparesVersionsNumericallyAndDeterministically(){assertTrue(JdbcHallRepository.compareVersions("2.10.0","2.9.9")>0);assertEquals(0,JdbcHallRepository.compareVersions("1.2","1.2.0"));assertTrue(JdbcHallRepository.compareVersions("1.2-rc1","1.2-beta")>0);}
 @Test void catalogClassificationDoesNotRequireTheAuthorityIndexToUseTheSameRegionTag(){
  assertEquals("JOIN aoo_compiled_index_active ai ON ai.game_id=gr.game_id",JdbcHallRepository.FILTER_ACTIVE_INDEX_JOIN);
  assertEquals("JOIN aoo_compiled_index_active a ON a.game_id=g.game_id",JdbcHallRepository.CATALOG_ACTIVE_INDEX_JOIN);
  assertFalse(JdbcHallRepository.FILTER_ACTIVE_INDEX_JOIN.contains("region_code"));
  assertFalse(JdbcHallRepository.CATALOG_ACTIVE_INDEX_JOIN.contains("region_code"));
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
 @Test void joinRetiresLeftMembershipsBeforeAllocatingAReusableSeat() throws Exception{
  String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  int cleanup=source.indexOf("DELETE FROM aoo_hall_room_member WHERE room_id=? AND status='LEFT'");
  int allocation=source.indexOf("SELECT COALESCE(MAX(seat_no),-1)+1",cleanup);
  assertTrue(cleanup>0&&allocation>cleanup,"vacated seats must be released before allocation");
 }
 @Test void lastMemberLeavingDissolvesAClubEntityRoom() throws Exception{
  String source=Files.readString(Path.of("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java"));
  assertTrue(source.contains("dissolveEmptyClubRoom(c,value,room)"));
  assertTrue(source.contains("SELECT COUNT(*) FROM aoo_hall_room_member WHERE room_id=? AND status IN('JOINING','JOINED')"));
  assertTrue(source.contains("projectClubRoom(c,value,room,\"DISSOLVED\")"));
 }
}
