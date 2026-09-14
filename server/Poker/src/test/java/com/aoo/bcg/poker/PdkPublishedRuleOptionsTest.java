package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import com.aoo.bcg.gamespi.GameCommandRequest;
import org.junit.jupiter.api.Test;

final class PdkPublishedRuleOptionsTest {
    @Test void mustBeatWhenPossibleFlowsFromCreationRulesIntoPublishedSnapshot() {
        PaoDeKuaiConfig config=PdkPublishedRuleOptions.apply(Map.of("mustBeatWhenPossible",false),
                PaoDeKuaiConfig.defaults());
        PokerRuleProfile base=PokerRuleProfile.paoDeKuai("pdk-test",48,null);
        PokerRuleProfile profile=PdkPublishedRuleOptions.profile("pdk-test",
                Map.of("mustBeatWhenPossible",false),config,base);
        assertFalse(profile.mustBeatWhenPossible());
        assertEquals(false,PdkPublishedRuleOptions.snapshot(config,profile).get("mustBeatWhenPossible"));
        assertEquals(false,PdkPublishedRuleOptions.snapshot(config,profile,false)
                .get("allowPassByRoomRule"));
        assertEquals(true,PdkPublishedRuleOptions.snapshot(config,profile,true)
                .get("allowPassByRoomRule"));
    }
    @Test void appliesStrictPublishedValuesAndRejectsUnknownEnumValue() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "minimumStraightLength", 3,
                "minimumPairRunLength", 3,
                "tripleAttachmentMode", "PAIRS",
                "fourAttachmentMode", "PAIRS",
                "tripleWithoutAttachmentTiming", "FINAL_ONLY",
                "forceHighestPairAgainstReportedPair", true), PaoDeKuaiConfig.defaults());
        assertEquals(3, config.minimumStraightLength());
        assertEquals(3, config.minimumPairRunLength());
        assertEquals(PaoDeKuaiConfig.AttachmentMode.PAIRS, config.tripleAttachmentMode());
        assertTrue(config.forceHighestPairAgainstReportedPair());
        assertEquals("STRAIGHT", new PaoDeKuaiRuleSet(config)
                .recognize(List.of(103,104,105), null).type());
        assertThrows(IllegalArgumentException.class, () -> PdkPublishedRuleOptions.apply(
                Map.of("tripleAttachmentMode", "guess"), PaoDeKuaiConfig.defaults()));
    }

    @Test void enforcesTripleTimingAttachmentModesAndAttachmentComparison() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "tripleWithoutAttachmentTiming", "FINAL_ONLY",
                "tripleAttachmentMode", "EITHER",
                "compareTripleAttachments", true), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        assertThrows(IllegalArgumentException.class, () -> rules.recognize(List.of(103,203,303),
                new PaoDeKuaiContext(false, 3, List.of(103,203,303,104))));
        assertEquals("TRIPLE", rules.recognize(List.of(103,203,303),
                new PaoDeKuaiContext(false, 3, List.of(103,203,303))).type());
        CardCombination low = rules.recognize(List.of(103,203,303,105), null);
        CardCombination highCoreLowAttachment = rules.recognize(List.of(104,204,304,103), null);
        CardCombination highCoreHighAttachment = rules.recognize(List.of(104,204,304,106), null);
        assertFalse(rules.canBeat(highCoreLowAttachment, low, null));
        assertTrue(rules.canBeat(highCoreHighAttachment, low, null));
        CardCombination pairWings = rules.recognize(List.of(104,204,304,106,206), null);
        CardCombination singleWings = rules.recognize(List.of(103,203,303,105,106), null);
        assertFalse(rules.canBeat(pairWings, singleWings, null));
    }

    @Test void recognizesProvenAirplaneAndFourAttachmentModes() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(Map.of(
                "airplaneAttachmentMode", "EITHER",
                "allowAirplaneWithTwo", true,
                "fourAttachmentMode", "PAIRS"), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        assertEquals("AIRPLANE_WITH_SINGLES", rules.recognize(
                List.of(103,203,303,104,204,304,105,106), null).type());
        assertEquals("AIRPLANE_WITH_PAIRS", rules.recognize(
                List.of(103,203,303,104,204,304,105,205,106,206), null).type());
        assertEquals("FOUR_WITH_TWO_PAIRS", rules.recognize(
                List.of(103,203,303,403,104,204,105,205), null).type());
        assertThrows(IllegalArgumentException.class, () -> rules.recognize(
                List.of(103,203,303,403,104,105), null));
    }

    @Test void forcesHighestPairWhenNextPlayerReportsTwoCards() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
                Map.of("forceHighestPairAgainstReportedPair", true), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        PaoDeKuaiContext context = new PaoDeKuaiContext(false, 2,
                List.of(103,203,105,205,106));
        assertThrows(IllegalStateException.class,
                () -> rules.validatePlay(List.of(103,203), context));
        assertDoesNotThrow(() -> rules.validatePlay(List.of(105,205), context));
    }

    @Test void serializedRuleOptionsAreImmutableAndRoundTrip() {
        PaoDeKuaiConfig original = PdkPublishedRuleOptions.apply(Map.of(
                "airplaneWithoutAttachmentTiming", "FINAL_ONLY",
                "allowSingle", false, "allowPair", false), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(original);
        assertThrows(IllegalArgumentException.class, () -> rules.recognize(List.of(103), null));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recognize(List.of(103,203), null));
        Map<String,Object> snapshot = PdkPublishedRuleOptions.snapshot(original);
        PaoDeKuaiConfig restored = PdkPublishedRuleOptions.apply(snapshot,
                PaoDeKuaiConfig.defaults());
        assertEquals(original, restored);
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("allowSingle", true));
    }

    @Test void supportsSourceBackedDealProfileConsecutiveBombAndSpecialTripleRanks() {
        List<Integer> deck = List.of(103,203,303,403,104,204,304,404);
        Map<String,Object> published = Map.of(
                "cardsPerPlayer", 3,
                "deckCards", deck,
                "allowConsecutiveBomb", true,
                "specialTripleBombRanks", List.of(5,6),
                "playedCardVisibility", "ALL_IN_ORDER");
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        PokerRuleProfile base = PdkRuleProfiles.flexibleTwoToFourPlayers("source-backed", null);
        PokerRuleProfile profile = PdkPublishedRuleOptions.profile("source-backed", published,
                config, base);
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config, profile);
        assertEquals(3, rules.cardsPerPlayer(2));
        assertEquals(deck, profile.deck());
        assertEquals("CONSECUTIVE_BOMB", rules.recognize(
                List.of(103,203,303,403,104,204,304,404), null).type());
        assertEquals("SPECIAL_TRIPLE_BOMB",
                rules.recognize(List.of(105,205,305), null).type());
        assertEquals(PaoDeKuaiConfig.PlayedCardVisibility.ALL_IN_ORDER,
                config.playedCardVisibility());
        assertEquals(deck, PdkPublishedRuleOptions.snapshot(config, profile).get("deckCards"));
    }

    @Test void rejectsDisabledConsecutiveAndUnknownSpecialTripleBombs() {
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(PaoDeKuaiConfig.defaults());
        assertThrows(IllegalArgumentException.class, () -> rules.recognize(
                List.of(103,203,303,403,104,204,304,404), null));
        assertEquals("TRIPLE", rules.recognize(List.of(105,205,305), null).type());
    }

    @Test void validatesEverySourceBackedPlayerAndHandSizeCombination() {
        PokerRuleProfile profile = PdkRuleProfiles.flexibleTwoToFourPlayers("source-deals", null);
        int[][] supported = {{2,8},{2,10},{2,13},{2,16},{3,8},{3,10},{3,13},{3,16},
                {4,8},{4,10}};
        for (int[] deal : supported) {
            PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
                    Map.of("cardsPerPlayer", deal[1]), PaoDeKuaiConfig.defaults());
            assertEquals(deal[1], new PaoDeKuaiRuleSet(config, profile)
                    .cardsPerPlayer(deal[0]));
        }
        PaoDeKuaiConfig tooMany = PdkPublishedRuleOptions.apply(
                Map.of("cardsPerPlayer", 13), PaoDeKuaiConfig.defaults());
        assertThrows(IllegalStateException.class,
                () -> new PaoDeKuaiRuleSet(tooMany, profile).cardsPerPlayer(4));
    }

    @Test void consecutiveBombComparisonMatchesSourceTierEncoding() {
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
                Map.of("allowConsecutiveBomb", true), PaoDeKuaiConfig.defaults());
        PaoDeKuaiRuleSet rules = new PaoDeKuaiRuleSet(config);
        CardCombination twoGroups = rules.recognize(
                List.of(103,203,303,403,104,204,304,404), null);
        CardCombination threeGroups = rules.recognize(
                List.of(103,203,303,403,104,204,304,404,105,205,305,405), null);
        assertTrue(rules.canBeat(threeGroups, twoGroups, null));
        assertFalse(rules.canBeat(twoGroups, threeGroups, null));
    }

    @Test void playedCardVisibilityIsServerAuthoritativeAndSurvivesRestore() {
        assertEquals(1, visiblePlayCount("LAST_ONLY"));
        assertEquals(2, visiblePlayCount("ALL_IN_ORDER"));
    }

    @SuppressWarnings("unchecked")
    private static int visiblePlayCount(String visibility) {
        Map<String,Object> published = Map.of("cardsPerPlayer",2,
                "playedCardVisibility",visibility);
        PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(published,
                PaoDeKuaiConfig.defaults());
        PokerRuleProfile base = PdkRuleProfiles.flexibleTwoToFourPlayers("history",null);
        PaoDeKuaiFamily family = new PaoDeKuaiFamily(config,
                PdkPublishedRuleOptions.profile("history",published,config,base));
        PokerAuthoritativeSession session = new PokerAuthoritativeSession(77,10,2,9,family);
        long sequence=1;
        session.execute(command(session,"join_req",sequence++,1,11,Map.of()));
        session.execute(command(session,"ready_req",sequence++,0,10,Map.of()));
        session.execute(command(session,"ready_req",sequence++,1,11,Map.of()));
        PokerTurnState state=(PokerTurnState)session.authoritativeState().get("state");
        int seat=state.currentSeat();
        session.execute(command(session,"play_req",sequence++,seat,10+seat,
                Map.of("cards",List.of(state.hands().get(seat).getFirst()))));
        state=(PokerTurnState)session.authoritativeState().get("state");
        seat=state.currentSeat();
        var hint=session.execute(command(session,"hint_req",sequence++,seat,10+seat,Map.of()));
        List<CardCombination> hints=(List<CardCombination>)hint.body().get("hints");
        if(hints.isEmpty()){
            session.execute(command(session,"pass_req",sequence++,seat,10+seat,Map.of()));
            state=(PokerTurnState)session.authoritativeState().get("state");
            seat=state.currentSeat();
            session.execute(command(session,"play_req",sequence++,seat,10+seat,
                    Map.of("cards",List.of(state.hands().get(seat).getFirst()))));
        }else session.execute(command(session,"play_req",sequence++,seat,10+seat,
                Map.of("cards",hints.getFirst().cards())));
        Map<String,Object> saved=session.authoritativeState();
        PokerAuthoritativeSession restored=PokerAuthoritativeSession.restore(saved,family);
        assertEquals(saved,restored.authoritativeState());
        return ((List<?>)restored.viewFor(10).get("playedCards")).size();
    }

    private static GameCommandRequest command(PokerAuthoritativeSession session,String message,
            long sequence,int seat,long player,Map<String,Object> body){
        Map<String,Object> state=session.authoritativeState();
        return new GameCommandRequest(message,"history-"+sequence,sequence,77,
                ((Number)state.get("roundNo")).intValue(),"history",String.valueOf(player),seat,body);
    }
}
