package com.aoo.bcg.poker.cx;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CxProviderFactoryTest {
 @Test void businessCodeAndPlayVersionHaveDistinctCanonicalCase(){assertEquals("chengdu2999",CxRules.GAME_CODE);assertEquals("cx-v1.0.0",CxRules.VERSION);assertTrue(CxRules.VERSION.matches("^[a-z][a-z0-9._-]*$"));assertFalse(CxRules.GAME_CODE.matches("^[a-z][a-z0-9._-]*$"));}
 @Test void providerPublishesStrictNonEmptyCommandBoundary(){var provider=new CxGameProvider(descriptor(74021,"chengdu2999",CxRules.FAMILY));var rules=provider.ruleComponents();assertEquals(1,rules.size());var rule=rules.getFirst();assertEquals("cx.command-boundary",rule.ruleId());assertEquals(CxRules.VERSION,rule.componentVersion());var commands=Set.of("poker.chengdu2999.state_req","poker.chengdu2999.sit_req","poker.chengdu2999.ready_req","poker.chengdu2999.preset_req","poker.chengdu2999.bet_req","poker.chengdu2999.add_card_req","poker.chengdu2999.split_req","poker.chengdu2999.delay_split_req","poker.chengdu2999.continue_req","poker.chengdu2999.restart_req","poker.chengdu2999.rebuy_req","poker.chengdu2999.stand_req","poker.chengdu2999.timeout_req");for(String command:commands)assertTrue(rule.execute(command(command,CxRules.VERSION)).accepted(),command);assertTrue(rule.execute(new GameCommandRequest("poker.chengdu2999.dispatch","dispatch-1",1,9001,0,CxRules.VERSION,"101",-1,Map.of("action","poker.chengdu2999.state_req","payload",Map.of()))).accepted());var badCommand=rule.execute(command("poker.chengdu2999.join_req",CxRules.VERSION));assertEquals("CX_COMMAND_NOT_ALLOWED",badCommand.code());assertTrue(badCommand.message().startsWith("[CX]"));var badVersion=rule.execute(command("poker.chengdu2999.state_req","CX-v1.0.0"));assertEquals("CX_PLAY_VERSION_MISMATCH",badVersion.code());assertTrue(badVersion.message().startsWith("[CX]"));}
 @Test void outerDispatchIsStrictlyUnwrappedBeforeAuthoritativeExecution(){var provider=new CxGameProvider(descriptor(74021,"chengdu2999",CxRules.FAMILY));var room=provider.roomFactory().create(new RoomCreationContext(9001,101,Map.of()));var response=provider.commandHandler().orElseThrow().handle(room,new GameCommandRequest("poker.chengdu2999.dispatch","dispatch-state",1,9001,0,CxRules.VERSION,"101",-1,Map.of("action","poker.chengdu2999.state_req","payload",Map.of())));assertEquals("poker.chengdu2999.state_rsp",response.msgId());assertEquals("chengdu2999",response.body().asMap().get("gameCode"));assertThrows(IllegalArgumentException.class,()->provider.commandHandler().orElseThrow().handle(room,new GameCommandRequest("poker.chengdu2999.dispatch","dispatch-bad",2,9001,0,CxRules.VERSION,"101",-1,Map.of("action","poker.chengdu2999.unknown_req","payload",Map.of()))));}
 @Test void roomFactoryCarriesExplicitSnapshotMetadataSource(){var provider=new CxGameProvider(descriptor(74021,"chengdu2999",CxRules.FAMILY));var source=new RoomSnapshotMetadataSource(7L,"a".repeat(64),"b".repeat(64),"card-v1","events-v1","protocol-v1","snapshot-v1",11L,"CN-SC","c".repeat(64));var room=provider.roomFactory().create(new RoomCreationContext(9002,101,Map.of(),null,source));assertSame(source,room.requireRoomSnapshotMetadataSource());}
 @Test void ruleRejectsProviderDescriptorIdentityInsteadOfAcceptingPlaceholder(){var wrongCode=new CxGameProvider(descriptor(74021,"chengdu2004",CxRules.FAMILY)).ruleComponents().getFirst().execute(command("poker.chengdu2999.state_req",CxRules.VERSION));assertEquals("CX_IDENTITY_MISMATCH",wrongCode.code());var wrongFamily=new CxGameProvider(descriptor(74021,"chengdu2999","poker:pdk")).ruleComponents().getFirst().execute(command("poker.chengdu2999.state_req",CxRules.VERSION));assertEquals("CX_IDENTITY_MISMATCH",wrongFamily.code());var wrongProviderVersion=new CxGameProvider(new GameDescriptor(74021,"chengdu2999","成都扯旋",GameCategory.POKER,CxRules.FAMILY,RegionScope.CITY,"SC","CD","other-v1")).ruleComponents().getFirst().execute(command("poker.chengdu2999.state_req",CxRules.VERSION));assertEquals("CX_PLAY_VERSION_MISMATCH",wrongProviderVersion.code());}
 @Test void restoreAndInternalReplayPreserveAuthorityForContinuedRealPlay(){
  var provider=new CxGameProvider(descriptor(74021,"chengdu2999",CxRules.FAMILY));
  var original=new CxSession(9001,101,77,CxRules.from(Map.of()));
  original.sit(0,101,"sit-1");original.sit(1,102,"sit-2");
  StatePayload replayBase=StatePayload.copyOf(original.authoritativeState());
  int firstSeat=((Number)original.authoritativeState().get("currentSeat")).intValue();
  long firstPlayer=((Number)((Map<?,?>)original.authoritativeState().get("players")).get(firstSeat)).longValue();
  int secondSeat=((Number)original.viewFor(firstPlayer==101?102:101).get("viewerSeat")).intValue();
  long secondPlayer=firstPlayer==101?102:101;
  original.execute(new GameCommandRequest("poker.chengdu2999.bet_req","b0",1,9001,1,CxRules.VERSION,String.valueOf(firstPlayer),firstSeat,Map.of("action","FOLLOW","amount",3)));
  var takeover=provider.restoreAuthoritativeSession(original.authoritativeState()).orElseThrow();
  assertEquals(original.authoritativeState(),takeover.authoritativeState());
  var continuation=new GameCommandRequest("poker.chengdu2999.bet_req","b1",2,9001,1,CxRules.VERSION,String.valueOf(secondPlayer),secondSeat,Map.of("action","FOLLOW","amount",3));
  original.execute(continuation);takeover.execute(continuation);
  assertEquivalentIgnoringWallClockDeadline(original.authoritativeState(),takeover.authoritativeState());
  assertEquivalentIgnoringWallClockDeadline(original.viewFor(101),takeover.viewFor(101));
  assertEquals(original.authoritativeState(),provider.eventReplayProvider().orElseThrow().replay(replayBase,original.recordedEvents()).asMap());
  assertTrue(original.invariantViolations().isEmpty());
 }
 @Test void factoryUsesCatalogProxyIdButOwnsOnlyCxIdentity(){var f=new CxProviderFactory();var first=descriptor(74021,"chengdu2999",CxRules.FAMILY);var second=descriptor(981337,"chengdu2999",CxRules.FAMILY);assertEquals(74021,f.create(first).orElseThrow().descriptor().gameId());assertEquals(981337,f.create(second).orElseThrow().descriptor().gameId());assertTrue(f.create(descriptor(74021,"chengdu2004",CxRules.FAMILY)).isEmpty());assertTrue(f.create(descriptor(74021,"chengdu2999","poker:pdk")).isEmpty());}
 @Test void createContractPublishesAndConsumesEveryWorkbookDefault(){var provider=new CxGameProvider(descriptor(74021,"chengdu2999",CxRules.FAMILY));var defaults=provider.defaultConfiguration();assertEquals("chengdu2999",defaults.get("gameCode"));assertEquals(CxRules.FAMILY,defaults.get("family"));assertEquals(CxRules.VERSION,defaults.get("playVersion"));assertEquals(CxRules.from(Map.of()).toMap().entrySet(),defaults.entrySet().stream().filter(e->CxRules.from(Map.of()).toMap().containsKey(e.getKey())).collect(java.util.stream.Collectors.toSet()));var custom=new java.util.HashMap<String,Object>(CxRules.from(Map.of()).toMap());custom.remove("roundCount");custom.put("roomDurationMinutes",60);custom.put("startPlayers",6);custom.put("operationSeconds",30);custom.put("standPolicy","NOBODY");custom.put("mangoFlipMode","ROLLING");custom.put("mangoRaise",5);custom.put("mangoScore",7);custom.put("openingBet",9);custom.put("restMango",false);custom.put("beatMango",false);custom.put("everyHandMango",false);custom.put("firstRoundCanRest",false);custom.put("eachPlayerMustFollow",true);custom.put("earthNineKing",false);custom.put("fireproofCard",false);custom.put("bigHeadKeepsBase",true);var session=provider.createAuthoritativeSession(new RoomCreationContext(9901,101,custom)).orElseThrow();assertEquals(Map.copyOf(custom),session.authoritativeState().get("rules"));}
 @Test void createContractRejectsMalformedTypedValues(){assertThrows(IllegalArgumentException.class,()->CxRules.from(Map.of("restMango","true")));assertThrows(IllegalArgumentException.class,()->CxRules.from(Map.of("mangoRaise",3.5)));assertThrows(IllegalArgumentException.class,()->CxRules.from(Map.of("standPolicy","loser_only")));}
 @Test void serviceLoaderPublishesFactoryOnly(){assertTrue(ServiceLoader.load(PokerFamilyProviderFactory.class).stream().anyMatch(p->p.type()==CxProviderFactory.class));assertTrue(ServiceLoader.load(GameProvider.class).stream().noneMatch(p->p.type()==CxGameProvider.class));}
 @Test void spectatorSitIsAtomicAutoReadyAndReconnectStable(){
  var s=new CxSession(9001,101,7,CxRules.from(Map.of()));
  assertEquals("SPECTATOR",s.viewFor(101).get("viewerRole"));
  assertTrue(((Map<?,?>)s.authoritativeState().get("players")).isEmpty());
  s.sit(0,101,"sit-1");
  int firstSeat=((Number)s.viewFor(101).get("viewerSeat")).intValue();
  assertEquals("SEATED",s.viewFor(101).get("viewerRole"));
  assertEquals(java.util.List.of(firstSeat),s.authoritativeState().get("readySeats"));
  assertEquals("SPECTATOR",s.viewFor(999).get("viewerRole"));
  assertThrows(IllegalStateException.class,()->s.sit(firstSeat,101,"duplicate-player"));
  s.sit(firstSeat,102,"same-click-other-player");
  int secondSeat=((Number)s.viewFor(102).get("viewerSeat")).intValue();
  assertNotEquals(firstSeat,secondSeat);
  assertEquals(CxSession.Phase.BETTING,CxSession.Phase.valueOf((String)s.authoritativeState().get("phase")));
  assertTrue(s.operationDeadline().open());
  int turnSeat=((Number)s.authoritativeState().get("currentSeat")).intValue();
  assertEquals(turnSeat,s.operationDeadline().seatId());
  assertThrows(IllegalStateException.class,()->s.timeout(turnSeat,"early-timeout"));
  assertThrows(IllegalArgumentException.class,()->s.presetBet(8,1,"spectator-action"));
  var restored=CxSession.restore(s.authoritativeState());
  assertEquals("SEATED",restored.viewFor(101).get("viewerRole"));
  assertEquals("SPECTATOR",restored.viewFor(999).get("viewerRole"));
  assertEquals(s.authoritativeState(),restored.authoritativeState());
 }
 @Test void canonicalSitRequiresAndPersistsCarryScore(){
  var s=new CxSession(9011,101,11,CxRules.from(Map.of("startPlayers",4,"roundCount",10)));
  var accepted=s.execute(new GameCommandRequest("poker.chengdu2999.sit_req","sit-carry",1,9011,0,CxRules.VERSION,"101",-1,Map.of("carryScore",1200)));
  int seat=((Number)accepted.body().asMap().get("viewerSeat")).intValue();
  assertEquals(1200L,((Map<?,?>)accepted.body().asMap().get("carryScores")).get(seat));
  assertEquals(1200L,((Map<?,?>)accepted.body().asMap().get("scores")).get(seat));
  assertEquals(s.authoritativeState(),CxSession.restore(s.authoritativeState()).authoritativeState());
  assertThrows(IllegalArgumentException.class,()->new CxSession(9012,101,12,CxRules.from(Map.of())).execute(new GameCommandRequest("poker.chengdu2999.sit_req","missing-carry",1,9012,0,CxRules.VERSION,"101",-1,Map.of())));
  assertThrows(IllegalArgumentException.class,()->new CxSession(9013,101,13,CxRules.from(Map.of())).execute(new GameCommandRequest("poker.chengdu2999.sit_req","negative-carry",1,9013,0,CxRules.VERSION,"101",-1,Map.of("carryScore",-1))));
 }
 @Test void concurrentSpectatorsCannotWinTheSameSeatTwice()throws Exception{var s=new CxSession(9010,101,9,CxRules.from(Map.of()));var gate=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);try{var attempts=java.util.List.of(java.util.concurrent.CompletableFuture.supplyAsync(()->sitAttempt(s,2,201,"race-a",gate),pool),java.util.concurrent.CompletableFuture.supplyAsync(()->sitAttempt(s,2,202,"race-b",gate),pool));gate.countDown();var results=attempts.stream().map(java.util.concurrent.CompletableFuture::join).toList();assertEquals(2,results.stream().filter("OK"::equals).count());assertNotEquals(s.viewFor(201).get("viewerSeat"),s.viewFor(202).get("viewerSeat"));assertEquals(2,((Map<?,?>)s.authoritativeState().get("players")).size());}finally{pool.shutdownNow();}}
 @Test void realPlayerLifecycleAndPrivateReconnect(){var s=new CxSession(9001,101,7,CxRules.from(Map.of()));s.sit(0,101,"sit-1");s.sit(1,102,"sit-2");assertEquals(CxSession.Phase.BETTING,CxSession.Phase.valueOf((String)s.authoritativeState().get("phase")));int opponentSeat=((Number)s.viewFor(102).get("viewerSeat")).intValue();assertEquals(2,((java.util.List<?>)((Map<?,?>)s.viewFor(101).get("hands")).get(opponentSeat)).size());assertEquals(0,((java.util.List<?>)((Map<?,?>)s.viewFor(101).get("hands")).get(opponentSeat)).get(0));assertTrue(s.invariantViolations().isEmpty());}
 private static GameDescriptor descriptor(int gameId,String code,String family){return new GameDescriptor(gameId,code,"成都扯旋",GameCategory.POKER,family,RegionScope.CITY,"SC","CD",CxRules.VERSION);}
 private static GameCommandRequest command(String msgId,String version){return new GameCommandRequest(msgId,"rule-test",1,9001,0,version,"101",0,Map.of());}
 private static String sitAttempt(CxSession s,int seat,long player,String op,java.util.concurrent.CountDownLatch gate){try{gate.await();s.sit(seat,player,op);return"OK";}catch(InterruptedException e){Thread.currentThread().interrupt();return"INTERRUPTED";}catch(IllegalStateException e){return e.getMessage();}}
 private static void assertEquivalentIgnoringWallClockDeadline(Map<String,Object>left,Map<String,Object>right){var a=new java.util.LinkedHashMap<>(left);var b=new java.util.LinkedHashMap<>(right);a.remove("operationDeadline");b.remove("operationDeadline");assertEquals(a,b);assertEquals(((Map<?,?>)left.get("operationDeadline")).get("operationId"),((Map<?,?>)right.get("operationDeadline")).get("operationId"));}
}
