package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;

final class PokerRuleMatrixTest {
    @Test void resolvesAllFirstLeadStrategiesWithoutFallback() {
        Map<Integer,List<Integer>> hands=Map.of(0,List.of(404),1,List.of(104),2,List.of(105));
        assertEquals(0,PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.ROOM_OWNER,null),hands,0,2,7));
        assertEquals(2,PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.PREVIOUS_WINNER,null),hands,0,2,7));
        assertThrows(IllegalStateException.class,()->PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.PREVIOUS_WINNER,null),hands,0,null,7));
        assertEquals(0,PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER,404),hands,1,null,7));
        assertEquals(PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.RANDOM,null),hands,0,null,7),PokerFirstLeadResolver.resolve(profile(PokerRuleProfile.FirstLead.RANDOM,null),hands,0,null,7));
    }
    @Test void deckProfilesAndWireMappingAreCompleteAndUnique() {
        for (int size : List.of(48, 52, 54)) {
            List<Integer> deck = PokerCardCodec.deck(size);
            assertEquals(size, deck.size()); assertEquals(size, Set.copyOf(deck).size());
            deck.forEach(card -> assertEquals(card, PokerCardCodec.encode(PokerCardCodec.suit(card), PokerCardCodec.rank(card))));
        }
        assertThrows(IllegalArgumentException.class, () -> PokerCardCodec.deck(50));
        assertThrows(IllegalArgumentException.class, () -> PokerCardCodec.validate(116));
        assertThrows(IllegalArgumentException.class, () -> PokerCardCodec.validate(503));
        PokerCardCodec.deck(52).forEach(card -> assertEquals(card, PokerCardCodec.fromLegacyNibble(PokerCardCodec.toLegacyNibble(card))));
        var p = PokerRuleProfile.paoDeKuai("xcpdk-v3", 48, 404);
        p.validatePlayerCount(3); assertThrows(IllegalStateException.class, () -> p.validatePlayerCount(4));
        PokerRuleProfile.paoDeKuai("52",52,null).validatePlayerCount(4);PokerRuleProfile.paoDeKuai("54",54,null).validatePlayerCount(3);
        assertThrows(IllegalStateException.class,()->PokerRuleProfile.paoDeKuai("54",54,null).validatePlayerCount(4));
        assertThrows(IllegalArgumentException.class,()->PokerRuleProfile.paoDeKuai("48",48,103));
        assertThrows(IllegalArgumentException.class, () -> p.validateRun(List.of(10,11,12,13,14,15), false));
        List<Integer>cut=new ArrayList<>(PokerCardCodec.deck(48));cut.set(cut.indexOf(104),403);var custom=new PokerRuleProfile("cut",48,3,3,PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER,403,5,2,false,false,true,true,1,16,2,20,cut);assertTrue(custom.deck().contains(403));assertFalse(custom.deck().contains(104));
    }

    @Test void recognitionComparisonAndAttachmentsFollowMatrix() {
        var rules = new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,true,null,false));
        assertEquals("TRIPLE_WITH_PAIR", rules.recognize(List.of(103,203,303,104,204), null).type());
        assertEquals("AIRPLANE_WITH_PAIRS", rules.recognize(List.of(103,203,303,104,204,304,105,205,106,206), null).type());
        assertEquals("FOUR_WITH_THREE", rules.recognize(List.of(107,207,307,407,108,109,110), null).type());
        assertThrows(IllegalArgumentException.class, () -> rules.recognize(List.of(111,112,113,114,115), null));
        var bomb = rules.recognize(List.of(107,207,307,407), null);
        var single = rules.recognize(List.of(115), null);
        assertTrue(rules.canBeat(bomb, single, null)); assertFalse(rules.canBeat(single, bomb, null));
        var allowTwo=new PokerRuleProfile("plane-two",52,4,4,PokerRuleProfile.FirstLead.RANDOM,null,5,2,true,false,true,false,1,16,2,20);
        assertEquals("AIRPLANE",new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,false,null,false),allowTwo).recognize(List.of(114,214,314,115,215,315),null).type());
        assertThrows(IllegalArgumentException.class,()->rules.recognize(List.of(114,214,314,115,215,315),null));
        var six=new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(6,true,true,false,null,false));assertThrows(IllegalArgumentException.class,()->six.recognize(List.of(103,104,105,106,107),null));
    }
    @Test void optionalFourWithThreeRequiresExplicitVariantPolicy() {
        var config = new PaoDeKuaiConfig(5,true,true,true,null,false);
        var family = new PaoDeKuaiFamily(config,
                PokerRuleProfile.fromConfig("optional-four-three",52,config));
        var combination = family.rules().recognize(List.of(107,207,307,407,108,109,110),null);
        assertEquals("FOUR_WITH_THREE",combination.type());
        assertThrows(IllegalArgumentException.class,
                () -> PdkVariantPolicy.standard(family).validatePattern(null,combination,null));
        assertDoesNotThrow(() -> PdkVariantPolicy.withExplicitOptionalPatterns(family,
                Set.of("FOUR_WITH_THREE")).validatePattern(null,combination,null));
    }
    @Test void regionalBombTiersAndTerminalAttachmentShortageAreCapabilities(){var cfg=new PaoDeKuaiConfig(5,true,true,false,null,false,14,true,true,1,3,2,true);var rules=new PaoDeKuaiRuleSet(cfg);CardCombination special=rules.recognize(List.of(114,214,314),null),normal=rules.recognize(List.of(113,213,313,413),null),withOne=rules.recognize(List.of(114,214,314,105),null),fourOne=rules.recognize(List.of(113,213,313,413,105),null);assertTrue(rules.isBomb(special));assertTrue(rules.canBeat(special,normal,null));assertTrue(rules.canBeat(withOne,fourOne,null));var terminal=new PaoDeKuaiContext(false,2,List.of(110,210,310));assertTrue(rules.canBeat(rules.recognize(terminal.handBeforePlay(),terminal),new CardCombination("TRIPLE_WITH_PAIR",9,List.of(109,209,309,108,208)),terminal));var strict=new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,false,null,false));assertFalse(strict.canBeat(strict.recognize(terminal.handBeforePlay(),terminal),new CardCombination("TRIPLE_WITH_PAIR",9,List.of(109,209,309,108,208)),terminal));}

    @Test void tripleWithTwoComparisonUsesTripleBodyAndPreservesTerminalShortagePolicy() {
        var strict = new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,false,null,false));
        var previous = strict.recognize(List.of(111,211,311,109,107),null);
        assertEquals("TRIPLE_WITH_TWO",previous.type());
        assertFalse(strict.canBeat(strict.recognize(List.of(104,204,304),null),previous,
                new PaoDeKuaiContext(false,2,List.of(104,204,304))));
        assertFalse(strict.canBeat(strict.recognize(List.of(104,204,304,105,106),null),previous,null));
        assertTrue(strict.canBeat(strict.recognize(List.of(112,212,312,103,104),null),previous,null));

        var shortage = new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(
                5,true,true,false,null,false,null,false,false,1,2,1,true));
        assertFalse(shortage.canBeat(shortage.recognize(List.of(104,204,304),null),previous,
                new PaoDeKuaiContext(false,2,List.of(104,204,304))));
        assertTrue(shortage.canBeat(shortage.recognize(List.of(112,212,312),null),previous,
                new PaoDeKuaiContext(false,2,List.of(112,212,312))));
        assertFalse(shortage.canBeat(shortage.recognize(List.of(104,204,304,105),null),previous,
                new PaoDeKuaiContext(false,2,List.of(104,204,304,105))));
    }

    @Test void hintsAreLegalCompleteStableAndIncludeTerminalPlay() {
        var rules = new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,true,null,false));
        List<Integer> hand=List.of(104,204,304,404,105,205,106,206,107,207);
        var previous=rules.recognize(List.of(103),null);
        var first=rules.hints(hand,previous,new PaoDeKuaiContext(false,2,hand));
        var second=rules.hints(hand,previous,new PaoDeKuaiContext(false,2,hand));
        assertEquals(first,second);assertFalse(first.isEmpty());
        first.forEach(h -> { assertTrue(hand.containsAll(h.cards())); assertTrue(rules.canBeat(h,previous,null)); assertEquals(h,rules.recognize(h.cards(),null)); });
        assertTrue(first.stream().anyMatch(h -> h.type().equals("BOMB")));
        assertTrue(rules.hints(List.of(103), null, new PaoDeKuaiContext(true,2,List.of(103))).stream().anyMatch(h -> h.cards().size()==1));
        var requiredRules=new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,false,103,true));
        assertTrue(requiredRules.hints(List.of(103,104),null,new PaoDeKuaiContext(true,2,List.of(103,104))).stream().allMatch(h->h.cards().contains(103)));
        assertEquals(List.of(114),requiredRules.hints(List.of(103,114),null,new PaoDeKuaiContext(false,1,List.of(103,114))).getFirst().cards());
    }

    @Test void responseHintsPreserveTargetTypeAndCardCountBeforeRanking() {
        var rules = new PaoDeKuaiRuleSet(PaoDeKuaiConfig.defaults());
        var previousPairs = rules.recognize(List.of(104,204,105,205),null);
        var hand = List.of(106,206,107,207,108,208,109,209);
        var hints = rules.hints(hand,previousPairs,new PaoDeKuaiContext(false,8,hand));
        assertEquals(List.of(106,206,107,207),hints.getFirst().cards());
        assertTrue(hints.stream().filter(hint -> !rules.isBomb(hint))
                .allMatch(hint -> hint.type().equals("CONSECUTIVE_PAIRS") && hint.cards().size()==4));
        var sixCards = List.of(107,207,108,208,109,209);
        assertTrue(rules.hints(sixCards,previousPairs,new PaoDeKuaiContext(false,8,sixCards)).stream()
                .allMatch(hint -> rules.isBomb(hint) || hint.cards().size()==4));
        assertFalse(rules.canBeat(rules.recognize(List.of(106,107,108,109,110,111),null),
                rules.recognize(List.of(103,104,105,106,107),null),null));
        assertFalse(rules.canBeat(rules.recognize(List.of(107,207,307,108,208,308),null),
                new CardCombination("AIRPLANE",6,List.of(106,206,306,107,207,307,108,208,308)),null));
        assertTrue(rules.canBeat(rules.recognize(List.of(106,206,306,406),null),previousPairs,null));
    }

    @Test void everyPdkProfileForcesHighestSingleWhenNextPlayerReported() {
        var config = new PaoDeKuaiConfig(5,true,true,false,null,false);
        var profile = new PokerRuleProfile("regional",52,2,4,PokerRuleProfile.FirstLead.RANDOM,null,
                5,2,false,false,true,false,1,16,2,20);
        var rules = new PaoDeKuaiRuleSet(config,profile);
        var context = new PaoDeKuaiContext(false,1,List.of(103,110,114));
        var error = assertThrows(IllegalStateException.class,() -> rules.validatePlay(List.of(110),context));
        assertEquals("下家报单,必须打最大单张",error.getMessage());
        assertDoesNotThrow(() -> rules.validatePlay(List.of(114),context));
        assertDoesNotThrow(() -> rules.validatePlay(List.of(103,203),context));
        assertTrue(rules.hints(context.handBeforePlay(),null,context).stream()
                .filter(hint -> "SINGLE".equals(hint.type())).allMatch(hint -> hint.primaryRank()==14));
    }

    @Test void passCycleReturnsLeadToLastPlayerAndSkipsPassedSeats() {
        var core=new PokerCoreEngine<Void>();
        var lead=new CardCombination("SINGLE",3,List.of(103));
        var state=new PokerTurnState(Map.of(0,List.of(104),1,List.of(105),2,List.of(106)),1,lead,0,Set.of(),false,-1);
        state=core.pass(state,1);assertEquals(2,state.currentSeat());
        state=core.pass(state,2);assertEquals(0,state.currentSeat());assertNull(state.previous());
        PokerTurnState cleared=state;assertThrows(IllegalStateException.class,()->core.pass(cleared,0));
    }

    @Test void settlementBreakdownIsRecomputableCappedAndZeroSum() {
        Map<Integer,PokerSettlementCalculator.SeatInput> seats=new LinkedHashMap<>();
        seats.put(0,new PokerSettlementCalculator.SeatInput(10,0,8,2));
        seats.put(1,new PokerSettlementCalculator.SeatInput(11,8,0,0));
        seats.put(2,new PokerSettlementCalculator.SeatInput(12,5,0,1));
        var calc=new PokerSettlementCalculator();var a=calc.calculate("njpdk-v2",0,seats,2,2);var b=calc.calculate("njpdk-v2",0,seats,2,2);
        assertEquals(a,b);assertEquals(2,a.multiplier());assertEquals(0,a.lines().stream().mapToLong(PokerSettlementCalculator.Line::total).sum());
        assertTrue(a.lines().stream().filter(l->l.playerId()!=10).allMatch(l->l.tags().contains("SHUT_OUT")));
        assertTrue(a.lines().stream().anyMatch(l->l.special()!=0));
        assertNotEquals(a.checksum(),calc.calculate("njpdk-v3",0,seats,2,2).checksum());
        seats.put(2,new PokerSettlementCalculator.SeatInput(12,5,0,2));assertNotEquals(a.checksum(),calc.calculate("njpdk-v2",0,seats,2,2).checksum());
        Map<Integer,PokerSettlementCalculator.SeatInput> reversed=new LinkedHashMap<>();reversed.put(2,seats.get(2));reversed.put(1,seats.get(1));reversed.put(0,seats.get(0));assertEquals(calc.calculate("njpdk-v2",0,seats,2,2),calc.calculate("njpdk-v2",0,reversed,2,2));
        Map<Integer,PokerSettlementCalculator.SeatInput> reverseSpring=Map.of(0,new PokerSettlementCalculator.SeatInput(10,5,1,0),1,new PokerSettlementCalculator.SeatInput(11,0,3,0),2,new PokerSettlementCalculator.SeatInput(12,6,0,0));assertTrue(calc.calculate("reverse",1,0,reverseSpring,0,2).lines().stream().allMatch(l->l.tags().contains("REVERSE_SPRING")));
    }
    @Test void capabilityConstructionAndPlaneBodiesAreStrict(){assertThrows(IllegalArgumentException.class,()->new PaoDeKuaiConfig(5,true,true,false,null,false,18,false,false,1,2,1,false));assertThrows(IllegalArgumentException.class,()->new PaoDeKuaiConfig(5,true,true,false,null,false,null,true,false,1,2,1,false));assertThrows(IllegalArgumentException.class,()->new PaoDeKuaiConfig(5,true,true,false,null,false,14,true,false,0,2,1,false));var rules=new PaoDeKuaiRuleSet(PaoDeKuaiConfig.defaults());assertThrows(IllegalArgumentException.class,()->rules.recognize(List.of(103,203,303,403,104,204,304,404),null));var terminalRules=new PaoDeKuaiRuleSet(new PaoDeKuaiConfig(5,true,true,false,null,false,null,false,false,1,2,1,true));var candidate=terminalRules.recognize(List.of(110,210,310,111,211,311),null);var threeGroup=new CardCombination("AIRPLANE_WITH_SINGLES",9,List.of(107,207,307,108,208,308,109,209,309,103,104,105));assertFalse(terminalRules.canBeat(candidate,threeGroup,new PaoDeKuaiContext(false,2,candidate.cards())));}
    @Test void jokerRanksCannotConfigureTripleBomb(){assertThrows(IllegalArgumentException.class,()->new PaoDeKuaiConfig(5,true,true,false,null,false,16,false,false,1,2,1,false));assertThrows(IllegalArgumentException.class,()->new PaoDeKuaiConfig(5,true,true,false,null,false,17,false,false,1,2,1,false));}
    @Test void catalogMatrixHas150BindingsAcrossEightFamilies()throws Exception{List<String>lines;try(var in=getClass().getResourceAsStream("/poker-family-region-config.tsv")){assertNotNull(in);lines=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)).lines().toList();}assertEquals(151,lines.size());Set<String>families=new TreeSet<>();Map<String,String[]>nativeRows=new HashMap<>();for(String line:lines.subList(1,lines.size())){String[]v=line.split("\\t",-1);assertEquals(11,v.length);families.add(v[2]);if(Set.of("pdk","aypdk","hbpdk","cp","hndzp","lhzp").contains(v[1]))nativeRows.put(v[1],v);assertFalse(v[10].isBlank());}assertEquals(Set.of("poker:510k","poker:betting","poker:climbing","poker:compare-hand","poker:generic-card-round","poker:landlord","poker:pao-de-kuai","poker:trick-taking"),families);assertEquals(6,nativeRows.size());for(var row:nativeRows.values()){GameDescriptor d=new GameDescriptor(Integer.parseInt(row[0]),row[1],row[1].toUpperCase(),GameCategory.POKER,row[2],RegionScope.PROVINCE,"test","","1.0.0");GameProvider p=PokerCatalogRuntimeRegistry.providerFor(d).orElseThrow();assertSame(d,p.descriptor());assertEquals("poker-family-runtime",p.defaultConfiguration().get("provider"));assertTrue(p.eventReplayProvider().isPresent());assertTrue(p.reconnectViewProvider().isPresent());assertTrue(p.settlementProvider().isPresent());}}
    @Test void pokerCatalogRegistryIsCategoryAndCodeIsolated(){GameDescriptor other=new GameDescriptor(1,"x","X",GameCategory.LONG_CARD,"long-card:regional",RegionScope.NATIONAL,"","","1");assertTrue(PokerCatalogRuntimeRegistry.providerFor(other).isEmpty());GameDescriptor unknown=new GameDescriptor(999,"unknown","UNKNOWN",GameCategory.POKER,"poker:generic-card-round",RegionScope.NATIONAL,"","","1");assertTrue(PokerCatalogRuntimeRegistry.providerFor(unknown).isEmpty());}
    @Test void publishedPdkProviderUsesTheCatalogPlayVersion(){GameDescriptor descriptor=new GameDescriptor(8,"pdk","成都跑得快",GameCategory.POKER,"poker:pao-de-kuai",RegionScope.PROVINCE,"CN-51-01","","1.0.0");GameProvider provider=PokerCatalogRuntimeRegistry.providerFor(descriptor).orElseThrow();assertEquals("1.0.0",provider.defaultConfiguration().get("playVersion"));assertEquals(descriptor,provider.descriptor());}
    private static PokerRuleProfile profile(PokerRuleProfile.FirstLead lead,Integer card){return new PokerRuleProfile("matrix",48,3,3,lead,card,5,2,false,false,true,true,1,16,2,20);}
}
