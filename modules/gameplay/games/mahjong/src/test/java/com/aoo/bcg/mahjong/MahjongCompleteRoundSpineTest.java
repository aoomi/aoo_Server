package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MahjongCompleteRoundSpineTest {
    @Test void exposedGangRecordsFourTilesDrawsFromTailAndCanDiscard() {
        MahjongRuleSet<MahjongState> rules=claimRules(1,11,MahjongOperation.GANG);
        MahjongState state=new MahjongState(List.of(41,42),Map.of(0,List.of(11),1,List.of(11,11,11,12,13,14,15,16,17,21,22,23,24)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        state=engine.execute(state,MahjongCommand.discard(0,11),rules);
        int beforeGang=state.hands().get(1).size();
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.GANG,0,List.of(11,11,11)),rules);
        assertEquals(List.of(11,11,11,11),state.melds().get(1).getFirst().tiles());
        assertEquals(42,state.lastDrawnTile());assertTrue(state.currentSeatHasDrawn());assertTrue(state.window().isEmpty());
        assertEquals(beforeGang-3+1,state.hands().get(1).size());
        MahjongState discarded=engine.execute(state,MahjongCommand.discard(1,42),rules);
        assertEquals(beforeGang-3,discarded.hands().get(1).size());
    }

    @Test void exposedGangReplacementTileCanWin() {
        MahjongRuleSet<MahjongState> rules=claimRules(1,11,MahjongOperation.GANG);
        MahjongState state=new MahjongState(List.of(41,24),Map.of(0,List.of(11),1,List.of(11,11,11,12,13,14,15,16,17,21,22,23,24)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        state=engine.execute(state,MahjongCommand.discard(0,11),rules);
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.GANG,0,List.of(11,11,11)),rules);
        assertTrue(engine.execute(state,new MahjongCommand(1,MahjongOperation.HU,24,List.of()),rules).finished());
    }

    @Test void exposedGangWithEmptyWallFinishesAsDraw() {
        MahjongRuleSet<MahjongState> rules=claimRules(1,11,MahjongOperation.GANG);
        MahjongState state=new MahjongState(List.of(),Map.of(0,List.of(11),1,List.of(11,11,11,12,13,14,15,16,17,21,22,23,24)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        state=engine.execute(state,MahjongCommand.discard(0,11),rules);
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.GANG,0,List.of(11,11,11)),rules);
        assertTrue(state.finished());assertTrue(state.window().isEmpty());assertEquals(10,state.hands().get(1).size());
    }

    @Test void chiRecordsDiscardAndConcealedHandCanStillWin() {
        MahjongRuleSet<MahjongState> rules=claimRules(1,11,MahjongOperation.CHI);
        MahjongState state=new MahjongState(List.of(41,24),Map.of(0,List.of(11),1,List.of(12,13,14,15,16,21,22,23,31,31,31,24,25)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        state=engine.execute(state,MahjongCommand.discard(0,11),rules);
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.CHI,0,List.of(12,13)),rules);
        assertEquals(List.of(12,13,11),state.melds().get(1).getFirst().tiles());
        state=engine.execute(state,MahjongCommand.discard(1,25),rules);
        state=engine.execute(state,MahjongCommand.draw(0),rules);
        state=engine.execute(state,MahjongCommand.discard(0,41),rules);
        state=engine.execute(state,MahjongCommand.draw(1),rules);
        assertTrue(engine.execute(state,new MahjongCommand(1,MahjongOperation.HU,24,List.of()),rules).finished());
    }

    @Test void claimedMeldIncludesDiscardAndConcealedHandCanStillWin() {
        MahjongRuleSet<MahjongState> rules=new MahjongRuleSet<>(){
            public Set<MahjongOperation>allowedOperations(int seat,MahjongState state){return seat==1&&state.lastDiscard()==11?Set.of(MahjongOperation.PENG,MahjongOperation.PASS):Set.of();}
            public boolean canWin(int seat,List<Integer>hand,int tile,MahjongState state){return new StandardMahjongWinDetector().isWinning(hand,tile);}
        };
        MahjongState state=new MahjongState(List.of(41,24),Map.of(0,List.of(11),1,List.of(11,11,12,13,14,15,16,17,18,21,21,22,23)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1);
        MahjongCoreEngine engine=new MahjongCoreEngine();
        state=engine.execute(state,MahjongCommand.discard(0,11),rules);
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.PENG,0,List.of(11,11)),rules);
        assertEquals(List.of(11,11,11),state.melds().get(1).getFirst().tiles());
        state=engine.execute(state,MahjongCommand.discard(1,18),rules);
        state=engine.execute(state,MahjongCommand.draw(0),rules);
        state=engine.execute(state,MahjongCommand.discard(0,41),rules);
        state=engine.execute(state,MahjongCommand.draw(1),rules);
        assertEquals(24,state.lastDrawnTile());
        assertTrue(engine.execute(state,new MahjongCommand(1,MahjongOperation.HU,24,List.of()),rules).finished());
    }

    @Test void chengdu1001ContinuesAfterFirstWinUntilOnlyOneSeatRemains() {
        MahjongRuleSet<MahjongState> rules=new MahjongRuleSet<>(){
            public Set<MahjongOperation>allowedOperations(int seat,MahjongState state){return Set.of();}
            public boolean canWin(int seat,List<Integer>hand,int tile,MahjongState state){return true;}
            public boolean continueAfterWin(){return true;}
        };
        MahjongState state=new MahjongState(List.of(22),Map.of(0,List.of(11),1,List.of(12),2,List.of(13)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1,Map.of(),Set.of(),11,MahjongRegionalRules.OpeningSelections.empty());
        MahjongCoreEngine engine=new MahjongCoreEngine();state=engine.execute(state,new MahjongCommand(0,MahjongOperation.HU,11,List.of()),rules);
        assertFalse(state.finished());assertEquals(Set.of(0),state.winners());assertEquals(1,state.currentSeat());
        state=engine.execute(state,MahjongCommand.draw(1),rules);state=engine.execute(state,new MahjongCommand(1,MahjongOperation.HU,22,List.of()),rules);
        assertTrue(state.finished());assertEquals(Set.of(0,1),state.winners());
    }

    @Test void chengdu1001ProfileLocksChengdu1001ContinuationAndOpeningSelections() {
        MahjongRegionRuntimeProfile chengdu1001=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==1001).findFirst().orElseThrow();
        MahjongRuleSet<MahjongState> chengdu1001Rules=new ProfiledMahjongRuleFamily(chengdu1001).ruleSet();
        assertEquals("mahjong:xue-zhan",chengdu1001.family());
        assertTrue(chengdu1001Rules.continueAfterWin());
        assertTrue(chengdu1001Rules.requireOpeningSelections());

        MahjongRegionRuntimeProfile standard=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.family().equals("mahjong:standard")).findFirst().orElseThrow();
        assertFalse(new ProfiledMahjongRuleFamily(standard).ruleSet().continueAfterWin());
    }

    @Test void chengdu1001ProfileContinuesAfterFirstWinAndFinishesWhenOneSeatRemains() {
        MahjongRegionRuntimeProfile chengdu1001=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==1001).findFirst().orElseThrow();
        MahjongRuleSet<MahjongState> rules=new ProfiledMahjongRuleFamily(chengdu1001).ruleSet();
        List<Integer> winning=List.of(21,22,23,24,25,26,27,28,29,31,32,33,34,34);
        List<Integer> waiting=winning.subList(0,13);
        MahjongRegionalRules.OpeningSelections opening=new MahjongRegionalRules.OpeningSelections(Map.of(),Map.of(0,MahjongSuit.WAN,1,MahjongSuit.WAN,2,MahjongSuit.WAN));
        MahjongState state=new MahjongState(List.of(34),Map.of(0,winning,1,waiting,2,List.of(21)),0,true,0,-1,MahjongOperationWindow.empty(),false,-1,Map.of(),Set.of(),34,opening);
        MahjongCoreEngine engine=new MahjongCoreEngine();

        state=engine.execute(state,new MahjongCommand(0,MahjongOperation.HU,34,List.of()),rules);
        assertFalse(state.finished());
        assertEquals(Set.of(0),state.winners());
        assertEquals(1,state.currentSeat());

        state=engine.execute(state,MahjongCommand.draw(1),rules);
        state=engine.execute(state,new MahjongCommand(1,MahjongOperation.HU,34,List.of()),rules);
        assertTrue(state.finished());
        assertEquals(Set.of(0,1),state.winners());
    }

    @Test void exhaustedWallFinishesAndAuthoritativeSettlementIsZeroSum() {
        MahjongRuleFamily family=new StandardMahjongFamily(StandardMahjongConfig.defaults());
        Map<String,Object>round=Map.of("wall",List.of(),"hands",Map.of(0,List.of(),1,List.of()),"currentSeat",0,"currentSeatHasDrawn",false,"lastDiscard",0,"lastDiscardSeat",1,"window",Map.of("candidates",Map.of()),"finished",false,"winnerSeat",-1);
        Map<String,Object>saved=Map.of("roomId",88,"ownerId",880,"seatLimit",2,"seed",1,"playVersion","v1","players",Map.of(0,880,1,881),"roundNo",1,"state",round,"operationDeadline",com.aoo.bcg.gamespi.time.OperationDeadline.none().toMap());
        MahjongAuthoritativeSession session=MahjongAuthoritativeSession.restore(saved,family);
        session.execute(new GameCommandRequest("mahjong.standard.draw_req","draw-empty",1,88,1,"v1","880",0,Map.of()));
        assertTrue((Boolean)session.viewFor(880).get("finished"));
        SettlementPayload settlement=session.settlement(1,"v1");
        assertEquals(0L,settlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum());assertTrue(settlement.scoreDelta().values().stream().allMatch(value->value==0));
    }

    @Test void chengdu1001DispatchAppliesExchangeAndDingqueBlocksForbiddenSuitWin() {
        MahjongRegionRuntimeProfile profile=MahjongRegionRuntimeProfiles.all().stream().filter(item->item.gameId()==1001).findFirst().orElseThrow();
        CatalogMahjongFamilyProvider provider=(CatalogMahjongFamilyProvider)MahjongCatalogRuntimeRegistry.providerFor(descriptor(profile)).orElseThrow();
        AuthoritativeGameSession session=provider.roomFactory().create(new RoomCreationContext(100111,10,RulePayload.copyOf(Map.of("playerNum",2,"shuffleSeed",9)))).requireAuthoritativeSession();
        execute(session,profile,1,0,10,"join",Map.of());execute(session,profile,2,1,20,"join",Map.of());execute(session,profile,3,0,10,"start",Map.of());
        MahjongState before=state(session);List<Integer>first=exchangeChoice(before.hands().get(0)),second=exchangeChoice(before.hands().get(1));
        execute(session,profile,4,0,10,"exchange",Map.of("tiles",first));execute(session,profile,5,1,20,"exchange",Map.of("tiles",second));
        MahjongState exchanged=state(session);assertTrue(exchanged.hands().get(0).containsAll(second));assertTrue(exchanged.hands().get(1).containsAll(first));
        execute(session,profile,6,0,10,"dingque",Map.of("suit","WAN"));execute(session,profile,7,1,20,"dingque",Map.of("suit","TIAO"));
        MahjongState selected=state(session);assertEquals(MahjongSuit.WAN,selected.openingSelections().missingSuits().get(0));
        List<Integer>winning=List.of(11,11,12,13,14,15,16,17,18,21,22,23,24,24);
        MahjongState forbidden=new MahjongState(List.of(),Map.of(0,winning,1,List.of()),0,true,0,-1,MahjongOperationWindow.empty(),false,-1,Map.of(),Set.of(),24,new MahjongRegionalRules.OpeningSelections(Map.of(),Map.of(0,MahjongSuit.WAN)));
        assertFalse(new StandardMahjongRuleSet(new StandardMahjongConfig(false,true,true,true,true)).canWin(0,winning,24,forbidden));
    }

    private static List<Integer>exchangeChoice(List<Integer>hand){for(MahjongSuit suit:MahjongSuit.values()){List<Integer>tiles=hand.stream().filter(suit::contains).limit(3).toList();if(tiles.size()==3)return tiles;}throw new AssertionError("no exchange choice");}
    private static MahjongRuleSet<MahjongState>claimRules(int claimant,int discard,MahjongOperation operation){return new MahjongRuleSet<>(){
        public Set<MahjongOperation>allowedOperations(int seat,MahjongState state){return seat==claimant&&state.lastDiscard()==discard?Set.of(operation,MahjongOperation.PASS):Set.of();}
        public boolean canWin(int seat,List<Integer>hand,int tile,MahjongState state){return new StandardMahjongWinDetector().isWinning(hand,tile);}
    };}
    private static MahjongState state(AuthoritativeGameSession session){return (MahjongState)((Map<?,?>)session.authoritativeState().get("mahjongAuthority")).get("state");}
    private static void execute(AuthoritativeGameSession session,MahjongRegionRuntimeProfile profile,long sequence,int seat,long player,String action,Map<String,Object>payload){session.execute(new GameCommandRequest("mahjong."+profile.code()+".dispatch","spine"+sequence,sequence,100111,1,"1.0.0",String.valueOf(player),seat,Map.of("action",action,"payload",payload)));}
    private static GameDescriptor descriptor(MahjongRegionRuntimeProfile profile){return new GameDescriptor(profile.gameId(),profile.code(),profile.code(),GameCategory.MAHJONG,profile.family().replace(':','-'),RegionScope.PROVINCE,"四川省","","1.0.0");}
}
