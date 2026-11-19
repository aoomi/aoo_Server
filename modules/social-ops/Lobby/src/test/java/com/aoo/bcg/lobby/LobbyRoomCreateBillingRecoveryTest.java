package com.aoo.bcg.lobby;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.util.*;import java.lang.reflect.*;
final class LobbyRoomCreateBillingRecoveryTest {
 @Test void reserveCommittedButAdvanceFailedStillReleasesOriginal()throws Exception {
  var calls=new ArrayList<String>();
  LobbyRoomCreateSaga.Store store=new LobbyRoomCreateSaga.Store(){public LobbyRoomCreateSaga.State begin(LobbyRoomCreateSaga.Command c){throw new AssertionError();}public LobbyRoomCreateSaga.State find(String id){throw new AssertionError();}public LobbyRoomCreateSaga.State advance(String id,long v,LobbyRoomCreateSaga.Step s,Map<String,Object>r){throw new AssertionError();}public void fail(String id,long v,LobbyRoomCreateSaga.Step s,LobbyRoomCreateSaga.Step origin,String reason){calls.add(s.name());}};
  var saga=new LobbyRoomCreateSaga(store,new LobbyRoomCreateSaga.BillingPort(){public void reserve(LobbyRoomCreateSaga.Command c){}public void confirm(LobbyRoomCreateSaga.Command c){}public void release(LobbyRoomCreateSaga.Command c){calls.add("release");}},new LobbyRoomCreateSaga.RoomPort(){public void register(LobbyRoomCreateSaga.Command c){}public Map<String,Object>confirm(LobbyRoomCreateSaga.Command c,long v){return Map.of();}public void remove(LobbyRoomCreateSaga.Command c){}},c->c,new LobbyRoomCreateSaga.AuthorityPort(){public Map<String,Object>create(LobbyRoomCreateSaga.Command c){return Map.of();}public Map<String,Object>recover(LobbyRoomCreateSaga.Command c){return Map.of();}public Map<String,Object>destroy(LobbyRoomCreateSaga.Command c,Map<String,Object>x){return Map.of();}});
  Method compensate=LobbyRoomCreateSaga.class.getDeclaredMethod("compensate",LobbyRoomCreateSaga.Command.class,LobbyRoomCreateSaga.State.class,RuntimeException.class);compensate.setAccessible(true);
  compensate.invoke(saga,new LobbyRoomCreateSaga.Command(17,"source-request-0001",42,2003,"liangshan2003-v1.0.0","test",1,Map.of(),new LobbyRoomCreateSaga.Scope("PERSONAL",0,""),Map.of(),"trace","a".repeat(64)),new LobbyRoomCreateSaga.State("source-request-0001","a".repeat(64),LobbyRoomCreateSaga.Step.STARTED,1,Map.of()),new IllegalStateException("advance lost"));
  assertEquals(List.of("release","COMPENSATED"),calls);
 }
}
