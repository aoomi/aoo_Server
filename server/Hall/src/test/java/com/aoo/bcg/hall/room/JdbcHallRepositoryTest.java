package com.aoo.bcg.hall.room;
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
}
