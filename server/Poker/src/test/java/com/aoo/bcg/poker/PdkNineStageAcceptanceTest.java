package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PdkNineStageAcceptanceTest {
    @Test void createJoinReadyDealTurnLegalPlayReconnectAndReplay() {
        PdkGameProvider provider = new PdkGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(
                8001, 10, Map.of("seatLimit", 3, "shuffleSeed", 17L)));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command("join_req", 1, 0, 10, Map.of()));
        handler.handle(room, command("join_req", 2, 1, 20, Map.of()));
        handler.handle(room, command("join_req", 3, 2, 30, Map.of()));
        handler.handle(room, command("ready_req", 4, 0, 10, Map.of()));
        handler.handle(room, command("start_req", 5, 0, 10, Map.of()));

        PokerAuthoritativeSession session = (PokerAuthoritativeSession) room.requireAuthoritativeSession();
        StatePayload replayBase = StatePayload.copyOf(session.authoritativeState());
        int replayOffset = session.recordedEvents().size();
        assertEquals(16, ownCards(session, 10, 0).size());
        int turn = (Integer) session.viewFor(10).get("currentSeat");
        long player = List.of(10L, 20L, 30L).get(turn);
        int card = ownCards(session, player, turn).getFirst();
        handler.handle(room, command("play_req", 6, turn, player, Map.of("cards", List.of(card))));
        assertNotEquals(turn, session.viewFor(10).get("currentSeat"));

        Map<String,Object> saved = session.authoritativeState();
        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(saved).orElseThrow();
        assertEquals(saved, restored.authoritativeState());
        assertEquals(session.viewFor(20), provider.reconnectViewProvider().orElseThrow().buildFor(20,
                new GameRoomHandle(8001, PdkGameProvider.GAME_ID, PdkGameProvider.VERSION, restored)));
        assertTrue(restored.invariantViolations().isEmpty());
        StatePayload replayed=provider.eventReplayProvider().orElseThrow().replay(replayBase,
                session.recordedEvents().subList(replayOffset,session.recordedEvents().size()));
        assertEquals(session.authoritativeState(),replayed.asMap());
        assertThrows(IllegalArgumentException.class,()->provider.eventReplayProvider().orElseThrow()
                .replay(StatePayload.copyOf(Map.of("stateVersion",0)),List.of(Map.of("version",2,"after",saved))));
    }

    @Test void serverRulesAndScoringAreRealAndServiceRegistrationIsUnique() {
        PdkGameProvider provider = new PdkGameProvider();
        assertEquals(48, ((PaoDeKuaiFamily) provider.pokerFamily()).profile().deckSize());
        PaoDeKuaiRuleSet rules = ((PaoDeKuaiFamily) provider.pokerFamily()).rules();
        assertEquals("FOUR_WITH_TWO", rules.recognize(List.of(103, 203, 303, 403, 104, 205),
                new PaoDeKuaiContext(false, 8, List.of(103, 203, 303, 403, 104, 205))).type());
        assertEquals(0, ServiceLoader.load(GameProvider.class).stream()
                .filter(item -> item.type() == PdkGameProvider.class).count());

        Map<String,Object> finished = finishedSnapshot(provider);
        AuthoritativeGameSession restored = provider.restoreAuthoritativeSession(finished).orElseThrow();
        SettlementPayload settlement = restored.settlement(1, PdkGameProvider.VERSION);
        assertEquals(0L, settlement.scoreDelta().values().stream().mapToLong(Long::longValue).sum());
        assertTrue(settlement.scoreDelta().get(10L) > 0);
        assertTrue(settlement.scoreDelta().get(20L) < 0);
    }

    private static Map<String,Object> finishedSnapshot(PdkGameProvider provider) {
        PaoDeKuaiFamily family = (PaoDeKuaiFamily) provider.pokerFamily();
        Map<String,Object> state = new LinkedHashMap<>();
        state.put("roomId", 8002L); state.put("ownerId", 10L); state.put("seatLimit", 3); state.put("seed", 9L);
        state.put("players", Map.of(0,10L,1,20L,2,30L)); state.put("readySeats", List.of(0,1,2));
        state.put("plays", Map.of(0,3,1,1,2,1)); state.put("bombs", Map.of(0,1,1,0,2,0));
        state.put("ruleVersion", PdkGameProvider.VERSION); state.put("ruleSnapshotKey", family.ruleSnapshotKey());
        state.put("pdkRuleOptions", PdkPublishedRuleOptions.snapshot(
                family.rules().config(), family.profile()));
        state.put("previousWinnerSeat", -1); state.put("initialLeadSeat", 0); state.put("operationDeadline", Map.of());
        state.put("playedCards", List.of(Map.of("type","SINGLE","primaryRank",15,
                "cards",List.of(115))));
        state.put("stateVersion", 8L);
        state.put("state", Map.of("hands", Map.of(0,List.of(),1,List.of(104,105),2,List.of(106,107,108)),
                "currentSeat",0,"previousSeat",0,"passed",List.of(),"finished",true,"winnerSeat",0,
                "previous",Map.of("type","SINGLE","primaryRank",15,"cards",List.of(115))));
        return state;
    }
    @SuppressWarnings("unchecked") private static List<Integer> ownCards(AuthoritativeGameSession session,long player,int seat) {
        Map<Integer,Object> seats=(Map<Integer,Object>)session.viewFor(player).get("seats");
        return (List<Integer>)((Map<String,Object>)seats.get(seat)).get("cards");
    }
    private static GameCommandRequest command(String id,long sequence,int seat,long player,Map<String,Object> body) {
        return new GameCommandRequest(id,"pdk-"+sequence,sequence,8001,0,PdkGameProvider.VERSION,
                String.valueOf(player),seat,body);
    }
}
