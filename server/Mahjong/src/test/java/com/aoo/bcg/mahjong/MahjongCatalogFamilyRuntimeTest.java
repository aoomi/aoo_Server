package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class MahjongCatalogFamilyRuntimeTest {
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
    private static GameDescriptor descriptor(MahjongRegionRuntimeProfile p){String[]region=p.region().split("/",2);RegionScope scope=region[0].equals("NATIONAL")?RegionScope.NATIONAL:region[0].equals("CITY")?RegionScope.CITY:RegionScope.PROVINCE;String province=scope==RegionScope.NATIONAL?"":region.length>1?region[1]:"";return new GameDescriptor(p.gameId(),p.code(),p.code(),GameCategory.MAHJONG,p.family().replace(':','-'),scope,province,"","1.0.0");}
    private static Map<String,Object>map(Object value){Map<String,Object>out=new LinkedHashMap<>();((Map<?,?>)value).forEach((k,v)->out.put(String.valueOf(k),v));return out;}
}
