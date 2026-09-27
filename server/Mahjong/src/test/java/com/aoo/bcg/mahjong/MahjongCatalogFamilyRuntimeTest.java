package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class MahjongCatalogFamilyRuntimeTest {
    @Test void cd101RejectsChiAndStaleGangAtPublicDispatch() {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream()
                .filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry
                .providerFor(descriptor(profile)).orElseThrow();
        AuthoritativeGameSession session=provider.roomFactory().create(new RoomCreationContext(51691,10,
                RulePayload.copyOf(Map.of("playerNum",2,"setCount",1)))).requireAuthoritativeSession();
        long before=session.stateVersion();
        assertThrows(IllegalArgumentException.class,()->session.execute(command(profile,51691,1,0,10,"chi",Map.of())));
        assertEquals(before,session.stateVersion());
        assertThrows(IllegalStateException.class,()->session.execute(command(profile,51691,2,0,10,"gang",
                Map.of("type",4,"mahjongId",11,"stateVersion",before-1))));
        assertEquals(before,session.stateVersion());
        assertEquals(List.of(),session.viewFor(10).get("gangCandidates"));
    }
    @Test void all364ProfilesRouteThroughFiveFamilyEntriesAndTwoMissingSourcesStayBlocked() {
        assertEquals(364,MahjongCatalogRuntimeRegistry.profileCount());
        assertEquals(362,MahjongCatalogRuntimeRegistry.runnableProfileCount());
        assertEquals(Set.of("mahjong:standard","mahjong:lai-zi","mahjong:tui-dao-hu","mahjong:xue-zhan","mahjong:xue-liu"),MahjongCatalogRuntimeRegistry.registeredFamilies());
        int runnable=0,blocked=0;
        for(var profile:MahjongRegionRuntimeProfiles.all()){
            var provider=MahjongCatalogRuntimeRegistry.providerFor(descriptor(profile));
            if(profile.sourceAvailable()){
                runnable++;
                CatalogMahjongFamilyProvider family=assertInstanceOf(CatalogMahjongFamilyProvider.class,provider.orElseThrow());
                assertEquals(profile.family(),family.profile().family());
                assertEquals(profile.configSchemaHash(),family.defaultConfiguration().get("configSchemaHash"));
                assertTrue(family.ruleComponents().size()>=1+profile.ruleComponents().size()+profile.lifecycleComponents().size());
            } else {blocked++;assertTrue(provider.isEmpty());assertTrue(Set.of("caooszmj","dlaoomj").contains(profile.code()));}
        }
        assertEquals(362,runnable);assertEquals(2,blocked);
    }

    @Test void eachOfFiveFamiliesPassesOneSharedNineStageLifecycle() {
        Map<String,MahjongRegionRuntimeProfile> representative=MahjongRegionRuntimeProfiles.all().stream().filter(MahjongRegionRuntimeProfile::sourceAvailable)
                .collect(Collectors.toMap(MahjongRegionRuntimeProfile::family,p->p,(a,b)->a,LinkedHashMap::new));
        assertEquals(5,representative.size());
        long roomSeed=70000;
        for(var profile:representative.values()) runNineStages(profile,++roomSeed);
    }

    @Test void cd101MatchSettlementSurvivesRestoreAndExposesBootstrapTriggerFields() {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream()
                .filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry
                .providerFor(descriptor(profile)).orElseThrow();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(51601,10,
                RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",7,"setCount",2))));
        Map<String,Object> state=new LinkedHashMap<>(room.requireAuthoritativeSession().authoritativeState());
        assertEquals(false,state.get("roundScored"));
        assertFalse(state.containsKey("roundScoreDelta"));
        CDXZMatchSettlementState match=new CDXZMatchSettlementState(2);
        match.record(51601,1,"finish-round-1",Map.of(10L,4L,20L,-4L));
        state.put("cdxzMatchSettlement",match.snapshot());
        state.put("phase","SETTLED");
        Map<String,Object> coreState=new LinkedHashMap<>(map(state.get("mahjongAuthority")));
        coreState.put("roundNo",1);
        state.put("mahjongAuthority",coreState);

        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(state).orElseThrow();
        Map<String,Object> authority=restored.authoritativeState();
        assertEquals(1,authority.get("roundNo"));
        assertEquals(true,authority.get("roundScored"));
        assertEquals(Map.of(10L,4L,20L,-4L),authority.get("roundScoreDelta"));
        assertEquals(false,authority.get("roomFinished"));
        assertEquals(Map.of(10L,4L,20L,-4L),authority.get("totalScore"));
        assertEquals(Map.of(10L,4L,20L,-4L),restored.viewFor(10).get("roundScoreDelta"));
        assertEquals(authority,provider.restoreAuthoritativeSession(authority).orElseThrow().authoritativeState());
        SettlementPayload settlement=provider.settlementProvider().orElseThrow()
                .settle(new GameRoomHandle(51601,profile.gameId(),"1.0.0",restored),1);
        assertEquals(Map.of(10L,4L,20L,-4L),settlement.scoreDelta());
    }

    @Test void nonCd101ViewerDoesNotExposeCd101RoundSettlementProjection() {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream()
                .filter(item->item.sourceAvailable()&&item.gameId()!=516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry
                .providerFor(descriptor(profile)).orElseThrow();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(51701,10,
                RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",7))));

        Map<String,Object> view=room.requireAuthoritativeSession().viewFor(10);
        Map<String,Object> authority=room.requireAuthoritativeSession().authoritativeState();
        assertFalse(view.containsKey("roundScoreDelta"));
        assertFalse(authority.containsKey("roundScoreDelta"));
        assertFalse(view.containsKey("roundScored"));
        assertFalse(authority.containsKey("roundScored"));
    }

    @Test void cd101OnlyMigratesUnfinishedSnapshotsWithoutMatchState() {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream()
                .filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry
                .providerFor(descriptor(profile)).orElseThrow();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(51602,10,
                RulePayload.copyOf(Map.of("playerNum",2,"setCount",2))));
        Map<String,Object> legacy=new LinkedHashMap<>(room.requireAuthoritativeSession().authoritativeState());
        legacy.remove("cdxzMatchSettlement");
        assertEquals(0,provider.restoreAuthoritativeSession(legacy).orElseThrow().authoritativeState().get("roundNo"));
        Map<String,Object> finishedLegacy=new LinkedHashMap<>(legacy);
        Map<String,Object> authority=new LinkedHashMap<>(map(legacy.get("mahjongAuthority")));
        authority.put("state",Map.of("finished",true));
        finishedLegacy.put("mahjongAuthority",authority);
        assertThrows(IllegalArgumentException.class,
                ()->provider.restoreAuthoritativeSession(finishedLegacy).orElseThrow());
    }

    @Test void cd101ContinueIsIdempotentRestorableAndStartsNextRoundOnlyAfterAllPlayers() throws Exception {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(profile)).orElseThrow();
        long roomId=51603;
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(roomId,10,RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",31,"setCount",2))));
        GameCommandHandler handler=provider.commandHandler().orElseThrow();
        handler.handle(room,command(profile,roomId,1,0,10,"join",Map.of()));
        handler.handle(room,command(profile,roomId,2,1,20,"join",Map.of()));
        handler.handle(room,command(profile,roomId,3,0,10,"start",Map.of()));
        Map<String,Object> settled=finishedState(room.requireAuthoritativeSession().authoritativeState());
        CDXZMatchSettlementState match=new CDXZMatchSettlementState(2);
        match.record(roomId,1,"settled-1",Map.of(10L,4L,20L,-4L));
        settled.put("cdxzMatchSettlement",match.snapshot());
        settled.put("phase","SETTLED");
        AuthoritativeGameSession session=provider.restoreAuthoritativeSession(settled).orElseThrow();

        session.execute(command(profile,roomId,4,0,10,"continue",Map.of()));
        assertEquals(List.of(0),session.viewFor(10).get("continueSeats"));
        assertEquals("SETTLED",session.authoritativeState().get("phase"));
        long afterFirst=session.stateVersion();
        var partialMapper=new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked") Map<String,Object> partialJson=partialMapper.readValue(partialMapper.writeValueAsString(session.authoritativeState()),Map.class);
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(partialJson).orElseThrow();
        assertEquals(List.of(0),restored.viewFor(10).get("continueSeats"));
        restored.execute(command(profile,roomId,4,0,10,"continue",Map.of()));
        assertEquals(afterFirst,restored.stateVersion());
        assertEquals(List.of(0),restored.viewFor(10).get("continueSeats"));
        assertThrows(IllegalArgumentException.class,()->restored.execute(command(profile,roomId,4,1,20,"continue",Map.of())));
        assertThrows(IllegalArgumentException.class,()->restored.execute(command(profile,roomId,4,0,20,"continue",Map.of())));
        assertThrows(IllegalArgumentException.class,()->restored.execute(new GameCommandRequest("mahjong."+profile.code()+".dispatch","cdxzmj4",4,roomId,2,"1.0.0","10",0,Map.of("action","continue","payload",Map.of()))));
        assertThrows(IllegalArgumentException.class,()->restored.execute(new GameCommandRequest("mahjong."+profile.code()+".dispatch","cdxzmj4",4,roomId+1,1,"1.0.0","10",0,Map.of("action","continue","payload",Map.of()))));
        assertThrows(IllegalArgumentException.class,()->restored.execute(new GameCommandRequest("mahjong."+profile.code()+".dispatch","cdxzmj4",4,roomId,1,"wrong-version","10",0,Map.of("action","continue","payload",Map.of()))));
        assertThrows(SecurityException.class,()->restored.execute(command(profile,roomId,40,0,99,"continue",Map.of())));

        restored.execute(command(profile,roomId,5,1,20,"continue",Map.of()));
        Map<String,Object> authority=restored.authoritativeState();
        assertEquals("PLAYING",authority.get("phase"));
        assertEquals(List.of(),restored.viewFor(10).get("continueSeats"));
        assertEquals(2,restored.viewFor(10).get("roundNo"));
        assertEquals(false,restored.viewFor(10).get("roundScored"));
        assertFalse(restored.viewFor(10).containsKey("roundScoreDelta"));
        assertEquals(1,restored.viewFor(10).get("completedRounds"));
        assertEquals(2,map(authority.get("mahjongAuthority")).get("roundNo"));
        assertFalse(((MahjongState)map(authority.get("mahjongAuthority")).get("state")).finished());
        long afterStart=restored.stateVersion();
        restored.execute(command(profile,roomId,5,1,20,"continue",Map.of()));
        assertEquals(afterStart,restored.stateVersion());

        Map<String,Object> roundTwoSettled=finishedState(restored.authoritativeState());
        CDXZMatchSettlementState twoRounds=CDXZMatchSettlementState.restore(roundTwoSettled.get("cdxzMatchSettlement"));
        twoRounds.record(roomId,2,"settled-2",Map.of(10L,-2L,20L,2L));
        roundTwoSettled.put("cdxzMatchSettlement",twoRounds.snapshot());
        roundTwoSettled.put("phase","SETTLED");
        AuthoritativeGameSession objectRestored=provider.restoreAuthoritativeSession(roundTwoSettled).orElseThrow();
        assertEquals(2,objectRestored.viewFor(10).get("roundNo"));
        assertEquals(true,objectRestored.viewFor(10).get("roundScored"));
        assertEquals(Map.of(10L,-2L,20L,2L),objectRestored.viewFor(10).get("roundScoreDelta"));
        assertEquals(Map.of(10L,2L,20L,-2L),objectRestored.viewFor(10).get("totalScore"));
        assertEquals(2,objectRestored.viewFor(10).get("completedRounds"));
        assertEquals(true,objectRestored.viewFor(10).get("roomFinished"));

        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked") Map<String,Object> jsonSnapshot=mapper.readValue(mapper.writeValueAsString(roundTwoSettled),Map.class);
        AuthoritativeGameSession jsonRestored=provider.restoreAuthoritativeSession(jsonSnapshot).orElseThrow();
        assertEquals(objectRestored.authoritativeState(),jsonRestored.authoritativeState());
        assertEquals(objectRestored.viewFor(20),jsonRestored.viewFor(20));
    }

    @Test void cd101V2CatalogCommandsAuthoritativelySettleTwoConsecutiveRounds() throws Exception {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(profile)).orElseThrow();
        long roomId=51606;
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(roomId,10,RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",31,"setCount",2))));
        AuthoritativeGameSession session=room.requireAuthoritativeSession();
        session.execute(commandRound(profile,roomId,1,1,0,10,"join",Map.of()));
        session.execute(commandRound(profile,roomId,2,1,1,20,"join",Map.of()));
        session.execute(commandRound(profile,roomId,3,1,0,10,"start",Map.of()));

        long sequence=playRoundToSettlement(session,profile,roomId,1,4);
        assertEquals("SETTLED",session.authoritativeState().get("phase"));
        assertEquals(1,session.viewFor(10).get("roundNo"));
        assertEquals(true,session.viewFor(10).get("roundScored"));
        @SuppressWarnings("unchecked") Map<Long,Long> firstDelta=(Map<Long,Long>)session.viewFor(10).get("roundScoreDelta");

        session.execute(commandRound(profile,roomId,sequence++,1,0,10,"continue",Map.of()));
        session.execute(commandRound(profile,roomId,sequence++,1,1,20,"continue",Map.of()));
        assertEquals("PLAYING",session.authoritativeState().get("phase"));
        assertEquals(2,session.viewFor(10).get("roundNo"));
        assertEquals(false,session.viewFor(10).get("roundScored"));

        playRoundToSettlement(session,profile,roomId,2,sequence);
        Map<String,Object> finalView=session.viewFor(10);
        assertEquals("SETTLED",session.authoritativeState().get("phase"));
        assertEquals(2,finalView.get("roundNo"));
        assertEquals(true,finalView.get("roundScored"));
        assertEquals(2,finalView.get("completedRounds"));
        assertEquals(true,finalView.get("roomFinished"));
        @SuppressWarnings("unchecked") Map<Long,Long> secondDelta=(Map<Long,Long>)finalView.get("roundScoreDelta");
        @SuppressWarnings("unchecked") Map<Long,Long> total=(Map<Long,Long>)finalView.get("totalScore");
        assertEquals(firstDelta.get(10L)+secondDelta.get(10L),total.get(10L));
        assertEquals(firstDelta.get(20L)+secondDelta.get(20L),total.get(20L));

        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked") Map<String,Object> json=mapper.readValue(mapper.writeValueAsString(session.authoritativeState()),Map.class);
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(json).orElseThrow();
        assertEquals(session.authoritativeState(),restored.authoritativeState());
        assertEquals(finalView,restored.viewFor(10));
    }

    @Test void cd101ContinueRejectsFinishedMatchAndOtherMahjongProfilesStayIsolated() {
        MahjongRegionRuntimeProfile cd101=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(cd101)).orElseThrow();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(51604,10,RulePayload.copyOf(Map.of("playerNum",2,"setCount",1))));
        Map<String,Object> settled=new LinkedHashMap<>(room.requireAuthoritativeSession().authoritativeState());
        CDXZMatchSettlementState finished=new CDXZMatchSettlementState(1);
        finished.record(51604,1,"settled-final",Map.of(10L,1L,20L,-1L));
        settled.put("cdxzMatchSettlement",finished.snapshot());
        settled.put("phase","SETTLED");
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(settled).orElseThrow();
        assertThrows(IllegalStateException.class,()->restored.execute(command(cd101,51604,1,0,10,"continue",Map.of())));

        MahjongRegionRuntimeProfile other=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.sourceAvailable()&&item.gameId()!=516).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider otherProvider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(other)).orElseThrow();
        AuthoritativeGameSession otherSession=otherProvider.roomFactory().create(new RoomCreationContext(51605,10,RulePayload.copyOf(Map.of("playerNum",2)))).requireAuthoritativeSession();
        assertThrows(IllegalArgumentException.class,()->otherSession.execute(command(other,51605,1,0,10,"continue",Map.of())));
    }

    private static void runNineStages(MahjongRegionRuntimeProfile profile,long roomId){
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(profile)).orElseThrow();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(roomId,10,RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",7))));
        GameCommandHandler handler=provider.commandHandler().orElseThrow();
        StatePayload base=StatePayload.copyOf(room.requireAuthoritativeSession().authoritativeState());
        handler.handle(room,command(profile,roomId,1,0,10,"join",Map.of()));
        handler.handle(room,command(profile,roomId,2,1,20,"join",Map.of()));
        handler.handle(room,command(profile,roomId,3,0,10,"ready",Map.of()));
        handler.handle(room,command(profile,roomId,4,1,20,"ready",Map.of()));
        handler.handle(room,command(profile,roomId,5,0,10,"start",Map.of()));
        CatalogMahjongFamilySession session=(CatalogMahjongFamilySession)room.requireAuthoritativeSession();
        MahjongState state=(MahjongState)((Map<?,?>)session.authoritativeState().get("mahjongAuthority")).get("state");
        int seat=state.currentSeat(),tile=state.hands().get(seat).getFirst();
        handler.handle(room,command(profile,roomId,6,seat,seat==0?10:20,"discard",Map.of("tile",tile)));
        assertTrue(session.invariantViolations().isEmpty(),profile.code());
        assertEquals(session.authoritativeState(),provider.eventReplayProvider().orElseThrow().replay(base,session.recordedEvents()).asMap(),profile.code());
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(session.authoritativeState()).orElseThrow();
        assertEquals(session.viewFor(20),provider.reconnectViewProvider().orElseThrow().buildFor(20,new GameRoomHandle(roomId,profile.gameId(),"1.0.0",restored)),profile.code());
        AuthoritativeGameSession finished=provider.restoreAuthoritativeSession(finishedState(session.authoritativeState())).orElseThrow();
        SettlementPayload settlement=provider.settlementProvider().orElseThrow().settle(new GameRoomHandle(roomId,profile.gameId(),"1.0.0",finished),1);
        assertEquals(0L,settlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum(),profile.code());
    }

    private static Map<String,Object>finishedState(Map<String,Object>state){Map<String,Object>out=new LinkedHashMap<>(state),authority=new LinkedHashMap<>(map(state.get("mahjongAuthority")));MahjongState item=(MahjongState)authority.get("state");Map<String,Object>s=new LinkedHashMap<>();s.put("wall",item.wall());s.put("hands",item.hands());s.put("currentSeat",item.currentSeat());s.put("currentSeatHasDrawn",item.currentSeatHasDrawn());s.put("lastDiscard",item.lastDiscard());s.put("lastDiscardSeat",item.lastDiscardSeat());s.put("window",Map.of("candidates",Map.of()));s.put("finished",true);s.put("winnerSeat",0);authority.put("state",s);out.put("mahjongAuthority",authority);return out;}
    private static GameCommandRequest command(MahjongRegionRuntimeProfile p,long room,long seq,int seat,long user,String action,Map<String,Object>payload){return new GameCommandRequest("mahjong."+p.code()+".dispatch",p.code()+seq,seq,room,1,"1.0.0",String.valueOf(user),seat,Map.of("action",action,"payload",payload));}
    private static GameCommandRequest commandRound(MahjongRegionRuntimeProfile p,long room,long seq,int round,int seat,long user,String action,Map<String,Object>payload){return new GameCommandRequest("mahjong."+p.code()+".dispatch",p.code()+seq,seq,room,round,"1.0.0",String.valueOf(user),seat,Map.of("action",action,"payload",payload));}
    private static long playRoundToSettlement(AuthoritativeGameSession session,MahjongRegionRuntimeProfile profile,long roomId,int round,long sequence){StandardMahjongWinDetector wins=new StandardMahjongWinDetector();for(int step=0;step<600;step++){MahjongState state=(MahjongState)map(session.authoritativeState().get("mahjongAuthority")).get("state");if(state.finished())return sequence;if(!state.window().isEmpty()){Optional<Integer>winner=state.window().candidates().entrySet().stream().filter(entry->entry.getValue().contains(MahjongOperation.HU)).map(Map.Entry::getKey).findFirst();if(winner.isPresent()){int seat=winner.orElseThrow();session.execute(commandRound(profile,roomId,sequence++,round,seat,seat==0?10:20,"hu",Map.of("tile",state.lastDiscard())));continue;}int seat=state.window().candidates().keySet().stream().sorted().findFirst().orElseThrow();session.execute(commandRound(profile,roomId,sequence++,round,seat,seat==0?10:20,"pass",Map.of()));continue;}int seat=state.currentSeat();long player=seat==0?10:20;if(state.currentSeatHasDrawn()){List<Integer>hand=state.hands().get(seat);if(wins.isWinning(hand,state.lastDrawnTile())){session.execute(commandRound(profile,roomId,sequence++,round,seat,player,"hu",Map.of("tile",state.lastDrawnTile())));}else{session.execute(commandRound(profile,roomId,sequence++,round,seat,player,"discard",Map.of("tile",hand.getLast())));}}else{if(state.wall().isEmpty())fail("round exhausted wall before authoritative settlement");session.execute(commandRound(profile,roomId,sequence++,round,seat,player,"draw",Map.of()));}}fail("round did not settle within command bound");return sequence;}
    private static GameDescriptor descriptor(MahjongRegionRuntimeProfile p){String[]region=p.region().split("/",2);RegionScope scope=region[0].equals("NATIONAL")?RegionScope.NATIONAL:region[0].equals("CITY")?RegionScope.CITY:RegionScope.PROVINCE;String province=scope==RegionScope.NATIONAL?"":region.length>1?region[1]:"";return new GameDescriptor(p.gameId(),p.code(),p.code(),GameCategory.MAHJONG,p.family().replace(':','-'),scope,province,"","1.0.0");}
    private static Map<String,Object>map(Object value){Map<String,Object>out=new LinkedHashMap<>();((Map<?,?>)value).forEach((k,v)->out.put(String.valueOf(k),v));return out;}
}
