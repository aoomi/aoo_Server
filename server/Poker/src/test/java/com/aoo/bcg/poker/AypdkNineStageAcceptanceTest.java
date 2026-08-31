package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AypdkNineStageAcceptanceTest {
    @Test void createJoinReadyDealTurnLegalPlaySettlementReplayAndReconnect() {
        AypdkGameProvider provider = new AypdkGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(
                78001, 10, Map.of("seatLimit", 3, "shuffleSeed", 78L)));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command("join_req", 1, 0, 10, Map.of()));
        handler.handle(room, command("join_req", 2, 1, 20, Map.of()));
        handler.handle(room, command("join_req", 3, 2, 30, Map.of()));
        handler.handle(room, command("ready_req", 4, 0, 10, Map.of()));
        handler.handle(room, command("start_req", 5, 0, 10, Map.of()));

        AypdkSession session = (AypdkSession) room.requireAuthoritativeSession();
        StatePayload base = StatePayload.copyOf(session.authoritativeState());
        int offset = session.recordedEvents().size();
        int turn = (Integer) session.viewFor(10).get("currentSeat");
        long player = List.of(10L, 20L, 30L).get(turn);
        List<Integer> cards = ownCards(session, player, turn);
        assertEquals(16, cards.size());
        assertTrue(cards.contains(AypdkRules.SPADE_THREE));
        handler.handle(room, command("play_req", 6, turn, player, Map.of("cards", List.of(AypdkRules.SPADE_THREE))));
        assertNotEquals(turn, session.viewFor(player).get("currentSeat"));

        Map<String,Object> saved = session.authoritativeState();
        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(saved).orElseThrow();
        assertEquals(saved, restored.authoritativeState());
        GameRoomHandle restoredRoom = new GameRoomHandle(78001, AypdkGameProvider.GAME_ID,
                AypdkGameProvider.VERSION, restored);
        assertEquals(session.viewFor(20), provider.reconnectViewProvider().orElseThrow().buildFor(20, restoredRoom));
        assertTrue(restored.invariantViolations().isEmpty());
        assertEquals(saved, provider.eventReplayProvider().orElseThrow().replay(base,
                session.recordedEvents().subList(offset, session.recordedEvents().size())).asMap());
        assertThrows(IllegalArgumentException.class, () -> provider.eventReplayProvider().orElseThrow()
                .replay(StatePayload.copyOf(Map.of("stateVersion", 0)),
                        List.of(Map.of("version", 2, "after", saved))));

        AuthoritativeGameSession finished = provider.restoreAuthoritativeSession(finishedSnapshot(provider)).orElseThrow();
        SettlementPayload settlement = provider.settlementProvider().orElseThrow().settle(
                new GameRoomHandle(78002, AypdkGameProvider.GAME_ID, AypdkGameProvider.VERSION, finished), 1);
        assertEquals(0L, settlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum());
        assertTrue(settlement.scoreDelta().get(10L) > 0);
    }

    @Test void exactLegacyDeckSpecialBombAndServiceRegistration() {
        PaoDeKuaiFamily family = (PaoDeKuaiFamily) new AypdkGameProvider().pokerFamily();
        assertEquals(48, family.profile().deck().size());
        assertEquals(3, family.profile().deck().stream().filter(c -> PokerCardCodec.rank(c) == 14).count());
        assertEquals(1, family.profile().deck().stream().filter(c -> PokerCardCodec.rank(c) == 15).count());
        List<Integer> aces = family.profile().deck().stream().filter(c -> PokerCardCodec.rank(c) == 14).toList();
        assertEquals("SPECIAL_TRIPLE_BOMB", family.rules().recognize(aces,
                new PaoDeKuaiContext(false, 8, aces)).type());
        assertEquals(0, ServiceLoader.load(GameProvider.class).stream()
                .filter(item -> item.type() == AypdkGameProvider.class).count());
    }

    @Test void allSourceButtonsAlterServerAuthority() {
        AypdkOptions defaults=AypdkOptions.from(Map.of());
        PaoDeKuaiFamily strict=AypdkRules.family(defaults);
        PaoDeKuaiFamily arbitrary=AypdkRules.family(AypdkOptions.from(Map.of("heitaosanbichu",1)));
        assertNotEquals(strict.ruleSnapshotKey(),arbitrary.ruleSnapshotKey());
        assertNotNull(strict.rules().config().requiredFirstCard());
        assertNull(arbitrary.rules().config().requiredFirstCard());
        assertEquals(PokerRuleProfile.FirstLead.RANDOM,AypdkRules.family(AypdkOptions.from(Map.of("chupai",2))).profile().firstLead());
        assertEquals(10,AypdkRules.family(AypdkOptions.from(Map.of("zhadan",2))).profile().bombUnit());
        assertEquals(0,strict.profile().bombUnit());
        PaoDeKuaiFamily tripleOne=AypdkRules.family(AypdkOptions.from(Map.of("paixing",List.of(1))));
        assertEquals("TRIPLE_WITH_ONE",tripleOne.rules().recognize(List.of(103,203,303,104),
                new PaoDeKuaiContext(false,8,List.of(103,203,303,104))).type());

        SettlementPayload high=finishedWithOptions(Map.of("daxiaoguan",0)).settlement(1,AypdkGameProvider.VERSION);
        SettlementPayload low=finishedWithOptions(Map.of("daxiaoguan",1)).settlement(1,AypdkGameProvider.VERSION);
        assertNotEquals(high.scoreDelta(),low.scoreDelta());
        assertNotEquals(AypdkRules.family(AypdkOptions.from(Map.of("teshu",List.of(0)))).ruleSnapshotKey(),
                AypdkRules.family(defaults).ruleSnapshotKey());
    }

    private static AuthoritativeGameSession finishedWithOptions(Map<String,Object> rules){AypdkOptions options=AypdkOptions.from(rules);Map<String,Object>s=new LinkedHashMap<>(finishedSnapshot(new AypdkGameProvider()));PaoDeKuaiFamily family=AypdkRules.family(options);s.put("ruleSnapshotKey",family.ruleSnapshotKey());s.put("aypdkOptions",options.toMap());return AypdkSession.restore(s);}

    private static Map<String,Object> finishedSnapshot(AypdkGameProvider provider) {
        PaoDeKuaiFamily family = (PaoDeKuaiFamily) provider.pokerFamily();
        Map<String,Object> state = new LinkedHashMap<>();
        state.put("roomId", 78002L); state.put("ownerId", 10L); state.put("seatLimit", 3); state.put("seed", 9L);
        state.put("players", Map.of(0,10L,1,20L,2,30L)); state.put("readySeats", List.of(0,1,2));
        state.put("plays", Map.of(0,3,1,1,2,1)); state.put("bombs", Map.of(0,1,1,0,2,0));
        state.put("ruleVersion", AypdkGameProvider.VERSION); state.put("ruleSnapshotKey", family.ruleSnapshotKey());
        state.put("aypdkOptions", AypdkOptions.from(Map.of()).toMap());
        state.put("previousWinnerSeat", -1); state.put("initialLeadSeat", 0); state.put("operationDeadline", Map.of());
        state.put("playedCards", List.of(Map.of("type","SINGLE","primaryRank",15,
                "cards",List.of(415))));
        state.put("stateVersion", 8L);
        state.put("state", Map.of("hands", Map.of(0,List.of(),1,List.of(104,105),2,List.of(106,107,108)),
                "currentSeat",0,"previousSeat",0,"passed",List.of(),"finished",true,"winnerSeat",0,
                "previous",Map.of("type","SINGLE","primaryRank",15,"cards",List.of(415))));
        return state;
    }
    @SuppressWarnings("unchecked") private static List<Integer> ownCards(AuthoritativeGameSession session,long player,int seat) {
        Map<Integer,Object> seats=(Map<Integer,Object>)session.viewFor(player).get("seats");
        return (List<Integer>)((Map<String,Object>)seats.get(seat)).get("cards");
    }
    private static GameCommandRequest command(String id,long sequence,int seat,long player,Map<String,Object> body) {
        return new GameCommandRequest(id,"aypdk-"+sequence,sequence,78001,0,AypdkGameProvider.VERSION,
                String.valueOf(player),seat,body);
    }
}
